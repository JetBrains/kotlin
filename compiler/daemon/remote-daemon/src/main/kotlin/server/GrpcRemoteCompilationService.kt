/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server

import common.OUTPUT_FILES_DIR
import common.OneFileOneChunkStrategy
import common.SERVER_COMPILATION_WORKSPACE_DIR
import common.SERVER_SOURCE_FILES_CACHE_DIR
import common.buildAbsPath
import common.computeSha256
import common.toCompileResponseGrpc
import common.toGrpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import model.CompilationResult
import model.CompilationResultSource
import model.toCompileResponse
import model.toDomain
import model.toGrpc
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.JpsCompilerServicesFacade
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporter
import org.jetbrains.kotlin.daemon.report.getBuildReporter
import org.jetbrains.kotlin.server.CompilationMetadataGrpc
import org.jetbrains.kotlin.server.CompilationResultGrpc
import org.jetbrains.kotlin.server.CompileRequestGrpc
import org.jetbrains.kotlin.server.CompileResponseGrpc
import org.jetbrains.kotlin.server.CompileServiceGrpcKt
import server.interceptors.AuthInterceptor
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GrpcRemoteCompilationService(
    private val cacheHandler: CacheHandler,
    private val compilationService: InProcessCompilationService
) : CompileServiceGrpcKt.CompileServiceCoroutineImplBase() {

    private val workspaceManager = WorkspaceManager()

    fun debug(text: String) {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        println("[${LocalDateTime.now().format(formatter)}] [thread=${Thread.currentThread().name}] DEBUG SERVER: $text")
    }

    override fun compile(requests: Flow<CompileRequestGrpc>): Flow<CompileResponseGrpc> {
        return channelFlow {
            val sourceFilesChannel = Channel<File>(capacity = Channel.UNLIMITED)
            val sourceFiles = mutableListOf<File>()

            val fileChunkStrategy = OneFileOneChunkStrategy()
            val responseHandler = ResponseHandler(fileChunkStrategy)
            var compilationMetadata: CompilationMetadataGrpc? = null

            val userId = AuthInterceptor.USER_ID_CONTEXT_KEY.get()

            // here we consume request stream
            launch {
                requests.collect { request ->
                    when {
                        request.hasMetadata() -> {
                            compilationMetadata = request.metadata
                        }
                        request.hasFileTransferRequest() -> {
                            compilationMetadata?.let {
                                launch {
                                    val fileFingerprint = request.fileTransferRequest.fileFingerprint
                                    val clientFilePath = request.fileTransferRequest.filePath
                                    val cachedFile = cacheHandler.getSourceFile(fileFingerprint)
                                    debug("client file path = $clientFilePath")
                                    debug("cached file path = ${cachedFile?.path}")
                                    val compileResponse = when (cachedFile) {
                                        is File -> {
                                            debug("file $clientFilePath is available in cache")
                                            val projectFilePath = workspaceManager.copyFileToProject(
                                                cachedFile.absolutePath,
                                                clientFilePath,
                                                userId,
                                                compilationMetadata.projectName
                                            )
                                            println("project file path = $projectFilePath")
                                            sourceFilesChannel.send(projectFilePath.toFile())
                                            responseHandler.buildFileTransferReply(clientFilePath, true)
                                        }
                                        else -> {
                                            debug("file $clientFilePath is not available in cache")
                                            responseHandler.buildFileTransferReply(clientFilePath, false)
                                        }
                                    }
                                    send(compileResponse)
                                }
                            }
                        }
                        request.hasSourceFileChunk() -> {
                            compilationMetadata?.let {
                                val chunk = request.sourceFileChunk.content.toByteArray()
                                val filePath = request.sourceFileChunk.filePath
                                fileChunkStrategy.addChunks(filePath, chunk)
                                if (request.sourceFileChunk.isLast) {
                                    launch {
                                        val allFileChunks = fileChunkStrategy.getChunks(filePath)
                                        val (fingerprint, cachedFile) = cacheHandler.addSourceFile(allFileChunks)
                                        val projectFilePath = workspaceManager.copyFileToProject(
                                            cachedFile.absolutePath,
                                            filePath,
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
                    if (sourceFiles.size == compilationMetadata?.fileCount){
                        launch(Dispatchers.Default) {
                            val outputDirectory = workspaceManager.getOutputDir(userId, compilationMetadata.projectName)
                            val compilerArguments = InProcessCompilationService.buildCompilerArgsWithoutSourceFiles(
                                outputDirectory,
                                compilationMetadata.compilerArgumentsList
                            )

                            // TODO: I made up compiler version
                            val (isCompilationResultCached, inputFingerprint) = cacheHandler.isCompilationResultCached(
                                sourceFiles,
                                compilerArguments,
                                compilerVersion = "2.0"
                            )
                            if (isCompilationResultCached) {
                                val compilationResultDirectory = cacheHandler.getCompilationResultDirectory(inputFingerprint)
                                send(CompilationResult(exitCode = 0, CompilationResultSource.CACHE).toGrpc().toCompileResponse())
                                responseHandler.buildFileChunkStream(compilationResultDirectory).collect { fileChunk ->
                                    send(fileChunk)
                                }
                                close()
                            } else {
                                val remoteMessageCollector = RemoteMessageCollector(object : OnReport {
                                    override fun onReport(msg: MessageCollectorImpl.Message) {
                                        trySend(msg.toGrpc().toCompileResponseGrpc())// TODO double check trySend
                                    }
                                })

                                val outputsCollector = { x: File, y: List<File> -> println("$x $y") }
                                val servicesFacade = BasicCompilerServicesWithResultsFacadeServer(remoteMessageCollector, outputsCollector)

                                debug("compilation started")
                                val exitCode = compilationService.compileImpl(
                                    compilerArguments = (sourceFiles.map { it.path } + compilerArguments).toTypedArray(),
                                    compilationOptions = compilationMetadata.compilationOptions.toDomain(),
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

                                send(CompilationResult(exitCode, CompilationResultSource.COMPILER).toGrpc().toCompileResponse())

                                if (exitCode == 0) {
                                    val cachedCompilationResult = cacheHandler.addCompilationResult(
                                        sourceFiles,
                                        outputDirectory.toFile(),
                                        compilerArguments,
                                        "2.0" // TODO I just made up this
                                    )
                                    responseHandler.buildFileChunkStream(cachedCompilationResult.file).collect { fileChunk ->
                                        send(fileChunk)
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