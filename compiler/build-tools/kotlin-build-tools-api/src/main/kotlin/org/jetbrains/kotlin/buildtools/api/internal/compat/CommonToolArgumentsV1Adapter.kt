/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.api.arguments.internal.compat

import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.NOWARN
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.VERBOSE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.VERSION
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.WERROR
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.WEXTRA
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments as V2CommonToolArguments

internal open class CommonToolArgumentsV1Adapter : V2CommonToolArguments {
    private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    override operator fun <V> `get`(key: V2CommonToolArguments.CommonToolArgument<V>): V = optionsMap[key.id] as V

    override operator fun <V> `set`(key: V2CommonToolArguments.CommonToolArgument<V>, `value`: V) {
        optionsMap[key.id] = `value`
    }

    operator fun contains(key: V2CommonToolArguments.CommonToolArgument<*>): Boolean = key.id in optionsMap

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalCompilerArgument::class)
    open fun toArgumentStrings(additionalArguments: List<String> = emptyList()): List<String> {
        val arguments = mutableListOf<String>()
        if ("VERSION" in optionsMap) { arguments.add("-version=" + get(VERSION)) }
        if ("VERBOSE" in optionsMap) { arguments.add("-verbose=" + get(VERBOSE)) }
        if ("NOWARN" in optionsMap) { arguments.add("-nowarn=" + get(NOWARN)) }
        if ("WERROR" in optionsMap) { arguments.add("-Werror=" + get(WERROR)) }
        if ("WEXTRA" in optionsMap) { arguments.add("-Wextra=" + get(WEXTRA)) }
        arguments.addAll(additionalArguments)
        return arguments
    }
}
