/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.client

import client.GrpcClientRemoteCompilationService
import common.CLIENT_COMPILED_DIR
import common.OneFileOneChunkStrategy
import common.buildAbsPath
import common.computeSha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import main.kotlin.server.ServerImplType
import model.CompilationMetadata
import model.CompilationResult
import model.CompileRequest
import model.CompileResponse
import model.CompilerMessage
import model.FileChunk
import model.FileTransferReply
import model.FileTransferRequest
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import java.io.File

class RemoteCompilationClient(
    val serverImplType: ServerImplType
) {

    init {
        File(CLIENT_COMPILED_DIR).mkdir()
    }

    private val client = GrpcClientRemoteCompilationService()

    suspend fun compile(compilerArguments: Array<out String>, compilationOptions: CompilationOptions, sourceFiles: List<File>) {
        val requestChannel = Channel<CompileRequest>(capacity = Channel.UNLIMITED)
        val responseChannel = Channel<CompileResponse>(capacity = Channel.UNLIMITED)

        val fileChunkStrategy = OneFileOneChunkStrategy()

        coroutineScope {
            // start consuming response
            launch(Dispatchers.IO) {
                client.compile(requestChannel.receiveAsFlow()).collect { responseChannel.send(it) }
            }

            launch {
                responseChannel.consumeAsFlow().collect {
                    when (it) {
                        is FileTransferReply -> {
                            if (!it.isPresent) {
                                launch {
                                    fileChunkStrategy.chunk(File(it.filePath)).collect { chunk ->
                                        requestChannel.send(chunk)
                                    }
                                }
                            }
                        }
                        is FileChunk -> {
                            launch {
                                val fileName = it.filePath.split("/").last()
                                fileChunkStrategy.addChunks(it.filePath, it.content)
                                if (it.isLast) {
                                    fileChunkStrategy.reconstruct(
                                        fileChunkStrategy.getChunks(it.filePath),
                                        buildAbsPath("$CLIENT_COMPILED_DIR/$fileName")
                                    )
                                }
                            }
                        }
                        is CompilationResult -> {
                            println("COMPILATION RESULT: $it")
                        }
                        is CompilerMessage -> {
                            println("COMPILER MESSAGE: $it")
                        }
                    }
                }
            }

            launch {
                // as a first step we want to send compilation metadata
                requestChannel.send(
                    CompilationMetadata(
                        "mycustomproject",
                        sourceFiles.size,
                        compilerArguments.toMutableList(),
                        compilationOptions
                    )
                )

                // then we will send a question for a server for each file
                sourceFiles.forEach {
                    requestChannel.send(
                        FileTransferRequest(
                            it.path,
                            computeSha256(it)
                        )
                    )
                }
            }
        }
    }

}
suspend fun main(args: Array<String>) {
    val client = RemoteCompilationClient(ServerImplType.GRPC)

    val compilationOptions = CompilationOptions(
        compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
        targetPlatform = CompileService.TargetPlatform.JVM,
        reportSeverity = 0,
        reportCategories = arrayOf(),
        requestedCompilationResults = arrayOf(),
    )

    val compilerArguments = arrayOf<String>()

    val sourceFiles = listOf(
        File("src/main/kotlin/client/input/Input.kt"),
        File("src/main/kotlin/client/input/Input2.kt"),
        File("src/main/kotlin/client/input/Input3.kt")
    )

    client.compile(
        compilerArguments = compilerArguments,
        compilationOptions = compilationOptions,
        sourceFiles = sourceFiles
    )
}