/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.jetbrains.kotlin.build.report.metrics.DAEMON_INCREASED_MEMORY
import org.jetbrains.kotlin.build.report.metrics.DAEMON_MEMORY_USAGE
import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.internal.serializability.BtaSerializable
import org.jetbrains.kotlin.buildtools.internal.serializability.Message
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Serializable
public abstract class BuildOperationImpl<R> : BuildOperation<R>, BuildOperation.Builder, BtaSerializable {
    @SerialName("METRICS_COLLECTOR")
    @Transient
    internal var metricsCollector: BuildMetricsCollector? = null
        set(value) {
            hasMetricsCollector = value != null
            field = value
        }

    @SerialName("HAS_METRICS_COLLECTOR")
    private var hasMetricsCollector = false

    @SerialName("XX_KGP_METRICS_COLLECTOR")
    internal var kgpMetricsCollector: Boolean = false

    @SerialName("XX_KGP_METRICS_COLLECTOR_OUT")
    internal var kgpMetricsCollectorOut: ByteArray? = null

    @SerialName("ENABLE_CLASSLOADER_CACHE")
    internal var enableClassloaderCache: Boolean = true

    internal fun copyFrom(from: BuildOperationImpl<R>) {
        metricsCollector = from.metricsCollector
        kgpMetricsCollector = from.kgpMetricsCollector
        kgpMetricsCollectorOut = from.kgpMetricsCollectorOut
        enableClassloaderCache = from.enableClassloaderCache
    }

    @Transient
    private val executionStarted = AtomicBoolean(false)

    public open val warningsAsError: Boolean = false

    public open val compilerMessageRenderer: CompilerMessageRenderer = DefaultCompilerMessageRenderer

    override fun afterSerialization(messageReporter: (Message) -> Unit) {
        if (hasMetricsCollector) {
            metricsCollector = MessageReportingMetricsCollector(messageReporter)
        }
    }

    override fun beforeSerialization(logger: KotlinLogger): List<MessageVisitor> {
        val messageVisitors = mutableListOf<MessageVisitor>()
        metricsCollector?.let { messageVisitors.add(MetricsCollectorVisitor(it)) }
        return messageVisitors
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: BuildOperation.Option<V>): V = BuildOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        BuildOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    internal fun execute(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger? = null,
        executionContext: ExecutionContext,
    ): R {
        check(executionStarted.compareAndSet(expectedValue = false, newValue = true)) {
            "Build operation $this already started execution."
        }
        return executeImpl(projectId, executionPolicy, logger, executionContext)
    }

    internal abstract fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger? = null,
        executionContext: ExecutionContext,
    ): R

    public abstract fun getResultSerializer(): KSerializer<R>

    @OptIn(ExperimentalSerializationApi::class)
    internal open fun executeInDaemon(
        projectId: ProjectId,
        executionPolicy: DaemonExecutionPolicyImpl,
        logger: KotlinLogger,
        executionContext: ExecutionContext,
    ): R {
        val messageVisitors: List<MessageVisitor> = beforeSerialization(logger) + LogLineVisitor(logger)

        val loggerAdapter = KotlinLoggerMessageCollectorAdapter(logger, compilerMessageRenderer, warningsAsError)
        val daemon = executionContext.daemonConnectionRegistry.getCompileServiceSession(executionPolicy, logger, loggerAdapter)
            ?: error("Unable to get daemon connection")
        val memoryUsageBeforeBuild = daemon.compileService.getUsedMemory(withGC = false).takeIf { it.isGood }?.get()

        val serializedOperation = KotlinToolchainsImpl.protobuf.encodeToByteArray<BtaSerializable>(this)
        val callbackChannel = BtaCallbackChannel(KotlinToolchainsImpl.protobuf, messageVisitors)
        val result = try {
            daemon.compileService.execute(serializedOperation, 0, callbackChannel).get()
        } finally {
            val memoryUsageAfterBuild = runCatching { daemon.compileService.getUsedMemory(withGC = false).takeIf { it.isGood }?.get() }.getOrNull()
            getMetricsReporter().let { metricsReporter ->
                if (memoryUsageAfterBuild == null || memoryUsageBeforeBuild == null) {
                    logger.debug("Unable to calculate memory usage")
                } else {
                    metricsReporter.addMetric(DAEMON_INCREASED_MEMORY, memoryUsageAfterBuild - memoryUsageBeforeBuild)
                    metricsReporter.addMetric(DAEMON_MEMORY_USAGE, memoryUsageAfterBuild)
                }
            }
        }
        return KotlinToolchainsImpl.protobuf.decodeFromByteArray(getResultSerializer(), result)
    }

    /**
     * `true` if this operation uses [org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment], so the [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession] can reuse it across operations.
     *
     * The value is checked only for in-process executions. The daemon uses `keepalive` mechanism of the compiler to cache it for the entire
     * lifetime of the daemon.
     */
    internal abstract val usesApplicationEnvironment: Boolean

    internal operator fun <V> get(key: Option<V>): V = BuildOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    internal operator fun <V> set(key: Option<V>, value: V) {
        BuildOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    internal class Option<V>(id: String) : BaseOption<V>(id)

    internal companion object {
        val METRICS_COLLECTOR: Option<BuildMetricsCollector?> = Option("METRICS_COLLECTOR")
        val XX_KGP_METRICS_COLLECTOR: Option<Boolean> = Option("XX_KGP_METRICS_COLLECTOR")
        val XX_KGP_METRICS_COLLECTOR_OUT: Option<ByteArray?> = Option("XX_KGP_METRICS_COLLECTOR_OUT")
        val ENABLE_CLASSLOADER_CACHE: Option<Boolean> = Option("ENABLE_CLASSLOADER_CACHE")
    }
}

private class MessageReportingMetricsCollector(private val messageReporter: (Message) -> Unit) : BuildMetricsCollector {
    override fun collectMetric(
        name: String,
        type: BuildMetricsCollector.ValueType,
        value: Long,
    ) {
        messageReporter(
            Message.CollectMetricMessage(name, type, value)
        )
    }
}
