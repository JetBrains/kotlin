/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.grpc

import common.FixedSizeChunkingStrategy
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import server.auth.BasicHTTPAuthServer
import server.core.CacheHandler
import server.core.InProcessCompilerService
import server.core.RemoteCompilationServiceImpl
import server.core.Server
import server.core.WorkspaceManager


class GrpcRemoteCompilationServerImpl(
    port: Int,
    private val logging: Boolean = false,
) : Server {

    private val fileChunkingStrategy = FixedSizeChunkingStrategy()
    private val cacheHandler = CacheHandler()

    val server: io.grpc.Server =
        ServerBuilder
            .forPort(port)
            .addService(
                ServerInterceptors
                    .intercept(
                        GrpcRemoteCompilationService(
                            RemoteCompilationServiceImpl(
                                cacheHandler,
                                fileChunkingStrategy,
                                InProcessCompilerService(),
                                logging = logging,
                            )
                        ),
//                        listOfNotNull(
//                            if (logging) LoggingServerInterceptor() else null,
//                            AuthServerInterceptor(BasicHTTPAuthServer())
//                        ),
                    )
            ).build()


    override fun start(block: Boolean) {
        server.start()
        println("GRPC server is running: localhost:${server.port}")
        if (block) {
            server.awaitTermination()
        }
    }

    override fun stop() {
        server.shutdownNow()
    }

    override fun cleanup() {
        cacheHandler.cleanup()
        WorkspaceManager.cleanup()
    }
}