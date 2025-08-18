/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.FileTransferReplyGrpc

data class FileTransferReply(
    val filePath: String,
    val isPresent: Boolean,
    val fileType: FileType
) : CompileResponse

fun FileTransferReplyGrpc.toDomain(): FileTransferReply {
    return FileTransferReply(filePath, isPresent, fileType.toDomain())
}

fun FileTransferReply.toGrpc(): FileTransferReplyGrpc {
    return FileTransferReplyGrpc.newBuilder()
        .setFilePath(filePath)
        .setIsPresent(isPresent)
        .setFileType(fileType.toGrpc())
        .build()
}