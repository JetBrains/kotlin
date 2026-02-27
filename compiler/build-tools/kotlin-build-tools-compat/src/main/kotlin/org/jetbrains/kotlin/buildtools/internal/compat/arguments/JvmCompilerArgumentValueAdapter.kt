/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.compat.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments

/**
 * Handles forward compatibility when the API version is older than the implementation version.
 * 
 * Converts between old API argument types (e.g., `String`) and new implementation types (e.g., `Path`)
 * to maintain compatibility when argument type definitions evolve between API and implementation.
 */
@OptIn(ExperimentalCompilerArgument::class)
internal object JvmCompilerArgumentValueAdapter {

    @Suppress("UNCHECKED_CAST")
    fun <V, T> mapFrom(value: T, key: JvmCompilerArguments.JvmCompilerArgument<V>): V? {
        return value as V
    }

    @Suppress("UNCHECKED_CAST")
    fun <V, T> mapTo(value: V, key: JvmCompilerArguments.JvmCompilerArgument<V>): T? {
        return value as T
    }
}