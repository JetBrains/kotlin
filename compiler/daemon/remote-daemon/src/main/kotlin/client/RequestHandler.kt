/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package client

import FileChunkStrategy
import com.google.protobuf.kotlin.toByteString
import computeSha256
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.server.CompilationMetadataGrpc
import org.jetbrains.kotlin.server.CompileRequestGrpc
import org.jetbrains.kotlin.server.FileChunkGrpc
import org.jetbrains.kotlin.server.FileTransferRequestGrpc
import toCompilationOptionsGrpc
import java.io.File

class RequestHandler(private val fileChunkStrategy: FileChunkStrategy) {

    fun buildCompilationMetadata(
        sessionId: Int,
        compilationOptions: CompilationOptions,
        compilerArguments: Array<out String>,
        fileCount: Int
    ): CompileRequestGrpc {
        val builder = CompilationMetadataGrpc
            .newBuilder()
            .setSessionId(sessionId)
            .setCompilationOptions(compilationOptions.toCompilationOptionsGrpc())
            .setFileCount(fileCount)

        compilerArguments.forEach { argument ->
            builder.addCompilerArguments(argument)
        }
        return CompileRequestGrpc.newBuilder().setMetadata(builder.build()).build()
    }

    fun buildFileChunkStream(filePath: String): Flow<CompileRequestGrpc> {
        return flow {
            val file = File(filePath)
            fileChunkStrategy.chunk(file).collect { chunk ->
                val fileChunk = FileChunkGrpc.newBuilder()
                    .setFilePath(filePath)
                    .setContent(chunk.content.toByteString())
                    .setFileSize(file.length())
                    .setIsLast(chunk.isLast)
                    .build()

                emit(CompileRequestGrpc.newBuilder().setSourceFileChunk(fileChunk).build())
            }
        }
    }

    fun buildFileTransferRequestStream(sourceFiles: List<File>): Flow<CompileRequestGrpc> {
        return flow {
            sourceFiles.forEach { file ->
                emit(
                    CompileRequestGrpc
                        .newBuilder()
                        .setFileTransferRequest(
                            FileTransferRequestGrpc
                                .newBuilder()
                                .setFilePath(file.path)
                                .setFileFingerprint(computeSha256(file))
                        )
                        .build()
                )
            }
        }
    }

    fun receiveFile(filePath: String, newFilePath: String, chunk: ByteArray, isLast: Boolean){
        fileChunkStrategy.addChunks(filePath, chunk)
        if (isLast) {
            println("reconstructing file $filePath to $newFilePath, size = ${File(filePath).length()} bytes")
            fileChunkStrategy.reconstruct(filePath, newFilePath)
        }
    }
}