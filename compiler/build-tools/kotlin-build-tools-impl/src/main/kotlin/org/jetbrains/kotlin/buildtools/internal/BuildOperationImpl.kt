/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Serializable
internal abstract class BuildOperationImpl<R> : BuildOperation<R>, BuildOperation.Builder {
    @SerialName("METRICS_COLLECTOR")
    internal var metricsCollector: BuildMetricsCollector? = null

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

    @UseFromImplModuleRestricted
    override fun <V> get(key: BuildOperation.Option<V>): V = BuildOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        BuildOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    fun execute(
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

    abstract fun executeImpl(
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
    abstract val usesApplicationEnvironment: Boolean

    operator fun <V> get(key: Option<V>): V = BuildOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    operator fun <V> set(key: Option<V>, value: V) {
        BuildOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    companion object {
        val METRICS_COLLECTOR: Option<BuildMetricsCollector?> = Option("METRICS_COLLECTOR")
        val XX_KGP_METRICS_COLLECTOR: Option<Boolean> = Option("XX_KGP_METRICS_COLLECTOR")
        val XX_KGP_METRICS_COLLECTOR_OUT: Option<ByteArray?> = Option("XX_KGP_METRICS_COLLECTOR_OUT")
        val ENABLE_CLASSLOADER_CACHE: Option<Boolean> = Option("ENABLE_CLASSLOADER_CACHE")
    }
}
