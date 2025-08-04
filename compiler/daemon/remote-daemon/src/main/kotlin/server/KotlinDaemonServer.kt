/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package main.kotlin.server

import common.SERVER_SOURCE_FILES_CACHE_DIR
import server.OnReport
import common.OUTPUT_FILES_DIR
import common.OneFileOneChunkStrategy
import common.SERVER_COMPILATION_WORKSPACE_DIR
import server.RemoteMessageCollector
import common.buildAbsPath
import common.computeSha256
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import server.CacheHandler
import server.GrpcRemoteCompilationService
import server.InProcessCompilationService import server.interceptors.LoggingInterceptor
import server.auth.BasicHTTPAuthServer
import server.interceptors.AuthInterceptor


class RemoteKotlinDaemonServer(private val port: Int) {

    private val fileChunkingStrategy = OneFileOneChunkStrategy()
    val cacheHandler = CacheHandler(fileChunkingStrategy)
    private val compilationService = InProcessCompilationService()

    val server: Server =
        ServerBuilder
            .forPort(port)
            .addService(
                ServerInterceptors
                    .intercept(
                        GrpcRemoteCompilationService(
                            cacheHandler,
                            compilationService
                        ),
                        LoggingInterceptor(),
                        AuthInterceptor(BasicHTTPAuthServer())
                    )
            )
            .build()



    fun start() {
        cacheHandler.loadSourceFilesCache()
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
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }


}

fun main() {
    try {
        val port = System.getenv("PORT")?.toInt() ?: 50051
        val server = RemoteKotlinDaemonServer(port)
        server.start()
        server.blockUntilShutdown()
    }catch (e: Exception){
        println("error occurred: ${e.message}")
        e.printStackTrace()
    }
}