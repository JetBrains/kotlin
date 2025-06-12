/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.v2.internal.Option
import org.jetbrains.kotlin.buildtools.api.v2.internal.OptionsDelegate

abstract class BuildOperationImpl<R> : BuildOperation<R> {
    protected val optionsDelegate = OptionsDelegate()

    override fun <V> get(key: BuildOperation.Option<V>): V = optionsDelegate[key as Option]
    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        optionsDelegate[key as Option] = value
    }

    abstract fun execute(executionPolicy: ExecutionPolicy? = null, logger: KotlinLogger? = null): R
}