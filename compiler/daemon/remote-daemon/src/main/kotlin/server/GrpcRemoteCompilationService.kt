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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import model.toDomain
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.JpsCompilerServicesFacade
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporter
import org.jetbrains.kotlin.daemon.report.getBuildReporter
import org.jetbrains.kotlin.server.CompilationMetadataGrpc
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


    val workspaceManager = WorkspaceManager()

    fun debug(text: String) {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        println("[${LocalDateTime.now().format(formatter)}] [thread=${Thread.currentThread().name}] DEBUG SERVER: $text")
    }

    override fun compile(requests: Flow<CompileRequestGrpc>): Flow<CompileResponseGrpc> {
        return channelFlow {
            val sourceFilesChannel = Channel<String>(capacity = Channel.UNLIMITED)
            val sourceFiles = mutableListOf<String>()

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
                                            val projectFilePath = workspaceManager.copyFileToProject(cachedFile.absolutePath, clientFilePath,userId, compilationMetadata.projectName)
                                            println("project file path = $projectFilePath")
                                            sourceFilesChannel.send(projectFilePath?.toAbsolutePath().toString())
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
                            compilationMetadata?.let{
                                val chunk = request.sourceFileChunk.content.toByteArray()
                                val filePath = request.sourceFileChunk.filePath
                                fileChunkStrategy.addChunks(filePath, chunk)
                                if (request.sourceFileChunk.isLast) {
                                    launch {
                                        val allFileChunks = fileChunkStrategy.getChunks(filePath)
                                        val (fingerprint, cachedFile) = cacheHandler.addSourceFile(allFileChunks)
                                        val projectFilePath = workspaceManager.copyFileToProject(cachedFile.absolutePath, filePath,userId, compilationMetadata.projectName)
                                        println("project file path = $projectFilePath")
                                        sourceFilesChannel.send(projectFilePath?.toAbsolutePath().toString())
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
                            val compilerArguments =
                                sourceFiles.toTypedArray() + "-d" + buildAbsPath(OUTPUT_FILES_DIR) + "-cp" + "/Users/michal.svec/Desktop/jars/kotlin-stdlib-2.2.0.jar" + compilationMetadata.compilerArgumentsList.toTypedArray()
                            println("DEBUG SERVER: compilerArguments=${compilerArguments.contentToString()}")

                            val remoteMessageCollector = RemoteMessageCollector(object: OnReport{
                                override fun onReport(msg: MessageCollectorImpl.Message) {
                                    println("ON REPORT")
                                }
                            })

                            val outputsCollector = { x: File, y: List<File> -> println("$x $y") }
                            val servicesFacade = BasicCompilerServicesWithResultsFacadeServer(remoteMessageCollector, outputsCollector)

                            debug("compilation started")
                            try {
                                val result = compilationService.compileImpl(
                                    compilerArguments = compilerArguments,
                                    compilationOptions = compilationMetadata.compilationOptions.toDomain(),
                                    servicesFacade = servicesFacade,
                                    compilationResults = null,
                                    hasIncrementalCaches = JpsCompilerServicesFacade::hasIncrementalCaches,
                                    createMessageCollector = { facade, options ->
                                        RemoteMessageCollector(object : OnReport {
                                            override fun onReport(msg: MessageCollectorImpl.Message) {
                                                println("this is our compilation message $msg")
                                            }
                                        })
                                    },
                                    createReporter = ::DaemonMessageReporter,
                                    createServices = { facade, eventManager ->
                                        Services.EMPTY
                                    },
                                    getICReporter = { a, b, c -> getBuildReporter(a, b!!, c) }
                                )
                                debug("compilation finished and exist code is $result")
                                responseHandler.buildFileChunkStream(File(buildAbsPath(OUTPUT_FILES_DIR))).collect { fileChunk->
                                    send(fileChunk)
                                }
                            } catch (e: Exception) {
                                println("error occurred: ${e.message}")
                                e.printStackTrace()
                                // TODO handle case when daemon is no longer alive
                            }
                        }
                    }
                }
            }
        }
    }

}