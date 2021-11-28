/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan
import java.io.*

val VERSION_PATH = "/META-INF/kotlin-native.compiler.version"
val CompilerVersion.Companion.CURRENT: CompilerVersion
    get() {
        return InputStreamReader(this::class.java.getResourceAsStream(VERSION_PATH)).use {
            it.readLines().single().parseCompilerVersion()
        }
    }

val currentCompilerVersion = CompilerVersion.CURRENT