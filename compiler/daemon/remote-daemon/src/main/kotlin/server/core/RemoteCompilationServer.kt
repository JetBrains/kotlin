/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package main.kotlin.server

import server.core.Server
import server.grpc.GrpcRemoteCompilationServerImpl

class RemoteCompilationServer(
    private val serverImpl: Server
) : Server {

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
        val port = 7777
        val server = RemoteCompilationServer(GrpcRemoteCompilationServerImpl(port))
        server.start(block = true)
    } catch (e: Exception) {
        println("error occurred: ${e.message}")
        e.printStackTrace()
    }
}