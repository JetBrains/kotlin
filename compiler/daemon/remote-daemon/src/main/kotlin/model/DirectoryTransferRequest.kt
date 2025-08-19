/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.DirectoryTransferRequestGrpc

data class DirectoryTransferRequest(
    val directoryPath: String,
    val directoryFingerprint: String,
    val directoryFiles: List<FileTransferRequest>
) : CompileRequest

fun DirectoryTransferRequest.toGrpc(): DirectoryTransferRequestGrpc {
    return DirectoryTransferRequestGrpc.newBuilder()
        .setDirectoryPath(directoryPath)
        .setDirectoryFingerprint(directoryFingerprint)
        .addAllDirectoryFiles(directoryFiles.map { it.toGrpc() })
        .build()
}

fun DirectoryTransferRequestGrpc.toDomain(): DirectoryTransferRequest {
    return DirectoryTransferRequest(
        directoryPath,
        directoryFingerprint,
        directoryFilesList.map { it.toDomain() }
    )
}
