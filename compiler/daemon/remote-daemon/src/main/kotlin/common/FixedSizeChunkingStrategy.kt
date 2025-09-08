/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import model.FileChunk
import model.ArtifactType
import java.io.File
import java.io.InputStream

class FixedSizeChunkingStrategy(
    val chunkSize: Int = FOUR_MB
) : FileChunkingStrategy() {

    // note: default gRPC message size limit is 4MB but it can be overridden
    override fun chunk(
        inputStream: InputStream,
        isDirectory: Boolean,
        artifactType: ArtifactType,
        filePath: String,
    ): Flow<FileChunk> {
        return flow {
            inputStream.buffered().use { stream ->
                val buffer = ByteArray(chunkSize)
                var bytesRead: Int

                while (true) {
                    bytesRead = stream.read(buffer)
                    if (bytesRead == -1) break

                    // only send the actual bytes read, not the full buffer
                    val chunkData = if (bytesRead < chunkSize) {
                        buffer.copyOf(bytesRead)
                    } else {
                        buffer.copyOf()
                    }

                    val isLast = if (bytesRead < chunkSize) {
                        true
                    } else {
                        stream.mark(1)
                        val nextByte = stream.read()
                        if (nextByte == -1) {
                            true
                        } else {
                            stream.reset()
                            false
                        }
                    }

                    emit(
                        FileChunk(
                            filePath,
                            artifactType,
                            chunkData,
                            isDirectory,
                            isLast = isLast
                        )
                    )

                    if (isLast) break
                }
            }
        }
    }

    override fun chunk(
        file: File,
        isDirectory: Boolean,
        artifactType: ArtifactType,
        filePath: String,
    ): Flow<FileChunk> {
        return chunk(file.inputStream(), isDirectory, artifactType, filePath)
    }
}