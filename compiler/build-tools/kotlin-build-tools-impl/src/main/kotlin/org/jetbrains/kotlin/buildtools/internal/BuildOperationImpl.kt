/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.internal.Options.Companion.registerOptions

internal abstract class BuildOperationImpl<R> : BuildOperation<R>, HasFinalizableValues by HasFinalizableValuesImpl() {
    private val options: Options = registerOptions(BuildOperation::class)

    @UseFromImplModuleRestricted
    override fun <V> get(key: BuildOperation.Option<V>): V = options[key.id]

    @UseFromImplModuleRestricted
    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        options[key] = value
    }

    fun execute(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger? = null): R {
        finalizeValues()
        (executionPolicy as? ExecutionPolicyImpl)?.finalizeValues()
        return executeImpl(projectId, executionPolicy, logger)
    }

    abstract fun executeImpl(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger? = null): R

    operator fun <V> get(key: Option<V>): V = options[key]

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    companion object {
        val METRICS_COLLECTOR: Option<BuildMetricsCollector?> = Option("METRICS_COLLECTOR", default = null)
        val XX_KGP_METRICS_COLLECTOR: Option<Boolean> = Option("XX_KGP_METRICS_COLLECTOR", default = false)
        val XX_KGP_METRICS_COLLECTOR_OUT: Option<ByteArray?> = Option("XX_KGP_METRICS_COLLECTOR_OUT", default = null)
    }
}