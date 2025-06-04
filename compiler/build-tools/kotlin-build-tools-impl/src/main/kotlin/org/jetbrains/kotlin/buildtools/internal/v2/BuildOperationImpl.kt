/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2

import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation
import org.jetbrains.kotlin.buildtools.api.v2.internal.OptionsDelegate

abstract class BuildOperationImpl<R> : BuildOperation<R> {
    private val optionsDelegate = OptionsDelegate<BuildOperation.Option<*>>()

    override fun <V> get(key: BuildOperation.Option<V>): V? = optionsDelegate[key]
    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    abstract fun execute(): R
}