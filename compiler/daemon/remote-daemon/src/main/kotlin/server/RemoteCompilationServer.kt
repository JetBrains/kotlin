/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package main.kotlin.server

import common.OneFileOneChunkStrategy
import common.RemoteCompilationService
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
import io.ktor.server.application.*
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json

enum class ServerImplType {
    GRPC,
    RPC,
}

class RemoteCompilationServer(
    private val port: Int,
    private val implType: ServerImplType
) {

    private val fileChunkingStrategy = OneFileOneChunkStrategy()
    private val cacheHandler = CacheHandler(fileChunkingStrategy)
    private val workspaceManager = WorkspaceManager()
    private var grpcServer: Server? = null
    private var rpcServer: EmbeddedServer<*, *>? = null

    private fun startGrpcServer() {
        grpcServer =
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
                .build().apply {
                    start()
                    awaitTermination()
                }
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@RemoteCompilationServer.stop()
                println("*** server shut down")
            },
        )
    }

    private fun startRpcServer() {
        fun Application.module() {
            install(Krpc)

            routing {
                rpc("/compile") {
                    rpcConfig {
                        serialization {
                            json()
                        }
                    }

                    registerService<RemoteCompilationService> {
                        RemoteCompilationServiceImpl(
                            cacheHandler,
                            InProcessCompilerService(),
                            workspaceManager
                        )
                    }
                }
            }
        }

        rpcServer = embeddedServer(Netty, port = port) {
            module()
            println("Server running")
        }.apply {
            start(wait = true)
            addShutdownHook { stop() }
        }
    }


    fun start() {
        when (implType) {
            ServerImplType.GRPC -> startGrpcServer()
            ServerImplType.RPC -> startRpcServer()
        }
    }

    private fun stop() {
        cleanup()
        when (implType) {
            ServerImplType.GRPC -> grpcServer?.shutdown()
            ServerImplType.RPC -> rpcServer?.stop(1000, 1000)
        }
    }

    fun cleanup() {
        cacheHandler.cleanup()
        workspaceManager.cleanup()
    }
}

fun main() {
    try {
        val port = System.getenv("PORT")?.toInt() ?: 50051
        val server = RemoteCompilationServer(port, ServerImplType.RPC)
        server.start()
    } catch (e: Exception) {
        println("error occurred: ${e.message}")
        e.printStackTrace()
    }
}