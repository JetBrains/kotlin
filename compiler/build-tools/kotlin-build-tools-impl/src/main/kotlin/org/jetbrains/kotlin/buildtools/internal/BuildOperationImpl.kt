/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.internal.serializability.*
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KMutableProperty1

@OptIn(ExperimentalAtomicApi::class)
@Serializable
internal abstract class BuildOperationImpl<R> : BuildOperation<R>, BuildOperation.Builder, BtaSerializable {

    protected abstract val options: Options // to be removed when subclasses are also converted to serializable

    @Transient
    internal var metricsCollector: BuildMetricsCollector? = null
        set(value) {
            hasMetricsCollector = value != null
            field = value
        }

    private var hasMetricsCollector = false

    internal var kgpMetricsCollector: Boolean = false

    internal var kgpMetricsCollectorOut: ByteArray? = null

    internal var enableClassloaderCache: Boolean = true

    internal open fun copyFrom(other: BuildOperationImpl<R>) {
        metricsCollector = other.metricsCollector
        kgpMetricsCollector = other.kgpMetricsCollector
        kgpMetricsCollectorOut = other.kgpMetricsCollectorOut
        enableClassloaderCache = other.enableClassloaderCache
    }

    @Transient
    private val executionStarted = AtomicBoolean(false)

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

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: BuildOperation.Option<V>): V = when (key.id) {
        "METRICS_COLLECTOR" -> metricsCollector
        "XX_KGP_METRICS_COLLECTOR" -> kgpMetricsCollector
        "XX_KGP_METRICS_COLLECTOR_OUT" -> kgpMetricsCollectorOut
        "ENABLE_CLASSLOADER_CACHE" -> enableClassloaderCache
        else -> throwPropertyNotFound(key.id)
    } as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        when (key.id) {
            "METRICS_COLLECTOR" -> metricsCollector = value.cast()
            "XX_KGP_METRICS_COLLECTOR" -> kgpMetricsCollector = value.cast()
            "XX_KGP_METRICS_COLLECTOR_OUT" -> kgpMetricsCollectorOut = value.cast()
            "ENABLE_CLASSLOADER_CACHE" -> enableClassloaderCache = value.cast()
            else -> throwPropertyNotFound(key.id)
        }
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

    /**
     * `true` if this operation uses [org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment], so the [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession] can reuse it across operations.
     *
     * The value is checked only for in-process executions. The daemon uses `keepalive` mechanism of the compiler to cache it for the entire
     * lifetime of the daemon.
     */
    internal abstract val usesApplicationEnvironment: Boolean
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
