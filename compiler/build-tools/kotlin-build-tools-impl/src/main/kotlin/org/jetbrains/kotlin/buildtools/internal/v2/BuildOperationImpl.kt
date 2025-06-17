/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.v2.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.v2.trackers.BuildMetricsCollector

abstract class BuildOperationImpl<R> : BuildOperation<R> {
    protected val optionsDelegate = OptionsDelegate()

    init {
        this[PROJECT_ID] = null
        this[METRICS_COLLECTOR] = null
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: BuildOperation.Option<V>): V = optionsDelegate[key.id]

    @UseFromImplModuleRestricted
    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> get(key: Option<V>): V = optionsDelegate[key]

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> set(key: Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    abstract fun execute(executionPolicy: ExecutionPolicy, logger: KotlinLogger? = null): R

    companion object {
        val PROJECT_ID: Option<ProjectId?> = Option("PROJECT_ID")

        val METRICS_COLLECTOR: Option<BuildMetricsCollector?> = Option("METRICS_COLLECTOR")
    }
}