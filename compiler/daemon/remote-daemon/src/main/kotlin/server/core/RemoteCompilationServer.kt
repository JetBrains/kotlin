/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package main.kotlin.server

import benchmark.RemoteCompilationServiceImplType
import kotlinx.rpc.krpc.serialization.cbor.cbor
import kotlinx.serialization.ExperimentalSerializationApi
import server.core.Server
import server.grpc.GrpcRemoteCompilationServerImpl
import server.kotlinxrpc.KotlinxRpcRemoteCompilationServerImpl

class RemoteCompilationServer(
    private val serverImpl: Server
) : Server {

    companion object {
        fun getServer(implType: RemoteCompilationServiceImplType, port: Int, logging: Boolean = false): RemoteCompilationServer {
            val serverImpl = when (implType) {
                RemoteCompilationServiceImplType.GRPC -> {
                    GrpcRemoteCompilationServerImpl(port, logging)
                }
                RemoteCompilationServiceImplType.KOTLINX_RPC -> {
                    @OptIn(ExperimentalSerializationApi::class)
                    KotlinxRpcRemoteCompilationServerImpl(
                        port,
                        logging = logging,
                        serialization = { cbor() }
                    )
                }
            }
            return RemoteCompilationServer(serverImpl)
        }
    }

    override fun start(block: Boolean) {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("Shutdown hook called, shutting down server")
                cleanup() // TODO: this is just a convenient for testing and debugging
                stop()
            },
        )
        serverImpl.start(block)
    }

    override fun stop() {
        serverImpl.stop()
    }

    override fun cleanup() {
        serverImpl.cleanup()
    }
}

fun main() {
    try {
        val port = 8000
        val server = RemoteCompilationServer.getServer(RemoteCompilationServiceImplType.GRPC, port, logging = true)
        server.start(block = true)
    } catch (e: Exception) {
        println("error occurred: ${e.message}")
        e.printStackTrace()
    }
}