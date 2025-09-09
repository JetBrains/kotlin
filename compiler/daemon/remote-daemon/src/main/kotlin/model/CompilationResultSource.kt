/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.CompilationResultSourceProto

enum class CompilationResultSource {
    COMPILER,
    CACHE
}

fun CompilationResultSource.toProto(): CompilationResultSourceProto {
    return when (this) {
        CompilationResultSource.COMPILER -> CompilationResultSourceProto.COMPILER
        CompilationResultSource.CACHE -> CompilationResultSourceProto.CACHE
    }
}

fun CompilationResultSourceProto.toDomain(): CompilationResultSource {
    return when (this) {
        CompilationResultSourceProto.COMPILER -> CompilationResultSource.COMPILER
        CompilationResultSourceProto.CACHE -> CompilationResultSource.CACHE
        CompilationResultSourceProto.UNRECOGNIZED -> CompilationResultSource.COMPILER // TODO double check
    }
}