/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.CompilationResultGrpc
import org.jetbrains.kotlin.server.CompileResponseGrpc

data class CompilationResult(
    val exitCode: Int,
    val compilationResultSource: CompilationResultSource
) : CompileResponse

fun CompilationResult.toGrpc(): CompilationResultGrpc {
    return CompilationResultGrpc
        .newBuilder()
        .setExitCode(exitCode)
        .setResultSource(compilationResultSource.toGrpc())
        .build()
}

fun CompilationResultGrpc.toDomain(): CompilationResult {
    return CompilationResult(exitCode, resultSource.toDomain())
}

fun CompilationResultGrpc.toCompileResponse(): CompileResponseGrpc{
    return CompileResponseGrpc
        .newBuilder()
        .setCompilationResult(this)
        .build()
}