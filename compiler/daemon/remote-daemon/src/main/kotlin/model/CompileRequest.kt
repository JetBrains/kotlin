/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model
import org.jetbrains.kotlin.server.CompileRequestGrpc

interface CompileRequest

fun CompileRequest.toGrpc(): CompileRequestGrpc = when (this) {
    is CompilationMetadata -> CompileRequestGrpc.newBuilder().setMetadata(toGrpc()).build()
    is FileChunk -> CompileRequestGrpc.newBuilder().setSourceFileChunk(toGrpc()).build()
    is FileTransferRequest -> CompileRequestGrpc.newBuilder().setFileTransferRequest(toGrpc()).build()
    is DirectoryTransferRequest -> CompileRequestGrpc.newBuilder().setDirectoryTransferRequest(toGrpc()).build()
    is DirectoryEntryChunk -> CompileRequestGrpc.newBuilder().setDirectoryEntryChunk(toGrpc()).build()
    else -> error("Unknown CompileRequest type: ${javaClass.simpleName}")
}