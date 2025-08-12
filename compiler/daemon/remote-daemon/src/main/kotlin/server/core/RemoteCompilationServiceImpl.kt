/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.core

import common.OneFileOneChunkStrategy
import common.RemoteCompilationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.JpsCompilerServicesFacade
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporter
import org.jetbrains.kotlin.daemon.report.getBuildReporter
import server.interceptors.AuthInterceptor
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RemoteCompilationServiceImpl(
    val cacheHandler: CacheHandler,
    val compilerService: InProcessCompilerService,
    val workspaceManager: WorkspaceManager,
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
            val sourceFilesChannel = Channel<File>(capacity = Channel.UNLIMITED)
            val sourceFiles = mutableListOf<File>()

            val fileChunkStrategy = OneFileOneChunkStrategy()
            var compilationMetadata: CompilationMetadata? = null

            val userId = AuthInterceptor.USER_ID_CONTEXT_KEY.get()

            // here we consume request stream
            launch {
                compileRequests.collect {
                    when (it) {
                        is CompilationMetadata -> {
                            compilationMetadata = it
                        }
                        is FileTransferRequest -> {
                            compilationMetadata?.let { metadata ->
                                launch {
                                    val compileResponse = when (cacheHandler.isFileCached(it.filePath, it.fileFingerprint)) {
                                        true -> {
                                            debug("file ${it.filePath} is available in cache")
                                            val cachedFile = cacheHandler.getFile(it.filePath, it.fileFingerprint)
                                            val projectFilePath = workspaceManager.copyFileToProject(
                                                cachedFilePath = cachedFile.absolutePath,
                                                clientFilePath = it.filePath,
                                                userId,
                                                compilationMetadata.projectName
                                            )
                                            println("project file path = $projectFilePath")
                                            sourceFilesChannel.send(projectFilePath.toFile())
                                            FileTransferReply(
                                                it.filePath,
                                                isPresent = true
                                            )
                                        }
                                        false -> {
                                            debug("file ${it.filePath} is not available in cache")
                                            FileTransferReply(
                                                it.filePath,
                                                isPresent = false
                                            )
                                        }
                                    }
                                    send(compileResponse)
                                }
                            }
                        }
                        is FileChunk -> {
                            compilationMetadata?.let { metadata ->
                                fileChunkStrategy.addChunks(it.filePath, it.content)
                                if (it.isLast) {
                                    launch {
                                        val allFileChunks = fileChunkStrategy.getChunks(it.filePath)
                                        val (fingerprint, cachedFile) = cacheHandler.cacheSourceFile(allFileChunks)
                                        val projectFilePath = workspaceManager.copyFileToProject(
                                            cachedFile.absolutePath,
                                            it.filePath,
                                            userId,
                                            compilationMetadata.projectName
                                        )
                                        println("project file path = $projectFilePath")
                                        sourceFilesChannel.send(projectFilePath.toFile())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // here we collect source files, if all source files are available, we are ready to compile
            launch {
                sourceFilesChannel.receiveAsFlow().collect { filePath ->
                    println("new message in source file channel: $filePath")
                    sourceFiles.add(filePath)
                    if (sourceFiles.size == compilationMetadata?.fileCount) {
                        launch(Dispatchers.Default) {
                            val outputDirectory = workspaceManager.getOutputDir(userId, compilationMetadata.projectName)
                            val compilerArguments = InProcessCompilerService.buildCompilerArgsWithoutSourceFiles(
                                outputDirectory,
                                compilationMetadata.compilerArguments
                            )

                            // TODO: I made up compiler version
                            val (isCompilationResultCached, inputFingerprint) = cacheHandler.isCompilationResultCached(
                                sourceFiles,
                                compilerArguments,
                                compilerVersion = "2.0"
                            )
                            if (isCompilationResultCached) {
                                val compilationResultDirectory = cacheHandler.getCompilationResultDirectory(inputFingerprint)
                                send(CompilationResult(exitCode = 0, CompilationResultSource.CACHE))
                                compilationResultDirectory.walkTopDown()
                                    .filter { it.isFile }
                                    .forEach { file ->
                                        fileChunkStrategy.chunk(file).collect { chunk ->
                                            send(
                                                FileChunk(
                                                    chunk.filePath,
                                                    chunk.content,
                                                    chunk.isLast,
                                                )
                                            )
                                        }
                                    }
                                close()
                            } else {
                                val remoteMessageCollector = RemoteMessageCollector(object : OnReport {
                                    override fun onReport(msg: CompilerMessage) {
                                        trySend(msg)// TODO double check trySend
                                    }
                                })

                                val outputsCollector = { x: File, y: List<File> -> println("$x $y") }
                                val servicesFacade =
                                    BasicCompilerServicesWithResultsFacadeServer(remoteMessageCollector, outputsCollector)

                                debug("compilation started")
                                val exitCode = compilerService.compileImpl(
                                    compilerArguments = (sourceFiles.map { it.path } + compilerArguments).toTypedArray(),
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
                                debug("compilation finished and exist code is $exitCode")

                                send(CompilationResult(exitCode, CompilationResultSource.COMPILER))

                                if (exitCode == 0) {
                                    val cachedCompilationResult = cacheHandler.addCompilationResult(
                                        sourceFiles,
                                        outputDirectory.toFile(),
                                        compilerArguments,
                                        "2.0" // TODO: I just made up this
                                    )

                                    cachedCompilationResult.file.walkTopDown()
                                        .filter { it.isFile }
                                        .forEach { file ->
                                            fileChunkStrategy.chunk(file).collect { chunk ->
                                                send(
                                                    FileChunk(
                                                        chunk.filePath,
                                                        chunk.content,
                                                        chunk.isLast,
                                                    )
                                                )
                                            }
                                        }
                                }
                                close()
                            }
                        }
                    }
                }
            }
        }
    }
}