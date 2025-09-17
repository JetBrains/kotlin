/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.server.FileTransferRequestProto

@Serializable
data class FileTransferRequest(
    val filePath: String,
    val fileFingerprint: String,
    val artifactTypes: Set<ArtifactType>
) : CompileRequest

fun FileTransferRequest.toProto(): FileTransferRequestProto {
    return FileTransferRequestProto.newBuilder()
        .setFilePath(filePath)
        .setFileFingerprint(fileFingerprint)
        .addAllArtifactTypes(artifactTypes.map { it.toProto() })
        .build()
}

fun FileTransferRequestProto.toDomain(): FileTransferRequest {
    return FileTransferRequest(
        filePath,
        fileFingerprint,
        artifactTypesList.map { it.toDomain() }.toSet()
    )
}