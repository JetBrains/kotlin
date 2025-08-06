/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.CompilationResultSourceGrpc

enum class CompilationResultSource {
    COMPILER,
    CACHE
}

fun CompilationResultSource.toGrpc(): CompilationResultSourceGrpc {
    return when (this) {
        CompilationResultSource.COMPILER -> CompilationResultSourceGrpc.COMPILER
        CompilationResultSource.CACHE -> CompilationResultSourceGrpc.CACHE
    }
}