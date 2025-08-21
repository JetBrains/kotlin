/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.CompileResponseGrpc

sealed interface CompileResponse

fun CompileResponseGrpc.toDomain(): CompileResponse = when {
    hasCompilationResult() -> compilationResult.toDomain()
    hasFileTransferReply() -> fileTransferReply.toDomain()
    hasCompiledFileChunk() -> compiledFileChunk.toDomain()
    hasCompilerMessage() -> compilerMessage.toDomain()
    else -> throw IllegalStateException("Unknown CompileResponseGrpc type") // TODO fix
}

fun CompileResponse.toGrpc(): CompileResponseGrpc = when (this) {
    is CompilationResult -> CompileResponseGrpc.newBuilder().setCompilationResult(toGrpc()).build()
    is FileTransferReply -> CompileResponseGrpc.newBuilder().setFileTransferReply(toGrpc()).build()
    is FileChunk -> CompileResponseGrpc.newBuilder().setCompiledFileChunk(toGrpc()).build()
    is CompilerMessage -> CompileResponseGrpc.newBuilder().setCompilerMessage(toGrpc()).build()
}