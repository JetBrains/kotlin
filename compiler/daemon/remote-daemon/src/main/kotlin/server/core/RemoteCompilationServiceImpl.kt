/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.core

import common.CompilerUtils
import common.FileChunkingStrategy
import common.OneFileOneChunkStrategy
import common.RemoteCompilationService
import common.SERVER_ARTIFACTS_CACHE_DIR
import common.computeSha256
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
import model.DirectoryTransferRequest
import model.FileChunk
import model.FileTransferReply
import model.FileTransferRequest
import model.ArtifactType
import model.DirectoryEntryChunk
import model.DirectoryTransferReply
import model.FileIdentifier
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.JpsCompilerServicesFacade
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporter
import org.jetbrains.kotlin.daemon.report.getBuildReporter
import server.interceptors.AuthInterceptor
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

enum class CompilationMode {
    REAL,
    FAKE
}

class RemoteCompilationServiceImpl(
    val cacheHandler: CacheHandler,
    val workspaceManager: WorkspaceManager,
    val fileChunkingStrategy: FileChunkingStrategy,
    val compilerService: InProcessCompilerService,
    val compilationMode: CompilationMode = CompilationMode.REAL,
) : RemoteCompilationService {

    fun debug(text: String) {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        println("[${LocalDateTime.now().format(formatter)}] [thread=${Thread.currentThread().name}] DEBUG SERVER: $text")
    }

    override fun cleanup() {
        workspaceManager.cleanup()
        cacheHandler.cleanup()
    }

    override fun compile(compileRequests: Flow<CompileRequest>): Flow<CompileResponse> {
        return channelFlow {

            val allFilesReady = CompletableDeferred<Unit>()
            val filesReceivedCounter = AtomicInteger(0)
            var totalFilesExpected = -1

            // <ClientPath, FileFromUserWorkspace>
            val sourceFiles = mutableMapOf<String, File>()
            val dependencyFiles = mutableMapOf<String, File>()
            val compilerPluginFiles = mutableMapOf<String, File>()

            val fileChunks = mutableMapOf<String, MutableList<FileChunk>>()

            fun fileAvailable(clientPath: String, file: File, artifactType: ArtifactType) {
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

            val fileChunkStrategy = OneFileOneChunkStrategy()
            var compilationMetadata: CompilationMetadata? = null

            val userId = AuthInterceptor.USER_ID_CONTEXT_KEY.get()

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
                                            val projectFilePath = workspaceManager.copyFileToProject(
                                                cachedFilePath = cachedFile.absolutePath,
                                                clientFilePath = it.filePath,
                                                userId,
                                                compilationMetadata.projectName
                                            )
                                            fileAvailable(it.filePath, projectFilePath.toFile(), it.artifactType)
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
                        is DirectoryTransferRequest -> {
                            if (cacheHandler.isFileCached(it.directoryFingerprint)) {
                                debug("Directory ${it.directoryPath} is available in cache")
                                val directory = cacheHandler.getFile(it.directoryFingerprint)
                                fileAvailable(it.directoryPath, directory, it.artifactType)
                                // TODO if it is dependency we do not need to copy the files and change their location
                            } else {
                                debug("Directory ${it.directoryPath} is not available in cache")
                                val missingFiles = mutableListOf<FileIdentifier>()
                                it.directoryFiles.forEach { directoryFile ->
                                    if (cacheHandler.isFileCached(directoryFile.fileFingerprint)) {
                                        debug("Directory file ${directoryFile.filePath} is available in cache")
                                    } else {
                                        missingFiles.add(
                                            FileIdentifier(
                                                directoryFile.filePath,
                                                directoryFile.fileFingerprint,
                                            )
                                        )
                                    }
                                }
                                send(
                                    DirectoryTransferReply(
                                        it.directoryPath,
                                        it.directoryFingerprint,
                                        false,
                                        missingFiles,
                                    )
                                )
                            }
                        }
                        is FileChunk -> {
                            compilationMetadata?.let { metadata ->
                                fileChunks.getOrPut(it.filePath) { mutableListOf() }.add(it)
                                if (it.isLast) {
                                    launch {
                                        val allFileChunks = fileChunks.getOrDefault(it.filePath,listOf())
                                        val fileHash = computeSha256(allFileChunks)
                                        val file = fileChunkingStrategy.reconstruct(allFileChunks, "$SERVER_ARTIFACTS_CACHE_DIR/$fileHash")
                                        val (fingerprint, cachedFile) = cacheHandler.cacheFile(file, it.artifactType)
                                        val projectFilePath = workspaceManager.copyFileToProject(
                                            cachedFile.absolutePath,
                                            it.filePath,
                                            userId,
                                            compilationMetadata.projectName
                                        )
                                        debug("Reconstructed file, fileType=${it.artifactType}, clientPath=${it.filePath}")
                                        fileAvailable(it.filePath, projectFilePath.toFile(), it.artifactType)
                                    }
                                }
                            }
                        }
                        is DirectoryEntryChunk -> {
                            val directoryEntryChunk = it.fileChunk
                            fileChunks.getOrPut(directoryEntryChunk.filePath) { mutableListOf() }.add(directoryEntryChunk)
                            if (directoryEntryChunk.isLast) {
                                val allFileChunks = fileChunks.getOrDefault(directoryEntryChunk.filePath,listOf())
                                val directoryEntryRelativePath =
                                    Paths.get(directoryEntryChunk.filePath).relativeTo(Paths.get(it.directoryPath)).pathString
                                val reconstructedFile = fileChunkingStrategy.reconstruct(
                                    allFileChunks,
                                    "$SERVER_ARTIFACTS_CACHE_DIR/${computeSha256(it.directoryPath)}/$directoryEntryRelativePath"
                                )
                                val (directoryEntryFingerprint, cachedFile) = cacheHandler.cacheFile(
                                    reconstructedFile,
                                    directoryEntryChunk.artifactType
                                )
                                if (directoryEntryChunk.artifactType == ArtifactType.RESULT) {

                                }
                            }
                        }
                    }
                }
            }


            // here we collect source files, if all source files are available, we are ready to compile
            launch {
                allFilesReady.join()

                val remoteCompilerArguments = CompilerUtils.replaceClientPathsWithRemotePaths(
                    userId,
                    compilationMetadata!!, // TODO exclamation marks
                    workspaceManager,
                    dependencyFiles,
                    sourceFiles,
                    compilerPluginFiles
                )
                when (compilationMode) {
                    CompilationMode.REAL -> {
                        doCompilation(userId, remoteCompilerArguments, compilationMetadata, fileChunkStrategy).collect { send(it) }
                    }
                    CompilationMode.FAKE -> {
                        doFakeCompilation(remoteCompilerArguments, fileChunkStrategy).collect { send(it) }
                    }
                }
                println("server closed ")
                this@channelFlow.close()
            }
        }
    }

    private fun reconstructCacheAndCopyFile(fileChunks: Collection<FileChunk>, newFilePath: String){

    }


    private fun doFakeCompilation(
        remoteCompilerArguments: Map<String, String>,
        fileChunkStrategy: FileChunkingStrategy
    ): Flow<CompileResponse> {
        return channelFlow {
            send(CompilationResult(0, CompilationResultSource.COMPILER))

            val outputDir = CompilerUtils.getOutputDir(remoteCompilerArguments).absolutePath
            File(outputDir).walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    fileChunkStrategy.chunk(file, ArtifactType.RESULT).collect { chunk ->
                        send(
                            FileChunk(
                                chunk.filePath,
                                ArtifactType.RESULT,
                                chunk.content,
                                chunk.isLast,
                            )
                        )
                    }
                }
        }
    }

    private fun doCompilation(
        userId: String,
        remoteCompilerArguments: Map<String, String>,
        compilationMetadata: CompilationMetadata,
        fileChunkStrategy: FileChunkingStrategy,
    ): Flow<CompileResponse> {
        return channelFlow {
            val (isCompilationResultCached, inputFingerprint) = cacheHandler.isCompilationResultCached(remoteCompilerArguments)

            if (isCompilationResultCached) {
                debug("[SERVER COMPILATION] Compilation result is cached, fingerprint: $inputFingerprint")
                val compilationResultDirectory = cacheHandler.getCompilationResultDirectory(inputFingerprint)
                send(CompilationResult(exitCode = 0, CompilationResultSource.CACHE))
                compilationResultDirectory.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        fileChunkStrategy.chunk(file, ArtifactType.RESULT).collect { chunk ->
                            send(
                                FileChunk(
                                    chunk.filePath,
                                    ArtifactType.RESULT,
                                    chunk.content,
                                    chunk.isLast,
                                )
                            )
                        }
                    }
            } else {
                val remoteMessageCollector = RemoteMessageCollector(object : OnReport {
                    override fun onReport(msg: CompilerMessage) {
                        trySend(msg)// TODO double check trySend
                    }
                })

                val outputsCollector = { x: File, y: List<File> -> println("$x $y") }
                val servicesFacade =
                    BasicCompilerServicesWithResultsFacadeServer(remoteMessageCollector, outputsCollector)

                debug("[SERVER COMPILATION] compilation started")
                debug("[SERVER COMPILATION] remote compiler arguments are:")
//                for ((key, value) in remoteCompilerArguments) {
//                    debug("[SERVER COMPILATION] $key $value")
//                }
                println(CompilerUtils.getCompilerArgumentsList(remoteCompilerArguments))
                val exitCode = compilerService.compileImpl(
                    compilerArguments = CompilerUtils.getCompilerArgumentsList(remoteCompilerArguments)
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

                send(CompilationResult(exitCode, CompilationResultSource.COMPILER))

                if (exitCode == 0) {
                    val cachedCompilationResult = cacheHandler.cacheFile(
                        File(remoteCompilerArguments["-d"]),
                        ArtifactType.RESULT,
                        remoteCompilerArguments
                    )

                    cachedCompilationResult.file.walkTopDown()
                        .filter { it.isFile }
                        .forEach { file ->
                            fileChunkStrategy.chunk(file, ArtifactType.RESULT).collect { chunk ->
                                send(
                                    FileChunk(
                                        chunk.filePath,
                                        ArtifactType.RESULT,
                                        chunk.content,
                                        chunk.isLast,
                                    )
                                )
                            }
                        }
                }
            }
            close()
        }
    }
}