/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.library.impl

enum class BuiltInsPlatform {
    JVM, JS, NATIVE, WASM, COMMON;

    companion object {
        fun parseFromString(name: String): BuiltInsPlatform? = values().firstOrNull { it.name == name }
    }
}

fun List<String>.toSpaceSeparatedString(): String = joinToString(separator = " ") {
    if (it.contains(" ")) "\"$it\"" else it
}
