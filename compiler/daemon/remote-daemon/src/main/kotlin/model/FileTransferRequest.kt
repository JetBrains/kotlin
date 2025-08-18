/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.FileTransferRequestGrpc

data class FileTransferRequest(
    val filePath: String,
    val fileFingerprint: String,
    val fileType: FileType
) : CompileRequest

fun FileTransferRequest.toGrpc(): FileTransferRequestGrpc {
    return FileTransferRequestGrpc.newBuilder()
        .setFilePath(filePath)
        .setFileFingerprint(fileFingerprint)
        .setFileType(fileType.toGrpc())
        .build()
}

fun FileTransferRequestGrpc.toDomain(): FileTransferRequest {
    return FileTransferRequest(
        filePath,
        fileFingerprint,
        fileType.toDomain()
    )
}