/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation.Companion.METRICS_COLLECTOR
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation.Companion.PROJECT_ID
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation.Option
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.internal.v2.OptionsDelegate

abstract class BuildOperationImpl<R> : BuildOperation<R> {
    protected val optionsDelegate = OptionsDelegate()

    init {
        this[PROJECT_ID] = null
        this[METRICS_COLLECTOR] = null
    }

    override fun <V> get(key: Option<V>): V = optionsDelegate[key]
    override fun <V> set(key: Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    abstract fun execute(executionPolicy: ExecutionPolicy, logger: KotlinLogger? = null): R
}