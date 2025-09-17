/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import com.google.protobuf.kotlin.toByteString
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.server.FileChunkProto

@Serializable
class FileChunk(
    val filePath: String,
    val artifactTypes: Set<ArtifactType>,
    val content: ByteArray,
    val isDirectory: Boolean,
    val isLast: Boolean
) : CompileRequest, CompileResponse

fun FileChunk.toProto(): FileChunkProto {
    return FileChunkProto.newBuilder()
        .setFilePath(filePath)
        .addAllArtifactTypes(artifactTypes.map { it.toProto() })
        .setContent(content.toByteString())
        .setIsDirectory(isDirectory)
        .setIsLast(isLast)
        .build()
}

fun FileChunkProto.toDomain(): FileChunk {
    return FileChunk(filePath, artifactTypesList.map { it.toDomain() }.toSet(), content.toByteArray(), isDirectory, isLast)
}