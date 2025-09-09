/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.server.CompileRequestProto

@Serializable
sealed interface CompileRequest

fun CompileRequest.toProto(): CompileRequestProto = when (this) {
    is CompilationMetadata -> CompileRequestProto.newBuilder().setMetadata(toProto()).build()
    is FileChunk -> CompileRequestProto.newBuilder().setSourceFileChunk(toProto()).build()
    is FileTransferRequest -> CompileRequestProto.newBuilder().setFileTransferRequest(toProto()).build()
}