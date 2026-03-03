/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.compat.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

/**
 * Handles forward compatibility when the API version is older than the implementation version.
 * 
 * Converts between old API argument types (e.g., `String`) and new implementation types (e.g., `Path`)
 * to maintain compatibility when argument type definitions evolve between API and implementation.
 */
@OptIn(ExperimentalCompilerArgument::class)
internal class CompilerArgumentValueAdapter<V> {

    @Suppress("UNCHECKED_CAST")
    fun <T> mapFrom(value: Any?, key: V): T? {
        return value as T?
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> mapTo(value: Any?, key: V): T? {
        return value as T
    }
}