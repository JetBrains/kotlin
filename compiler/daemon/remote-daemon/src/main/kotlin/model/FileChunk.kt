/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import com.google.protobuf.kotlin.toByteString
import org.jetbrains.kotlin.server.FileChunkGrpc

class FileChunk(
    val filePath: String,
    val artifactType: ArtifactType,
    val content: ByteArray,
    val isDirectory: Boolean,
    val isLast: Boolean
) : CompileRequest, CompileResponse

fun FileChunk.toGrpc(): FileChunkGrpc {
    return FileChunkGrpc.newBuilder()
        .setFilePath(filePath)
        .setFileType(artifactType.toGrpc())
        .setContent(content.toByteString())
        .setIsDirectory(isDirectory)
        .setIsLast(isLast)
        .build()
}

fun FileChunkGrpc.toDomain(): FileChunk {
    return FileChunk(filePath, fileType.toDomain(), content.toByteArray(), isDirectory, isLast)
}