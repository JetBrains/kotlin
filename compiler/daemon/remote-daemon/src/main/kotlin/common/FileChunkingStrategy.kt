/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import kotlinx.coroutines.flow.Flow
import model.FileChunk
import model.ArtifactType
import java.io.File
import java.util.UUID

abstract class FileChunkingStrategy {

    abstract fun chunk(file: File, isDirectory: Boolean, artifactType: ArtifactType, filePath: String? = null): Flow<FileChunk>

    fun reconstruct(fileChunks: Collection<FileChunk>, folder: String): File{
        try {
            // TODO here we are basically return empty file, would it be better to return null?
            val filePath = "${folder}/${UUID.randomUUID()}"
            val file = File(filePath)
            if (fileChunks.isEmpty()) return file
            // TODO consider sending filesize in metadata
            val totalSize = fileChunks.sumOf { it.content.size }
            val completeContent = ByteArray(totalSize)

            var offset = 0
            fileChunks.forEach { chunk ->
                chunk.content.copyInto(completeContent, offset)
                offset += chunk.content.size
            }

            if (fileChunks.last().isDirectory) {
                val temporaryTar = File("$filePath.tar")
                temporaryTar.writeBytes(completeContent)
                extractTarArchive(temporaryTar, file)
                // TODO delete temporary tar
                println("[RECONSTRUCTION] folder ${fileChunks.last().filePath} has been successfully reconstructed to ${file.absolutePath}")
            } else {
                file.writeBytes(completeContent)
                println("[RECONSTRUCTION] file ${fileChunks.last().filePath} has been successfully reconstructed to ${file.absolutePath} ")
            }
            println("[RECONSTRUCTION] file exists: ${file.exists()}")
            return file
        } catch (e: Exception) {
            println("Error while reconstructing file $fileChunks")
            throw e
        }
    }
}