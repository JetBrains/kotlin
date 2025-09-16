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
import common.SERVER_TMP_CACHE_DIR
import common.computeSha256
import common.createTarArchiveStream
import kotlinx.coroutines.CoroutineScope
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
import model.MissingFilesRequest
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import java.nio.file.Paths


import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name

@OptIn(ExperimentalPathApi::class)
class RemoteCompilationClient(
    val clientImpl: RemoteCompilationService,
    val logging: Boolean = false
) {

    val fileChunkStrategy = FixedSizeChunkingStrategy()

    companion object {

        fun getClient(
            implType: RemoteCompilationServiceImplType,
            host: String = "localhost",
            port: Int,
            logging: Boolean = false
        ): RemoteCompilationClient {
            val clientImpl = when (implType) {
                RemoteCompilationServiceImplType.KOTLINX_RPC -> {
                    // TODO logging
                    @OptIn(ExperimentalSerializationApi::class)
                    KotlinxRpcRemoteCompilationServiceClient(
                        host,
                        port,
                        serialization = { cbor() }
                    )
                }
                RemoteCompilationServiceImplType.GRPC -> {
                    GrpcRemoteCompilationServiceClient(host, port, logging)
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
        Files.createDirectories(CLIENT_COMPILED_DIR)
        Files.createDirectories(CLIENT_TMP_DIR)
    }

    suspend fun compile(projectName: String, compilerArguments: List<String>, compilationOptions: CompilationOptions): CompilationResult {
        val parsedArgs = CompilerUtils.parseArgs(compilerArguments)
        val sourceFiles = CompilerUtils.getSourceFiles(parsedArgs)
        val dependencyFiles = CompilerUtils.getDependencyFiles(parsedArgs)
        val compilerPluginFiles = CompilerUtils.getXPluginFiles(parsedArgs)

        val fileChunks = ConcurrentHashMap<String, MutableList<FileChunk>>()

        val requestChannel = Channel<CompileRequest>(capacity = Channel.UNLIMITED)
        val responseChannel = Channel<CompileResponse>(capacity = Channel.UNLIMITED)
        var compilationResult: CompilationResult? = null


        coroutineScope {
            // start consuming response
            val responseJob = launch(Dispatchers.IO) {
                clientImpl.compile(requestChannel.receiveAsFlow()).collect { responseChannel.send(it) }
                // when the server stops closes connection, we can close our channels
                responseChannel.close()
                requestChannel.close()
            }

            // responding on server replies
            launch {
                responseChannel.consumeAsFlow().collect {
                    when (it) {
                        is FileTransferReply -> {
                            if (!it.isPresent) {
                                launch(Dispatchers.IO) {
                                    val file = File(it.filePath)
                                    if (Files.isDirectory(file.toPath())) {
                                        createTarArchiveStream(file).use { tarStream ->
                                            fileChunkStrategy
                                                .chunk(
                                                    tarStream,
                                                    true,
                                                    it.artifactType,
                                                    it.filePath
                                                ).collect { chunk ->
                                                    requestChannel.send(chunk)
                                                }
                                        }
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
                            launch(Dispatchers.IO) {
                                fileChunks.getOrPut(it.filePath) { mutableListOf() }.add(it)
                                if (it.isLast) {
                                    val allFileChunks = fileChunks.getOrDefault(it.filePath, listOf())
                                    if (it.artifactType == ArtifactType.IC_CACHE && it.isDirectory) {
                                        val clientPath = Paths.get(it.filePath)
                                        fileChunkStrategy.reconstruct(
                                            allFileChunks,
                                            clientPath.parent,
                                            clientPath.name
                                        )
                                    }
                                    if (it.artifactType == ArtifactType.RESULT) {
                                        val moduleName = CompilerUtils.getModuleName(parsedArgs)
                                        fileChunkStrategy.reconstruct(
                                            allFileChunks,
                                            CLIENT_COMPILED_DIR.resolve(moduleName),
                                            it.filePath,
                                        )
                                    }
                                }
                            }
                        }
                        is MissingFilesRequest -> {
                            requestTransferOfFiles(it.filePaths.map { fp -> File(fp) }, it.artifactType, requestChannel)
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

            // sending data to the server
            launch {
                // as a first step we want to send compilation metadata
                requestChannel.send(
                    CompilationMetadata(
                        projectName,
                        sourceFiles.size,
                        dependencyFiles.size,
                        compilerPluginFiles.size,
                        compilerArguments,
                        compilationOptions
                    )
                )

                when (compilationOptions) {
                    is IncrementalCompilationOptions -> {
                        val srcChanges = compilationOptions.sourceChanges
                        if (srcChanges is SourcesChanges.Known) {
                            sendSourceChangesDirectlyWithoutRequest(srcChanges, requestChannel)
                        }

                        val cpChanges = compilationOptions.classpathChanges
                        if (cpChanges is ClasspathChanges.ClasspathSnapshotEnabled) {
                            requestTransferOfClasspathSnapshotFiles(cpChanges.classpathSnapshotFiles, requestChannel)
                        }
                    }
                    is CompilationOptions -> {
                        requestTransferOfFiles(sourceFiles, ArtifactType.SOURCE, requestChannel)
                        requestTransferOfFiles(dependencyFiles, ArtifactType.DEPENDENCY, requestChannel)
                        requestTransferOfFiles(compilerPluginFiles, ArtifactType.COMPILER_PLUGIN, requestChannel)
                    }
                }


            }
            responseJob.join()
        }
        return compilationResult ?: throw IllegalStateException("Compilation result is null")
    }

    suspend fun cleanup() {
        clientImpl.cleanup()
    }

    private fun CoroutineScope.requestTransferOfClasspathSnapshotFiles(
        classpathSnapshotFiles: ClasspathSnapshotFiles,
        requestChannel: Channel<CompileRequest>
    ) {
        classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.forEach {
            launch(Dispatchers.IO) {
                requestChannel.send(
                    FileTransferRequest(it.path, computeSha256(it), ArtifactType.CLASSPATH_ENTRY_SNAPSHOT)
                )
            }
        }
    }

    private fun CoroutineScope.sendSourceChangesDirectlyWithoutRequest(
        sourcesChanges: SourcesChanges.Known,
        requestChannel: Channel<CompileRequest>
    ) {
        sourcesChanges.modifiedFiles.forEach {
            launch(Dispatchers.IO) {
                fileChunkStrategy.chunk(
                    it,
                    isDirectory = Files.isDirectory(it.toPath()),
                    ArtifactType.SOURCE,
                    it.absolutePath
                ).collect { chunk ->
                    requestChannel.send(chunk)
                }
            }
        }
        // TODO question: do we need to somehow handle removal of files?
    }

    private fun CoroutineScope.requestTransferOfFiles(
        files: List<File>,
        artifactType: ArtifactType,
        requestChannel: Channel<CompileRequest>
    ) {
        files.forEach { file ->
            launch(Dispatchers.IO) {
                requestChannel.send(FileTransferRequest(file.path, computeSha256(file), artifactType))
            }
        }
    }
}

suspend fun main() {
    val client = RemoteCompilationClient.getClient(RemoteCompilationServiceImplType.GRPC, "localhost", 8000)
    client.cleanup()
    client.compile(
        "test",
        listOf("-d", "/Users/michal.svec/Desktop/ic-example/build/classes/kotlin/main"),
        IncrementalCompilationOptions(
            sourceChanges = SourcesChanges.Known(
                modifiedFiles = listOf(
                    File("/Users/michal.svec/Desktop/ic-example/src/main/kotlin/Main.kt"),
                ),
                removedFiles = listOf()
            ),
            classpathChanges = ClasspathChanges.ClasspathSnapshotDisabled,
            workingDir = File("/Users/michal.svec/Desktop/ic-example/build/kotlin/compileKotlin/cacheable"),
            compilerMode = CompilerMode.INCREMENTAL_COMPILER,
            targetPlatform = CompileService.TargetPlatform.JVM,
            outputFiles = listOf(
                File("/Users/michal.svec/Desktop/ic-example/build/classes/kotlin/main"),
                File("/Users/michal.svec/Desktop/ic-example/build/kotlin/compileKotlin/cacheable"),
                File("/Users/michal.svec/Desktop/ic-example/build/kotlin/compileKotlin/local-state"),
                File("/Users/michal.svec/Desktop/ic-example/build/kotlin/compileKotlin")
            ),
            reportCategories = emptyArray(),
            reportSeverity = 0,
            requestedCompilationResults = emptyArray(),
            useJvmFirRunner = false,
            rootProjectDir = null,
            buildDir = null
        )
    )

}