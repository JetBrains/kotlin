import client.auth.BasicHTTPAuthClient
import client.auth.CallAuthenticator
import client.GrpcClientRemoteCompilationService
import common.OneFileOneChunkStrategy
import common.RemoteCompilationService
import common.SERVER_COMPILATION_WORKSPACE_DIR
import common.computeSha256
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import org.jetbrains.kotlin.server.CompileServiceGrpcKt
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import server.core.CacheHandler
import server.GrpcRemoteCompilationService
import server.core.InProcessCompilerService
import server.auth.BasicHTTPAuthServer
import server.core.RemoteCompilationServiceImpl
import server.core.WorkspaceManager
import server.interceptors.AuthInterceptor
import server.interceptors.LoggingInterceptor
import java.io.File

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class BaseCompilationCompilationTest {

    protected val projectName = "testProject"
    protected val sourceFile = this::class.java.getResource("/TestInput.kt")
        ?.toURI()
        ?.let { File(it) }
        ?: throw IllegalStateException("Resource /TestInput.kt not found")

    protected val sourceFileFingerprint = computeSha256(sourceFile)
    protected val fileChunkingStrategy = OneFileOneChunkStrategy()

    companion object {
        protected const val SERVER_NAME = "test-kotlin-daemon-server"
        protected lateinit var server: Server
        protected lateinit var channel: ManagedChannel
        protected val cacheHandler = CacheHandler(OneFileOneChunkStrategy())
    }

    fun getGrpcClient(): RemoteCompilationService{
        return GrpcClientRemoteCompilationService()
    }

    @BeforeEach
    fun setup() {
        server = InProcessServerBuilder
            .forName(SERVER_NAME)
            .addService(
                ServerInterceptors
                    .intercept(
                        GrpcRemoteCompilationService(
                            RemoteCompilationServiceImpl(
                                cacheHandler,
                                InProcessCompilerService(),
                                WorkspaceManager()
                            )
                        ),
                        LoggingInterceptor(),
                        AuthInterceptor(BasicHTTPAuthServer())
                    )
            )
            .build()
            .start()
        channel = InProcessChannelBuilder.forName(SERVER_NAME).build()
    }

    @AfterEach
    fun cleanup() {
        cacheHandler.clear()
        File(SERVER_COMPILATION_WORKSPACE_DIR).deleteRecursively()
        server.shutdownNow()
        channel.shutdownNow()
    }
}
