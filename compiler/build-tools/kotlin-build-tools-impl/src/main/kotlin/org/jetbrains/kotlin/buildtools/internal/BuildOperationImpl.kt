/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.kotlin.build.report.metrics.DAEMON_INCREASED_MEMORY
import org.jetbrains.kotlin.build.report.metrics.DAEMON_MEMORY_USAGE
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.internal.serializability.*
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorWithDiagnosticId
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.reflect.KMutableProperty1

@OptIn(ExperimentalAtomicApi::class)
@Serializable
internal abstract class BuildOperationImpl<R> : BuildOperation<R>, BuildOperation.Builder, BtaSerializable {
    @Transient
    protected val compilationId: Int = compilationIdCounter.incrementAndFetch()

    protected open val options: Options get() = Options(this::class) // to be removed when subclasses are also converted to serializable

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

    internal open fun copyFrom(other: BuildOperationImpl<R>) {
        metricsCollector = other.metricsCollector
        kgpMetricsCollector = other.kgpMetricsCollector
        kgpMetricsCollectorOut = other.kgpMetricsCollectorOut
        enableClassloaderCache = other.enableClassloaderCache
    }

    @Transient
    private val executionStarted = AtomicBoolean(false)

    protected open val warningsAsError: Boolean
        get() = false

    @Transient
    open val compilerMessageRenderer: CompilerMessageRenderer = DefaultCompilerMessageRenderer

    override fun afterDeserialization(messageReporter: (Message) -> Unit, compilerFacade: CompilerServicesFacadeBase?) {
        if (hasMetricsCollector) {
            metricsCollector = MessageReportingMetricsCollector(messageReporter)
        }
    }

    override fun beforeSerialization(logger: KotlinLogger): List<MessageVisitor> = buildList {
        metricsCollector?.let { add(MetricsCollectorVisitor(it)) }
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: BuildOperation.Option<V>): V = getPropertyByName(propertyAccessMap, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        setPropertyByName(propertyAccessMap, key.id, value)
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

    internal open fun executeInDaemon(
        projectId: ProjectId,
        executionPolicy: DaemonExecutionPolicyImpl,
        logger: KotlinLogger,
        executionContext: ExecutionContext,
    ): R {
        @Suppress("UNCHECKED_CAST")
        this as SupportsNewDaemonMode<R>
        @Suppress("UNCHECKED_CAST")
        this as DeepCopyable<BuildOperationImpl<R>>

        deepCopy().let { operationCopy -> // do not modify the original operation object
            val messageVisitors: List<MessageVisitor> = operationCopy.beforeSerialization(logger) + LogLineVisitor(logger)

            val loggerAdapter =
                KotlinLoggerMessageCollectorAdapter(logger, operationCopy.compilerMessageRenderer, operationCopy.warningsAsError)
            val daemon = executionContext.daemonConnectionRegistry.getCompileServiceSession(executionPolicy, logger, loggerAdapter)
                ?: error("Unable to get daemon connection")
            val memoryUsageBeforeBuild = daemon.compileService.getUsedMemory(withGC = false).takeIf { it.isGood }?.get()

            val serializedOperation = KotlinToolchainsImpl.serializer.serialize<BtaSerializable>(operationCopy)
            val callbackChannel = BtaCallbackChannel(KotlinToolchainsImpl.serializer, messageVisitors)

            if (this is CancellableBuildOperationImpl<*>) {
                onCancel {
                    logger.info("Sending cancel request for operation ${operationCopy.compilationId}")
                    daemon.compileService.cancelCompilation(daemon.sessionId, operationCopy.compilationId)
                }
            }

            val result = try {
                val callResult = daemon.compileService.execute(
                    daemon.sessionId,
                    operationCopy.compilationId,
                    serializedOperation,
                    callbackChannel,
                    createCompilerServicesFacade(logger, createMessageCollector(logger)),
                    loggerDebugEnabled = logger.isDebugEnabled,
                )
                if (callResult is CompileService.CallResult.Error) {
                    callResult.cause?.let { throw it }
                }
                callResult.get()
            } finally {
                val memoryUsageAfterBuild =
                    operationCopy.runCatching { daemon.compileService.getUsedMemory(withGC = false).takeIf { it.isGood }?.get() }.getOrNull()
                operationCopy.getMetricsReporter().let { metricsReporter ->
                    if (memoryUsageAfterBuild == null || memoryUsageBeforeBuild == null) {
                        logger.debug("Unable to calculate memory usage")
                    } else {
                        metricsReporter.addMetric(DAEMON_INCREASED_MEMORY, memoryUsageAfterBuild - memoryUsageBeforeBuild)
                        metricsReporter.addMetric(DAEMON_MEMORY_USAGE, memoryUsageAfterBuild)
                    }
                }
            }
            return deserializeResult(result)
        }
    }

    /**
     * `true` if this operation uses [org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment], so the [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession] can reuse it across operations.
     *
     * The value is checked only for in-process executions. The daemon uses `keepalive` mechanism of the compiler to cache it for the entire
     * lifetime of the daemon.
     */
    internal abstract val usesApplicationEnvironment: Boolean

    internal companion object {
        private val propertyAccessMap: Map<String, KMutableProperty1<BuildOperationImpl<*>, out Any?>> = buildMap {
            put("METRICS_COLLECTOR", BuildOperationImpl<*>::metricsCollector)
            put("XX_KGP_METRICS_COLLECTOR", BuildOperationImpl<*>::kgpMetricsCollector)
            put("XX_KGP_METRICS_COLLECTOR_OUT", BuildOperationImpl<*>::kgpMetricsCollectorOut)
            put("ENABLE_CLASSLOADER_CACHE", BuildOperationImpl<*>::enableClassloaderCache)
        }

        private val compilationIdCounter = AtomicInt(0)
    }

    protected open fun createMessageCollector(logger: KotlinLogger): MessageCollectorWithDiagnosticId =
        DoNothingMessageCollector()

    protected open fun createCompilerServicesFacade(
        logger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId
    ): CompilerServicesFacadeBase? = null
}

private class DoNothingMessageCollector : MessageCollectorWithDiagnosticId {
    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
        diagnosticId: String?,
    ) {

    }

    override fun clear() {

    }

    override fun hasErrors(): Boolean {
        return false
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
