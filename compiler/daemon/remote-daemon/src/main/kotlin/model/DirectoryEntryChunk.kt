/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.DirectoryEntryChunkGrpc

data class DirectoryEntryChunk(
    val directoryPath: String,
    val isLastDirectoryEntry: Boolean,
    val fileChunk: FileChunk,
) : CompileRequest

fun DirectoryEntryChunk.toGrpc(): DirectoryEntryChunkGrpc {
    return DirectoryEntryChunkGrpc.newBuilder()
        .setDirectoryPath(directoryPath)
        .setIsLastDirectoryEntry(isLastDirectoryEntry)
        .setFileChunk(fileChunk.toGrpc())
        .build()
}

fun DirectoryEntryChunkGrpc.toDomain(): DirectoryEntryChunk {
    return DirectoryEntryChunk(
        directoryPath,
        isLastDirectoryEntry,
        fileChunk.toDomain()
    )
}