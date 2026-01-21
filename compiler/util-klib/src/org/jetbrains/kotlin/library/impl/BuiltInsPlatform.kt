/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

enum class BuiltInsPlatform {
    JVM, JS, NATIVE, WASM, COMMON;

    companion object {
        fun parseFromString(name: String): BuiltInsPlatform? = values().firstOrNull { it.name == name }
    }
}
