/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package client.core

import benchmark.RemoteCompilationServiceImplType
import client.grpc.GrpcRemoteCompilationServiceClient
import com.example.KotlinxRpcRemoteCompilationServiceClient
import common.CLIENT_COMPILED_DIR
import common.CLIENT_TMP_DIR
import common.CompilerUtils
import common.FixedSizeChunkingStrategy
import common.RemoteCompilationService
import common.computeSha256
import common.createTarArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.serialization.cbor.cbor
import kotlinx.serialization.ExperimentalSerializationApi
import model.ArtifactType
import model.CompilationMetadata
import model.CompilationResult
import model.CompileRequest
import model.CompileResponse
import model.CompilerMessage
import model.FileChunk
import model.FileTransferReply
import model.FileTransferRequest
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import server.grpc.GrpcRemoteCompilationServerImpl
import server.kotlinxrpc.KotlinxRpcRemoteCompilationServerImpl
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
class RemoteCompilationClient(
    val clientImpl: RemoteCompilationService,
    val logging: Boolean = false
) {

    companion object {

        fun getClient(implType: RemoteCompilationServiceImplType, port: Int, logging: Boolean = false): RemoteCompilationClient{
            val clientImpl = when (implType) {
                RemoteCompilationServiceImplType.KOTLINX_RPC -> {
                    // TODO logging
                    @OptIn(ExperimentalSerializationApi::class)
                    KotlinxRpcRemoteCompilationServiceClient(
                        port,
                        serialization = { cbor() }
                    )
                }
                RemoteCompilationServiceImplType.GRPC -> {
                    GrpcRemoteCompilationServiceClient(port, logging)
                }
            }
            return RemoteCompilationClient(clientImpl)
        }
    }

    fun debug(text: String) {
        if (logging) {
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            println("[${LocalDateTime.now().format(formatter)}] [thread=${Thread.currentThread().name}] DEBUG SERVER: $text")
        }
    }

    init {
        CLIENT_COMPILED_DIR.toFile().mkdirs()
        CLIENT_TMP_DIR.toFile().mkdirs()

        // TODO: this is just a convenient for testing and debugging
        Runtime.getRuntime().addShutdownHook(
            Thread {
                CLIENT_COMPILED_DIR.deleteRecursively()
                CLIENT_TMP_DIR.deleteRecursively()
            },
        )
    }

    suspend fun compile(projectName: String, compilerArguments: List<String>, compilationOptions: CompilationOptions): CompilationResult {
        val compilerArgumentsMap = CompilerUtils.getMap(compilerArguments)

        val sourceFiles = CompilerUtils.getSourceFilePaths(compilerArgumentsMap).map { it.toFile() }
        val dependencyFiles = CompilerUtils.getDependencyFilePaths(compilerArgumentsMap).map { it.toFile() }
        val compilerPluginFiles = CompilerUtils.getCompilerPluginFilesPath(compilerArgumentsMap).map { it.toFile() }

        val fileChunks = mutableMapOf<String, MutableList<FileChunk>>()

        val requestChannel = Channel<CompileRequest>(capacity = Channel.Factory.UNLIMITED)
        val responseChannel = Channel<CompileResponse>(capacity = Channel.Factory.UNLIMITED)
        var compilationResult: CompilationResult? = null

        val fileChunkStrategy = FixedSizeChunkingStrategy()

        coroutineScope {
            // start consuming response
            val responseJob = launch(Dispatchers.IO) {
                clientImpl.compile(requestChannel.receiveAsFlow()).collect { responseChannel.send(it) }
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
                            debug("COMPILATION RESULT: $it")
                        }
                        is CompilerMessage -> {
                            debug("COMPILER MESSAGE: $it")
                        }
                    }
                }
            }

            launch {
                // as a first step we want to send compilation metadata
                requestChannel.send(
                    CompilationMetadata(
                        projectName,
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
                            it.path,
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

    suspend fun cleanup() {
        clientImpl.cleanup()
    }
}