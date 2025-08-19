/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package main.kotlin.server

import common.OneFileOneChunkStrategy
import common.RemoteCompilationServiceImplType
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import server.GrpcRemoteCompilationService
import server.core.CacheHandler
import server.core.InProcessCompilerService
import server.interceptors.LoggingInterceptor
import server.auth.BasicHTTPAuthServer
import server.core.RemoteCompilationServiceImpl
import server.core.WorkspaceManager
import server.interceptors.AuthInterceptor

class RemoteCompilationServer(
    private val port: Int,
    private val serverImplType: RemoteCompilationServiceImplType
) {

    private val fileChunkingStrategy = OneFileOneChunkStrategy()
    private val cacheHandler = CacheHandler(fileChunkingStrategy)
    private val workspaceManager = WorkspaceManager()

    val server: Server =
        ServerBuilder
            .forPort(port)
            .addService(
                ServerInterceptors
                    .intercept(
                        GrpcRemoteCompilationService(
                            RemoteCompilationServiceImpl(
                                cacheHandler,
                                InProcessCompilerService(),
                                workspaceManager
                            )
                        ),
                        LoggingInterceptor(),
                        AuthInterceptor(BasicHTTPAuthServer())
                    )
            )
            .build()

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@RemoteCompilationServer.stop()
                println("*** server shut down")
            },
        )
    }

    private fun stop() {
        cleanup()
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    fun cleanup() {
        cacheHandler.cleanup()
        workspaceManager.cleanup()
    }
}

fun main() {
    try {
        val port = System.getenv("PORT")?.toInt() ?: 50051
        val server = RemoteCompilationServer(port, RemoteCompilationServiceImplType.GRPC)
        server.start()
        server.blockUntilShutdown()
    } catch (e: Exception) {
        println("error occurred: ${e.message}")
        e.printStackTrace()
    }
}