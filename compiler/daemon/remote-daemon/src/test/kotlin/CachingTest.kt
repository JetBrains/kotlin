/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import client.RequestHandler
import client.auth.BasicHTTPAuthClient
import client.auth.CallAuthenticator
import common.OneFileOneChunkStrategy
import common.SERVER_COMPILATION_RESULT_DIR
import common.SERVER_COMPILATION_WORKSPACE_DIR
import common.SERVER_SOURCE_FILES_CACHE_DIR
import common.calculateCompilationInputHash
import common.computeSha256
import common.toCompileRequest
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.runTest
import model.CompilationMetadata
import model.FileTransferRequest
import model.toGrpc
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.server.CompileRequestGrpc
import org.jetbrains.kotlin.server.CompileServiceGrpcKt
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import server.CacheHandler
import server.GrpcRemoteCompilationService
import server.InProcessCompilationService
import server.auth.BasicHTTPAuthServer
import server.interceptors.AuthInterceptor
import server.interceptors.LoggingInterceptor
import java.io.File
import java.nio.file.Paths
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CachingTest {

    private val projectName = "testProject"
    private val sourceFile = CachingTest::class.java.getResource("/TestInput.kt")
        ?.toURI()
        ?.let { File(it) }
        ?: throw IllegalStateException("Resource /TestInput.kt not found")
    private val sourceFileFingerprint = computeSha256(sourceFile)

    companion object {

        private const val SERVER_NAME = "test-kotlin-daemon-server"
        private lateinit var server: Server
        private lateinit var channel: ManagedChannel
        private val cacheHandler = CacheHandler(OneFileOneChunkStrategy())

        fun getClient(): CompileServiceGrpcKt.CompileServiceCoroutineStub {
            return CompileServiceGrpcKt
                .CompileServiceCoroutineStub(channel)
//                .withInterceptors(RemoteClientInterceptor())
                .withCallCredentials(
                    CallAuthenticator(
                        BasicHTTPAuthClient(
                            username = "admin",
                            password = "admin"
                        )
                    )
                )
        }
    }

    @BeforeEach
    fun setup() {
        val cacheHandler = CacheHandler(OneFileOneChunkStrategy())
        val compilationService = InProcessCompilationService()
        server = InProcessServerBuilder
            .forName(SERVER_NAME)
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

    @Test
    fun testIfAbsentSourceFileIsSavedInCache() = runTest {
        val client = getClient()
        val requestChannel = Channel<CompileRequestGrpc>(capacity = Channel.UNLIMITED)

        requestChannel.send(
            CompilationMetadata(
                projectName,
                1,
                listOf(),
                CompilationOptions(
                    compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                    targetPlatform = CompileService.TargetPlatform.JVM,
                    reportSeverity = 0,
                    reportCategories = arrayOf(),
                    requestedCompilationResults = arrayOf(),
                )
            ).toGrpc().toCompileRequest()
        )

        requestChannel.send(
            FileTransferRequest(
                sourceFile.path,
                sourceFileFingerprint
            ).toGrpc().toCompileRequest()
        )

        client.compile(requestChannel.receiveAsFlow())
            .takeWhile { !it.hasCompiledFileChunk() }
            .collect {
                if (it.hasFileTransferReply() && it.fileTransferReply.filePath.contains(sourceFile.name)) {
                    assertFalse { it.fileTransferReply.isPresent }
                    RequestHandler(OneFileOneChunkStrategy())
                        .buildFileChunkStream(sourceFile.path).collect { chunk ->
                            requestChannel.send(chunk)
                        }
                    requestChannel.close()
                }
            }

        assertTrue { File("$SERVER_SOURCE_FILES_CACHE_DIR/$sourceFileFingerprint").exists() }
    }

    @Test
    fun testIfAbsentCompilationResultIsSavedInCache() = runTest {
        val client = getClient()
        val requestChannel = Channel<CompileRequestGrpc>(capacity = Channel.UNLIMITED)

        requestChannel.send(
            CompilationMetadata(
                projectName,
                1,
                listOf(),
                CompilationOptions(
                    compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                    targetPlatform = CompileService.TargetPlatform.JVM,
                    reportSeverity = 0,
                    reportCategories = arrayOf(),
                    requestedCompilationResults = arrayOf(),
                )
            ).toGrpc().toCompileRequest()
        )

        requestChannel.send(
            FileTransferRequest(
                sourceFile.path,
                sourceFileFingerprint
            ).toGrpc().toCompileRequest()
        )

        client.compile(requestChannel.receiveAsFlow()).collect {
            if (it.hasFileTransferReply() && !it.fileTransferReply.isPresent) {
                RequestHandler(OneFileOneChunkStrategy())
                    .buildFileChunkStream(sourceFile.path).collect { chunk ->
                        requestChannel.send(chunk)
                    }
            }
        }

        val compilerInputFingerprint = calculateCompilationInputHash(
            listOf(sourceFile), InProcessCompilationService.buildCompilerArgsWithoutSourceFiles(
                Paths.get("does/not/matter/it/gets/removed/anyway"),
                listOf("")
            ), "2.0"
        )
        assertTrue { File("$SERVER_COMPILATION_RESULT_DIR/$compilerInputFingerprint").exists() }
    }
}
