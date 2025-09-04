/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.core

import common.CompilerUtils
import common.FileChunkingStrategy
import common.FixedSizeChunkingStrategy
import common.RemoteCompilationService
import common.SERVER_TMP_CACHE_DIR
import common.cleanCompilationResultPath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import model.CompilationMetadata
import model.CompilationResult
import model.CompilationResultSource
import model.CompileRequest
import model.CompileResponse
import model.CompilerMessage
import model.FileChunk
import model.FileTransferReply
import model.FileTransferRequest
import model.ArtifactType
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.JpsCompilerServicesFacade
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporter
import org.jetbrains.kotlin.daemon.report.getBuildReporter
import server.grpc.AuthServerInterceptor
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RemoteCompilationServiceImpl(
    val cacheHandler: CacheHandler,
    val workspaceManager: WorkspaceManager,
    val fileChunkingStrategy: FileChunkingStrategy,
    val compilerService: InProcessCompilerService,
    val logging: Boolean = false,
) : RemoteCompilationService {

    fun debug(text: String) {
        if (logging) {
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            println("[${LocalDateTime.now().format(formatter)}] [thread=${Thread.currentThread().name}] DEBUG SERVER: $text")
        }
    }

    override suspend fun cleanup() {
        workspaceManager.cleanup()
        cacheHandler.cleanup()
    }

    override fun compile(compileRequests: Flow<CompileRequest>): Flow<CompileResponse> {
        return channelFlow {

            val allFilesReady = CompletableDeferred<Unit>()
            val filesReceivedCounter = AtomicInteger(0)
            var totalFilesExpected = -1

            // <ClientPath, FileFromUserWorkspace>
            val sourceFiles = ConcurrentHashMap<Path, File>()
            val dependencyFiles = ConcurrentHashMap<Path, File>()
            val compilerPluginFiles = ConcurrentHashMap<Path, File>()

            val fileChunks = mutableMapOf<String, MutableList<FileChunk>>()

            fun fileAvailable(clientPath: Path, file: File, artifactType: ArtifactType) {
                when (artifactType) {
                    ArtifactType.SOURCE -> sourceFiles[clientPath] = file
                    ArtifactType.DEPENDENCY -> dependencyFiles[clientPath] = file
                    ArtifactType.COMPILER_PLUGIN -> compilerPluginFiles[clientPath] = file
                    ArtifactType.RESULT -> debug("Received illegal file type: $artifactType")
                }
                if (totalFilesExpected > 0 && filesReceivedCounter.incrementAndGet() == totalFilesExpected) {
                    allFilesReady.complete(Unit)
                }
            }

            val fileChunkStrategy = FixedSizeChunkingStrategy()
            var compilationMetadata: CompilationMetadata? = null

            val userId = AuthServerInterceptor.USER_ID_CONTEXT_KEY.get() ?: "3489439j"

            // here we consume request stream
            launch {
                compileRequests.collect {
                    when (it) {
                        is CompilationMetadata -> {
                            compilationMetadata = it
                            totalFilesExpected = it.sourceFilesCount + it.dependencyFilesCount + it.compilerPluginFilesCount
                        }
                        is FileTransferRequest -> {
                            compilationMetadata?.let { metadata ->
                                launch {
                                    val compileResponse = when (cacheHandler.isFileCached(it.fileFingerprint)) {
                                        true -> {
                                            debug("file ${it.filePath} is available in cache")
                                            val cachedFile = cacheHandler.getFile(it.fileFingerprint)
                                            val projectFile = workspaceManager.copyFileToProject(
                                                cachedFilePath = cachedFile.absolutePath,
                                                clientFilePath = it.filePath,
                                                userId,
                                                compilationMetadata.projectName
                                            )
                                            fileAvailable(Paths.get(it.filePath).toAbsolutePath().normalize(), projectFile, it.artifactType)
                                            FileTransferReply(
                                                it.filePath,
                                                isPresent = true,
                                                it.artifactType
                                            )
                                        }
                                        false -> {
                                            debug("file ${it.filePath} is not available in cache")
                                            FileTransferReply(
                                                it.filePath,
                                                isPresent = false,
                                                it.artifactType
                                            )
                                        }
                                    }
                                    send(compileResponse)
                                }
                            }
                        }
                        is FileChunk -> {
                            compilationMetadata?.let { metadata ->
                                fileChunks.getOrPut(it.filePath) { mutableListOf() }.add(it)
                                if (it.isLast) {
                                    launch {
                                        val allFileChunks = fileChunks.getOrDefault(it.filePath, listOf())
                                        val reconstructedFile = fileChunkingStrategy.reconstruct(allFileChunks, SERVER_TMP_CACHE_DIR)
                                        val cachedFile =
                                            cacheHandler.cacheFile(reconstructedFile, it.artifactType, deleteOriginalFile = true)
                                        val projectFile = workspaceManager.copyFileToProject(
                                            cachedFile.absolutePath,
                                            it.filePath,
                                            userId,
                                            compilationMetadata.projectName
                                        )
                                        debug("Reconstructed ${if (reconstructedFile.isFile) "file" else "directory"}, artifactType=${it.artifactType}, clientPath=${it.filePath}")
                                        fileAvailable(Paths.get(it.filePath).toAbsolutePath().normalize(), projectFile, it.artifactType)
                                    }
                                }
                            }
                        }
                    }
                }
            }


            // here we collect source files, if all source files are available, we are ready to compile
            launch {
                allFilesReady.join()

                val remoteCompilerArguments = CompilerUtils.replacePathsWithRemoteOnes(
                    userId,
                    compilationMetadata!!,
                    workspaceManager,
                    sourceFiles,
                    dependencyFiles,
                    compilerPluginFiles
                )

                val (isCompilationResultCached, inputFingerprint) = cacheHandler.isCompilationResultCached(remoteCompilerArguments)
                val (outputDir, compilationResult) = if (isCompilationResultCached) {
                    debug("[SERVER COMPILATION] Compilation result is cached, fingerprint: $inputFingerprint")
                    cacheHandler.getCompilationResultDirectory(inputFingerprint) to CompilationResult(
                        0,
                        CompilationResultSource.CACHE
                    )
                } else {
                    debug("[SERVER COMPILATION] Compilation result is NOT cached, fingerprint: $inputFingerprint")
                    val (outputDir, compilationResult) = doCompilation(
                        remoteCompilerArguments,
                        compilationMetadata,
                        this@channelFlow::trySend// TODO: double check trySend
                    )
                    cacheHandler.cacheFile(outputDir, ArtifactType.RESULT, deleteOriginalFile = false, remoteCompilerArguments)
                    outputDir to compilationResult
                }
                send(compilationResult)

                outputDir.walkTopDown()
                    .filter { !Files.isDirectory(it.toPath()) }
                    .forEach { file ->
                        val clientCleanedPath = cleanCompilationResultPath(file.path)
                        fileChunkStrategy.chunk(file, isDirectory = false, ArtifactType.RESULT).collect { chunk ->
                            send(
                                FileChunk(
                                    clientCleanedPath,
                                    ArtifactType.RESULT,
                                    chunk.content,
                                    chunk.isDirectory,
                                    chunk.isLast,
                                )
                            )
                        }
                    }
                this@channelFlow.close()
            }
        }
    }

    private fun doCompilation(
        remoteCompilerArguments: K2JVMCompilerArguments,
        compilationMetadata: CompilationMetadata,
        send: (CompileResponse) -> Unit
    ): Pair<File, CompilationResult> {
        val remoteMessageCollector = RemoteMessageCollector(object : OnReport {
            override fun onReport(msg: CompilerMessage) {
                send(msg)
            }
        })

        val outputsCollector = { x: File, y: List<File> -> println("$x $y") }
        val servicesFacade =
            BasicCompilerServicesWithResultsFacadeServer(remoteMessageCollector, outputsCollector)

        debug("[SERVER COMPILATION] remote compiler arguments are:")
        debug(ArgumentUtils.convertArgumentsToStringList(remoteCompilerArguments).toString())
        val exitCode = compilerService.compileImpl(
            compilerArguments = ArgumentUtils.convertArgumentsToStringList(remoteCompilerArguments)
                .toTypedArray(),
            compilationOptions = compilationMetadata.compilationOptions,
            servicesFacade = servicesFacade,
            compilationResults = null,
            hasIncrementalCaches = JpsCompilerServicesFacade::hasIncrementalCaches,
            createMessageCollector = { facade, options ->
                remoteMessageCollector
            },
            createReporter = ::DaemonMessageReporter,
            createServices = { facade, eventManager ->
                Services.EMPTY
            },
            getICReporter = { a, b, c ->
                getBuildReporter(a, b!!, c)
            }
        )
        debug("[SERVER COMPILATION] compilation finished and exit code is $exitCode")
        val outputDir = CompilerUtils.getOutputDir(remoteCompilerArguments)
        return outputDir to CompilationResult(exitCode, CompilationResultSource.COMPILER)
    }
}