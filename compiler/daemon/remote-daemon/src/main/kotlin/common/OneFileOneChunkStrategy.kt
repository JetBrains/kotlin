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

class OneFileOneChunkStrategy : FileChunkingStrategy() {

    override fun chunk(file: File, artifactType: ArtifactType): Flow<FileChunk> {
        return flow {
            emit(FileChunk(file.path, artifactType, file.readBytes(), isLast = true))
        }
    }
}