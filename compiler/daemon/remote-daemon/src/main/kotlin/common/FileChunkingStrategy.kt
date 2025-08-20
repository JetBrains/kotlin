/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import kotlinx.coroutines.flow.Flow
import model.FileChunk
import model.ArtifactType
import java.io.File

abstract class FileChunkingStrategy {

    abstract fun chunk(file: File, artifactType: ArtifactType): Flow<FileChunk>

    fun reconstruct(fileChunks: Collection<FileChunk>, newFilePath: String): File {
        try {
            // TODO here we are basically return empty file, would it be better to return null?
            val newFile = File(newFilePath)
            if (fileChunks.isEmpty()) return newFile
            // TODO consider sending filesize in metadata
            val totalSize = fileChunks.sumOf { it.content.size }
            val completeContent = ByteArray(totalSize)

            var offset = 0
            fileChunks.forEach { chunk ->
                // chunk is already ByteArray, no need for conversion
                chunk.content.copyInto(completeContent, offset)
                offset += chunk.content.size
            }

            newFile.writeBytes(completeContent)
            // TODO maybe cleanup chunks from the hashmap
            println("File $newFilePath has been successfully reconstructed")
            return newFile
        } catch (e: Exception) {
            println("Error while reconstructing file $fileChunks")
            throw e
        }
    }
}