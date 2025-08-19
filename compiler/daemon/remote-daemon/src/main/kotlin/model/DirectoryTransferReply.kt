/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.DirectoryTransferReplyGrpc

data class DirectoryTransferReply(
    val directoryPath: String,
    val directoryFingerprint: String,
    val isEntireDirectoryPresent: Boolean,
    val missingFiles: List<FileIdentifier>
) : CompileResponse

fun DirectoryTransferReplyGrpc.toDomain(): DirectoryTransferReply {
    return DirectoryTransferReply(
        directoryPath,
        directoryFingerprint,
        isEntireDirectoryPresent,
        missingFilesList.map { it.toDomain() }
    )
}

fun DirectoryTransferReply.toGrpc(): DirectoryTransferReplyGrpc {
    return DirectoryTransferReplyGrpc.newBuilder()
        .setDirectoryPath(directoryPath)
        .setDirectoryFingerprint(directoryFingerprint)
        .setIsEntireDirectoryPresent(isEntireDirectoryPresent)
        .addAllMissingFiles(missingFiles.map { it.toGrpc() })
        .build()
}