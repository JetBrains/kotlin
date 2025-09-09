/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.server.FileTransferReplyProto

@Serializable
data class FileTransferReply(
    val filePath: String,
    val isPresent: Boolean,
    val artifactType: ArtifactType
) : CompileResponse

fun FileTransferReplyProto.toDomain(): FileTransferReply {
    return FileTransferReply(filePath, isPresent, fileType.toDomain())
}

fun FileTransferReply.toProto(): FileTransferReplyProto {
    return FileTransferReplyProto.newBuilder()
        .setFilePath(filePath)
        .setIsPresent(isPresent)
        .setFileType(artifactType.toProto())
        .build()
}