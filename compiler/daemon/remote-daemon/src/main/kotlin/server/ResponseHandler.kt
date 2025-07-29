/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server

import FileChunkStrategy
import com.google.protobuf.kotlin.toByteString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.kotlin.server.CompileResponseGrpc
import org.jetbrains.kotlin.server.FileChunkGrpc
import org.jetbrains.kotlin.server.FileTransferReplyGrpc
import java.io.File

class ResponseHandler(private val fileChunkStrategy: FileChunkStrategy) {

    fun buildFileTransferReply(filePath: String, isPresent: Boolean): CompileResponseGrpc {
        return CompileResponseGrpc
            .newBuilder()
            .setFileTransferReply(
                FileTransferReplyGrpc
                    .newBuilder()
                    .setFilePath(filePath)
                    .setIsPresent(isPresent)
                    .build()
            ).build()
    }


    fun buildFileChunkStream(directory: File): Flow<CompileResponseGrpc> = flow {
        directory.listFiles()?.forEach { file ->
            if (!file.isDirectory) {

                fileChunkStrategy.chunk(file).collect { chunk ->
                    val fileChunk = FileChunkGrpc.newBuilder()
                        .setFilePath(file.path)
                        .setContent(chunk.content.toByteString())
                        .setFileSize(file.length())
                        .setIsLast(chunk.isLast)
                        .build()

                    val response = CompileResponseGrpc.newBuilder()
                        .setCompiledFileChunk(fileChunk)
                        .build()

                    emit(response)
                }
            }
        }
    }
}