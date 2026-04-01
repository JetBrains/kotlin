/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.compat.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.CommonCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments

/**
 * Handles forward compatibility when the API version is older than the implementation version.
 *
 * Converts between old API argument types (e.g., `String`) and new implementation types (e.g., `Path`)
 * to maintain compatibility when argument type definitions evolve between API and implementation.
 */
internal interface CommonToolArgumentValueAdapter {
    fun <V, T> mapFrom(value: T, key: CommonToolArguments.CommonToolArgument<V>): V
    fun <T, V> mapTo(value: V, key: CommonToolArguments.CommonToolArgument<V>): T
}

internal interface CommonCompilerArgumentValueAdapter : CommonToolArgumentValueAdapter {
    fun <V, T> mapFrom(value: T, key: CommonCompilerArgument<V>): V
    fun <T, V> mapTo(value: V, key: CommonCompilerArgument<V>): T
}

internal interface JvmCompilerArgumentValueAdapter : CommonCompilerArgumentValueAdapter {
    fun <V, T> mapFrom(value: T, key: JvmCompilerArguments.JvmCompilerArgument<V>): V
    fun <T, V> mapTo(value: V, key: JvmCompilerArguments.JvmCompilerArgument<V>): T
}