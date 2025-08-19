/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.client

import client.GrpcClientRemoteCompilationService
import common.CLIENT_COMPILED_DIR
import common.CompilerUtils
import common.OneFileOneChunkStrategy
import common.RemoteCompilationServiceImplType
import common.buildAbsPath
import common.computeSha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import model.CompilationMetadata
import model.CompilationResult
import model.CompileRequest
import model.CompileResponse
import model.CompilerMessage
import model.FileChunk
import model.FileTransferReply
import model.FileTransferRequest
import model.FileType
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import java.io.File

class RemoteCompilationClient(
    val serverImplType: RemoteCompilationServiceImplType
) {

    init {
        File(CLIENT_COMPILED_DIR).mkdir()
    }

    private val client = GrpcClientRemoteCompilationService()

    suspend fun compile(compilerArguments: List<String>, compilationOptions: CompilationOptions): CompilationResult {
        val compilerArgumentsMap = CompilerUtils.getMap(compilerArguments)

        val sourceFiles = CompilerUtils.getSourceFiles(compilerArgumentsMap)
        val dependencyFiles = CompilerUtils.getDependencyFiles(compilerArgumentsMap)
        val compilerPluginFiles = CompilerUtils.getCompilerPluginFiles(compilerArgumentsMap)

//        for (sf in sourceFiles) {
//            println("source file: ${sf.path}")
//        }
//
//        for (df in dependencyFiles) {
//            println("dependency file: ${df.path}")
//        }
//
//        for (pff in compilerPluginFiles) {
//            println("plugin file: ${pff.path}")
//        }
//

        val requestChannel = Channel<CompileRequest>(capacity = Channel.UNLIMITED)
        val responseChannel = Channel<CompileResponse>(capacity = Channel.UNLIMITED)
        var compilationResult: CompilationResult? = null

        val fileChunkStrategy = OneFileOneChunkStrategy()

        coroutineScope {
            // start consuming response
            val responseJob = launch(Dispatchers.IO) {
                client.compile(requestChannel.receiveAsFlow()).collect { responseChannel.send(it) }
                // when server stops closes connection, we can close our channels
                responseChannel.close()
                requestChannel.close()
            }

            launch {
                responseChannel.consumeAsFlow().collect {
                    when (it) {
                        is FileTransferReply -> {
                            if (!it.isPresent) {
                                launch {
                                    val file
                                    fileChunkStrategy.chunk(File(it.filePath), it.fileType).collect { chunk ->
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
                            compilationResult = it
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
                        dependencyFiles.size,
                        compilerPluginFiles.size,
                        compilerArgumentsMap,
                        compilationOptions
                    )
                )

                // then we will ask server if source file is present in server cache
                sourceFiles.forEach {
                    requestChannel.send(
                        FileTransferRequest(
                            it.path,
                            computeSha256(it),
                            FileType.SOURCE
                        )
                    )
                }

                dependencyFiles.forEach {
                    if (it.isDirectory) {

                    } else {
                        requestChannel.send(
                            FileTransferRequest(
                                it.path,
                                computeSha256(it),
                                FileType.DEPENDENCY
                            )
                        )
                    }

                }

                compilerPluginFiles.forEach {
                    requestChannel.send(
                        FileTransferRequest(
                            it.path,
                            computeSha256(it),
                            FileType.COMPILER_PLUGIN
                        )
                    )
                }
            }
            responseJob.join()
        }
        return compilationResult ?: throw IllegalStateException("Compilation result is null")
    }
}

suspend fun main(args: Array<String>) {
    val client = RemoteCompilationClient(RemoteCompilationServiceImplType.GRPC)

    val compilationOptions = CompilationOptions(
        compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
        targetPlatform = CompileService.TargetPlatform.JVM,
        reportSeverity = 0,
        reportCategories = arrayOf(),
        requestedCompilationResults = arrayOf(),
    )

    val compilerArguments = mapOf<String, String>()

    val sourceFiles = listOf(
        File("src/main/kotlin/client/input/Input.kt"),
        File("src/main/kotlin/client/input/Input2.kt"),
        File("src/main/kotlin/client/input/Input3.kt")
    )

//    client.compile(
//        compilerArguments = compilerArguments,
//        compilationOptions = compilationOptions,
//        sourceFiles = sourceFiles
//    )
}