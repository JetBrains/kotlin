/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.server.CompilationResultProto
import org.jetbrains.kotlin.server.CompileResponseProto

@Serializable
data class CompilationResult(
    val exitCode: Int,
    val compilationResultSource: CompilationResultSource
) : CompileResponse

fun CompilationResult.toProto(): CompilationResultProto {
    return CompilationResultProto
        .newBuilder()
        .setExitCode(exitCode)
        .setResultSource(compilationResultSource.toProto())
        .build()
}

fun CompilationResultProto.toDomain(): CompilationResult {
    return CompilationResult(exitCode, resultSource.toDomain())
}

fun CompilationResultProto.toCompileResponse(): CompileResponseProto{
    return CompileResponseProto
        .newBuilder()
        .setCompilationResult(this)
        .build()
}