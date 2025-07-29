/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import kotlinx.coroutines.flow.Flow
import model.FileChunk
import java.io.File

abstract class FileChunkingStrategy {

    // TODO: we are currently using filePath as the key, but using fileHash might be less error prone
    private val chunks = mutableMapOf<String, MutableList<ByteArray>>()

    fun addChunks(fileName:String, chunk: ByteArray) {
        chunks.getOrPut(fileName) { mutableListOf() }.add(chunk)
    }

    fun addChunks(fileName:String, chunks: List<ByteArray>) {
        this.chunks.getOrPut(fileName) { mutableListOf() }.addAll(chunks)
    }

    abstract fun chunk(file: File): Flow<FileChunk>

    fun reconstruct(filePath: String, newFilePath: String): File {
        try {
            // TODO here we are basically return empty file, would it be better to return null?
            val allFileChunks = chunks.getOrDefault(filePath, mutableListOf())
            val newFile = File(newFilePath)
            if (allFileChunks.isEmpty()) return newFile
            // TODO consider sending filesize in metadata
            val totalSize = allFileChunks.sumOf { it.size }
            val completeContent = ByteArray(totalSize)

            var offset = 0
            allFileChunks.forEach { chunk ->
                // chunk is already ByteArray, no need for conversion
                chunk.copyInto(completeContent, offset)
                offset += chunk.size
            }

            newFile.writeBytes(completeContent)
            // TODO maybe cleanup chunks from the hashmap
            println("File $newFilePath has been successfully reconstructed")
            return newFile
        }catch (e: Exception){
            println("Error while reconstructing file $filePath")
            throw e
        }
    }
}