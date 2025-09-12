/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.MissingFilesRequestProto

data class MissingFilesRequest(
    val filePaths: List<String>,
    val artifactType: ArtifactType
) : CompileResponse

fun MissingFilesRequest.toProto(): MissingFilesRequestProto {
    return MissingFilesRequestProto
        .newBuilder()
        .addAllFilePaths(filePaths)
        .setArtifactType(artifactType.toProto())
        .build()
}

fun MissingFilesRequestProto.toDomain(): MissingFilesRequest {
    return MissingFilesRequest(
        filePathsList,
        artifactType.toDomain()
    )
}