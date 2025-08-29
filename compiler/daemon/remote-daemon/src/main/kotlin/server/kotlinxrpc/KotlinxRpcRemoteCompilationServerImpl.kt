package server.kotlinxrpc/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import common.FixedSizeChunkingStrategy
import common.RemoteCompilationService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import server.core.CacheHandler
import server.core.InProcessCompilerService
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.rpc.krpc.serialization.KrpcSerialFormatConfiguration
import server.core.RemoteCompilationServiceImpl
import server.core.Server
import server.core.WorkspaceManager

class KotlinxRpcRemoteCompilationServerImpl(
    val port: Int,
    val logging: Boolean = false,
    val serialization: KrpcSerialFormatConfiguration.() -> Unit
) : Server {

    val cacheHandler = CacheHandler()
    val workspaceManager = WorkspaceManager()
    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    fun Application.module() {
        install(Krpc)

        routing {
            rpc("/compile") {
                rpcConfig {
                    serialization {
                        this.serialization()
                    }
                }
                registerService<RemoteCompilationService> {
                    RemoteCompilationServiceImpl(
                        cacheHandler,
                        workspaceManager,
                        FixedSizeChunkingStrategy(),
                        InProcessCompilerService(),
                        logging
                    )
                }
            }
        }
    }

    override fun start(block: Boolean) {
        server = embeddedServer(Netty, port) {
            module()
        }
        server.start(wait = block)
    }

    override fun stop() {
        server.stop(1000, 1000)
    }

    override fun cleanup() {
        cacheHandler.cleanup()
        workspaceManager.cleanup()
    }
}