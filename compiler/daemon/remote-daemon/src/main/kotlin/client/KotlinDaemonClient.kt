/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.client

import client.auth.BasicHTTPAuthClient
import client.auth.CallAuthenticator
import client.RemoteClientInterceptor
import client.RequestHandler
import common.CLIENT_COMPILED_DIR
import common.OneFileOneChunkStrategy
import common.buildAbsPath
import common.toGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import model.DaemonJVMOptionsConfigurator
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.server.*
import java.io.Closeable
import java.io.File
import java.util.concurrent.TimeUnit

class RemoteDaemonClient(
    private val channel: ManagedChannel,
) : Closeable {
    private val stub: CompileServiceGrpcKt.CompileServiceCoroutineStub = CompileServiceGrpcKt
        .CompileServiceCoroutineStub(channel)
        .withCallCredentials(
            CallAuthenticator(
                BasicHTTPAuthClient(
                    username = "dummy",
                    password = "dummy"
                )
            )
        )

    init {
        File(CLIENT_COMPILED_DIR).mkdir()
    }

    fun connect(daemonJVMOptionsConfigurator: DaemonJVMOptionsConfigurator): Flow<ConnectResponseGrpc> {
        val connectRequest = ConnectRequestGrpc
            .newBuilder()
            .setDaemonJvmOptionsConfigurator(daemonJVMOptionsConfigurator.toGrpc())
            .build()
        return stub.connect(connectRequest)
    }

    suspend fun compile(sessionId: Int, compilerArguments: Array<out String>, compilationOptions: CompilationOptions, sourceFiles: List<File>) {
        val requestChannel = Channel<CompileRequestGrpc>(capacity = Channel.UNLIMITED)
        val responseChannel = Channel<CompileResponseGrpc>(capacity = Channel.UNLIMITED)

        val fileChunkStrategy = OneFileOneChunkStrategy()
        val requestHandler = RequestHandler(
            fileChunkingStrategy = fileChunkStrategy
        )

        coroutineScope {
            // start consuming response
            launch(Dispatchers.IO) {
                stub.compile(requestChannel.receiveAsFlow()).collect { responseChannel.send(it) }
            }

            // start consuming our request channel
            launch {
                responseChannel.consumeAsFlow().collect {
                    when {
                        it.hasFileTransferReply() -> {
                            if (!it.fileTransferReply.isPresent){
                                val filePath = it.fileTransferReply.filePath
                                launch {
                                    // a file is chunked based on a selected strategy and streamed to server
                                    requestHandler.buildFileChunkStream(filePath).collect { fileChunk ->
                                        requestChannel.send(fileChunk)
                                    }
                                }
                            }
                        }
                        it.hasCompiledFileChunk() ->{
                            launch {
                                val fileName = it.compiledFileChunk.filePath.split("/").last()
                                requestHandler.receiveFile(
                                    filePath = it.compiledFileChunk.filePath,
                                    newFilePath = buildAbsPath("$CLIENT_COMPILED_DIR/$fileName"),
                                    chunk = it.compiledFileChunk.content.toByteArray(),
                                    isLast = it.compiledFileChunk.isLast
                                )
                            }
                        }
                    }
                }
            }

            launch {
                // as a first step we want to send compilation metadata
                requestChannel.send(
                    requestHandler.buildCompilationMetadata(
                        sessionId,
                        compilationOptions,
                        compilerArguments,
                        sourceFiles.size
                    )
                )

                // process a stream of file request and pass it to a request channel
                requestHandler.buildFileTransferRequestStream(sourceFiles).collect {
                    requestChannel.send(it)
                }
            }
        }
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

}


suspend fun main(args: Array<String>) {
    val port = System.getenv("PORT")?.toInt() ?: 50051

    val channel = ManagedChannelBuilder
        .forAddress("localhost", port)
        .usePlaintext()
        .intercept(RemoteClientInterceptor())
        .build()

    val client = RemoteDaemonClient(channel)

    val compilationOptions = CompilationOptions(
        compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
        targetPlatform = CompileService.TargetPlatform.JVM,
        reportSeverity = 0,
        reportCategories = arrayOf(),
        requestedCompilationResults = arrayOf(),
    )

    val compilerArguments = arrayOf<String>()

    val sourceFiles = listOf(
        File("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/client/input/Input.kt"),
        File("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/client/input/Input2.kt"),
        File("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/client/input/Input3.kt")
    )

    var sessionId: Int? = null

    while (true) {
        print("Enter command: ")
        val command = readLine()?.trim()?.lowercase()

        when (command) {
            "connect" -> {
                client.connect(
                    DaemonJVMOptionsConfigurator(
                        inheritMemoryLimits = true,
                        inheritOtherJvmOptions = false,
                        inheritAdditionalProperties = true,
                    )
                ).collect { response ->
                    println("collecting message")
                    when {
                        response.hasDaemonMessage() -> {
                            // TODO handle messages however you want
                        }
                        response.hasSessionId() -> {
                            println("we obtained a sessionID ${response.sessionId}")
                            sessionId = response.sessionId
                        }
                    }
                }
            }
            "compile" -> {
                if (sessionId == null) {
                    println("You need to connect first")
                    continue
                }

                client.compile(
                    sessionId = sessionId,
                    compilerArguments = compilerArguments,
                    compilationOptions = compilationOptions,
                    sourceFiles = sourceFiles
                )
            }
            else -> {
                println("Unknown command")
            }
        }
    }
}