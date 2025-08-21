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

class FixedSizeChunkingStrategy(
    val chunkSize: Int = FOUR_MB
) : FileChunkingStrategy() {

    // note: default gRPC message size limit is 4MB but it can be overridden
    override fun chunk(file: File, isDirectory: Boolean, artifactType: ArtifactType, filePath: String?): Flow<FileChunk> {
        return flow {
            if (file.length() <= chunkSize) {
                emit(FileChunk(filePath ?: file.path, artifactType, file.readBytes(), isDirectory, isLast = true))
            } else {
                val chunks = file
                    .readBytes()
                    .asList()
                    .chunked(chunkSize)

                chunks.forEachIndexed { index, chunk ->
                    emit(
                        FileChunk(
                            filePath ?: file.path,
                            artifactType,
                            chunk.toByteArray(),
                            isDirectory,
                            isLast = (index == chunks.size - 1)
                        )
                    )
                }
            }
        }
    }
}