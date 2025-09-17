/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.server.CompileResponseProto

@Serializable
sealed interface CompileResponse

fun CompileResponseProto.toDomain(): CompileResponse = when {
    hasCompilationResult() -> compilationResult.toDomain()
    hasFileTransferReply() -> fileTransferReply.toDomain()
    hasCompiledFileChunk() -> compiledFileChunk.toDomain()
    hasCompilerMessage() -> compilerMessage.toDomain()
    hasMissingArtifactsRequest() -> missingArtifactsRequest.toDomain()
    else -> throw IllegalStateException("Unknown CompileResponseGrpc type") // TODO fix
}

fun CompileResponse.toProto(): CompileResponseProto = when (this) {
    is CompilationResult -> CompileResponseProto.newBuilder().setCompilationResult(toProto()).build()
    is FileTransferReply -> CompileResponseProto.newBuilder().setFileTransferReply(toProto()).build()
    is FileChunk -> CompileResponseProto.newBuilder().setCompiledFileChunk(toProto()).build()
    is CompilerMessage -> CompileResponseProto.newBuilder().setCompilerMessage(toProto()).build()
    is MissingArtifactsRequest -> CompileResponseProto.newBuilder().setMissingArtifactsRequest(toProto()).build()
}