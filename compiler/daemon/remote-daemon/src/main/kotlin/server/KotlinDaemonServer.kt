/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package main.kotlin.server

import common.CACHED_FILES_DIR
import server.MessageSender
import common.OUTPUT_FILES_DIR
import common.OneFileOneChunkStrategy
import server.RemoteMessageCollector
import common.buildAbsPath
import common.toConnectResponseGrpc
import common.toDomain
import common.toGrpc
import common.computeSha256
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.configureDaemonJVMOptions
import org.jetbrains.kotlin.server.CompilationMetadataGrpc
import org.jetbrains.kotlin.server.CompileRequestGrpc
import org.jetbrains.kotlin.server.CompileResponseGrpc
import org.jetbrains.kotlin.server.CompileServiceGrpcKt
import org.jetbrains.kotlin.server.ConnectRequestGrpc
import org.jetbrains.kotlin.server.ConnectResponseGrpc
import server.CacheHandler
import server.KotlinDaemonManager
import server.RemoteKotlinDaemonInterceptor
import server.ResponseHandler
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class RemoteKotlinDaemonServer(private val port: Int) {

    val cacheHandler = CacheHandler()

    val server: Server =
        ServerBuilder
            .forPort(port)
            .addService(ServerInterceptors.intercept(RemoteKotlinDaemonService(), RemoteKotlinDaemonInterceptor()))
            .build()

    fun debug(text: String){
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        println("[${LocalDateTime.now().format(formatter)}] [thread=${Thread.currentThread().name}] DEBUG SERVER: $text")
    }

    fun start() {
        cacheHandler.loadCache()
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@RemoteKotlinDaemonServer.stop()
                println("*** server shut down")
            },
        )
    }

    private fun stop() {
        server.shutdown()
        cacheHandler.dumpCache()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    inner class RemoteKotlinDaemonService : CompileServiceGrpcKt.CompileServiceCoroutineImplBase() {

        private val sessions = mutableMapOf<Int, Pair<CompileService, RemoteMessageCollector>>()

        override fun connect(request: ConnectRequestGrpc): Flow<ConnectResponseGrpc> {
            return channelFlow {
                val remoteMessageCollector = RemoteMessageCollector(object : MessageSender {
                    override fun send(msg: MessageCollectorImpl.Message) {
                        trySend(msg.toGrpc().toConnectResponseGrpc()) // TODO double check trySend, some messages can get lost
                    }
                })

                val daemonJVMOptions = configureDaemonJVMOptions(
                    inheritOtherJvmOptions = request.daemonJvmOptionsConfigurator.inheritOtherJvmOptions,
                    inheritMemoryLimits = request.daemonJvmOptionsConfigurator.inheritMemoryLimits,
                    inheritAdditionalProperties = request.daemonJvmOptionsConfigurator.inheritAdditionalProperties,
                )

                val (daemon, sessionId) = KotlinDaemonManager.getDaemon(remoteMessageCollector, daemonJVMOptions)
                sessions[sessionId] = daemon to remoteMessageCollector
                send(ConnectResponseGrpc.newBuilder().setSessionId(sessionId).build())
                close() // TODO do we really need to close this channel?
            }
        }


        override fun compile(requests: Flow<CompileRequestGrpc>): Flow<CompileResponseGrpc> {
            return channelFlow {
                val sourceFilesChannel = Channel<String>(capacity = Channel.UNLIMITED)
                val sourceFiles = mutableListOf<String>()

                val fileChunkStrategy = OneFileOneChunkStrategy()
                val responseHandler = ResponseHandler(fileChunkStrategy)
                var compilationMetadata: CompilationMetadataGrpc? = null

                // here we consume request stream
                launch {
                    requests.collect { request ->
                        when {
                            request.hasMetadata() -> {
                                compilationMetadata = request.metadata
                            }
                            request.hasFileTransferRequest() -> {
                                launch {
                                    val fileFingerprint= request.fileTransferRequest.fileFingerprint
                                    val filePath = request.fileTransferRequest.filePath
                                    val cachedFilePath = cacheHandler.getFilePath(fileFingerprint)
                                    val compileResponse = when (cachedFilePath) {
                                        is String -> {
                                            debug("file $filePath is available in cache")
                                            sourceFilesChannel.send(cachedFilePath)
                                            responseHandler.buildFileTransferReply(filePath, true)
                                        }
                                        else -> {
                                            debug("file $filePath is not available in cache")
                                            responseHandler.buildFileTransferReply(filePath, false)
                                        }
                                    }
                                    send(compileResponse)
                                }
                            }
                            request.hasSourceFileChunk() -> {
                                val chunk = request.sourceFileChunk.content.toByteArray()
                                val filePath = request.sourceFileChunk.filePath
                                fileChunkStrategy.addChunks(filePath, chunk)
                                if (request.sourceFileChunk.isLast) {
                                    launch {
                                        val file = fileChunkStrategy.reconstruct(
                                            filePath,
                                            buildAbsPath("$CACHED_FILES_DIR/${filePath.split("/").last()}")
                                        )
                                        // TODO probably not the best solution, we are computing the hash again and we do not use hash that client sent us
                                        cacheHandler.addFile(computeSha256(file),file.path)
                                        sourceFilesChannel.send(file.path)
                                    }
                                }
                            }
                        }
                    }
                }

                // here we collect source files, if all source files are available, we are ready to compile
                launch {
                    sourceFilesChannel.receiveAsFlow().collect { filePath ->
                        sourceFiles.add(filePath)
                        if (sourceFiles.size == compilationMetadata?.fileCount){
                            launch(Dispatchers.Default) {
                                val compilerArguments =
                                    sourceFiles.toTypedArray() + "-d" + buildAbsPath(OUTPUT_FILES_DIR) + "-cp" + "/Users/michal.svec/Desktop/jars/kotlin-stdlib-2.2.0.jar" + compilationMetadata.compilerArgumentsList.toTypedArray()
                                println("DEBUG SERVER: compilerArguments=${compilerArguments.contentToString()}")

                                val (daemon, remoteMessageCollector) = sessions[compilationMetadata.sessionId]
                                    ?: error("No session found with id ${compilationMetadata.sessionId}")

                                val outputsCollector = { x: File, y: List<File> -> println("$x $y") }
                                val servicesFacade = BasicCompilerServicesWithResultsFacadeServer(remoteMessageCollector, outputsCollector)

                                debug("compilation started")
                                try {
                                    val result = daemon.compile(
                                        sessionId = compilationMetadata.sessionId,
                                        compilerArguments = compilerArguments,
                                        compilationOptions = compilationMetadata.compilationOptions.toDomain(),
                                        servicesFacade = servicesFacade,
                                        compilationResults = null
                                    )
                                    debug("compilation finished")
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

        private fun connectToDaemon(){

        }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 50051
    val server = RemoteKotlinDaemonServer(port)
    server.start()
    server.blockUntilShutdown()
}