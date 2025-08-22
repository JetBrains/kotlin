/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.client

import client.GrpcClientRemoteCompilationService
import common.CLIENT_COMPILED_DIR
import common.CLIENT_TMP_DIR
import common.CompilerUtils
import common.FixedSizeChunkingStrategy
import common.RemoteCompilationServiceImplType
import common.computeSha256
import common.createTarArchive
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
import model.ArtifactType
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import java.io.File
import java.nio.file.Files

class RemoteCompilationClient(
    val serverImplType: RemoteCompilationServiceImplType
) {

    init {
        CLIENT_COMPILED_DIR.toFile().mkdirs()
        CLIENT_TMP_DIR.toFile().mkdirs()
    }

    private val client = GrpcClientRemoteCompilationService()

    suspend fun compile(compilerArguments: List<String>, compilationOptions: CompilationOptions): CompilationResult {
        val compilerArgumentsMap = CompilerUtils.getMap(compilerArguments)

        val sourceFiles = CompilerUtils.getSourceFiles(compilerArgumentsMap)
        val dependencyFiles = CompilerUtils.getDependencyFiles(compilerArgumentsMap)
        val compilerPluginFiles = CompilerUtils.getCompilerPluginFiles(compilerArgumentsMap)

        val fileChunks = mutableMapOf<String, MutableList<FileChunk>>()

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

        val requestChannel = Channel<CompileRequest>(capacity = Channel.UNLIMITED)
        val responseChannel = Channel<CompileResponse>(capacity = Channel.UNLIMITED)
        var compilationResult: CompilationResult? = null

        val fileChunkStrategy = FixedSizeChunkingStrategy()

        coroutineScope {
            // start consuming response
            val responseJob = launch(Dispatchers.IO) {
                client.compile(requestChannel.receiveAsFlow()).collect { responseChannel.send(it) }
                // when the server stops closes connection, we can close our channels
                responseChannel.close()
                requestChannel.close()
            }

            launch {
                responseChannel.consumeAsFlow().collect {
                    when (it) {
                        is FileTransferReply -> {
                            if (!it.isPresent) {
                                launch {
                                    val file = File(it.filePath)
                                    if (Files.isDirectory(file.toPath())) {
                                        Files.createDirectories(CLIENT_TMP_DIR.resolve(file.parent))
                                        val tarFile =
                                            CLIENT_TMP_DIR.resolve(file.parent).resolve("${file.nameWithoutExtension}.tar").toFile()
                                        createTarArchive(file, tarFile)
                                        fileChunkStrategy.chunk(tarFile, true, it.artifactType, it.filePath)
                                            .collect { chunk ->
                                                requestChannel.send(chunk)
                                            }
                                        tarFile.delete()
                                    } else {
                                        fileChunkStrategy.chunk(file, isDirectory = false, it.artifactType, it.filePath)
                                            .collect { chunk ->
                                                requestChannel.send(chunk)
                                            }
                                    }
                                }
                            }
                        }
                        is FileChunk -> {
                            launch {
                                val moduleName = CompilerUtils.getModuleName(compilerArgumentsMap)
                                fileChunks.getOrPut(it.filePath) { mutableListOf() }.add(it)
                                if (it.isLast) {
                                    fileChunkStrategy.reconstruct(
                                        fileChunks.getOrDefault(it.filePath, listOf()),
                                        CLIENT_COMPILED_DIR.resolve(moduleName),
                                        it.filePath,
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

                // then we will ask the server if a source file is present in the server cache
                sourceFiles.forEach {
                    requestChannel.send(
                        FileTransferRequest(
                            it.path,
                            computeSha256(it),
                            ArtifactType.SOURCE
                        )
                    )
                }

                dependencyFiles.forEach {
                    requestChannel.send(
                        FileTransferRequest(
                            it.path, // we need to preserve original path for dependency
                            computeSha256(it),
                            ArtifactType.DEPENDENCY
                        )
                    )
                }

                compilerPluginFiles.forEach {
                    requestChannel.send(
                        FileTransferRequest(
                            it.path,
                            computeSha256(it),
                            ArtifactType.COMPILER_PLUGIN
                        )
                    )
                }
            }
            responseJob.join()
        }
        return compilationResult ?: throw IllegalStateException("Compilation result is null")
    }
}