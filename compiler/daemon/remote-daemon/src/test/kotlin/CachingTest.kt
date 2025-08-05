/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


import client.auth.BasicHTTPAuthClient
import client.auth.CallAuthenticator
import common.OneFileOneChunkStrategy
import common.SERVER_CACHE_CACHE_DIR
import common.SERVER_COMPILATION_WORKSPACE_DIR
import common.SERVER_SOURCE_FILES_CACHE_DIR
import common.computeSha256
import common.toCompileRequest
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import model.CompilationMetadata
import model.FileTransferRequest
import model.toGrpc
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.server.CompileRequestGrpc
import org.jetbrains.kotlin.server.CompileServiceGrpcKt
import org.jetbrains.kotlin.server.FileTransferReplyGrpc
import org.jetbrains.kotlin.server.FileTransferRequestGrpc
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import server.CacheHandler
import server.GrpcRemoteCompilationService
import server.InProcessCompilationService
import java.io.File
import kotlin.collections.forEach
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.expect

class CachingTest {

    companion object {

        private const val serverName = "test-kotlin-daemon-server"
        private lateinit var server: Server
        private lateinit var channel: ManagedChannel

        @JvmStatic
        @BeforeAll
        fun setup() {
            val cacheHandler = CacheHandler(OneFileOneChunkStrategy())
            val compilationService = InProcessCompilationService()
            server = InProcessServerBuilder
                .forName(serverName)
                .addService(
                    GrpcRemoteCompilationService(
                        cacheHandler, compilationService
                    )
                )
                .build()
                .start()
            channel = InProcessChannelBuilder.forName(serverName).build()
        }


        fun getClient(): CompileServiceGrpcKt.CompileServiceCoroutineStub {
            return CompileServiceGrpcKt
                .CompileServiceCoroutineStub(channel)
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


    @AfterEach
    fun cleanup() {
        println("after each called ")
        val currentDir = File(System.getProperty("user.dir"))

        println("current dir: ${currentDir.absolutePath}")
        val cacheDir = File(SERVER_CACHE_CACHE_DIR)
        val cacheDirAbsolute = File("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/server/cache")

        println("cache dir aboslute exists: ${cacheDirAbsolute.exists()}")
        println("cache dir aboslute path: ${cacheDirAbsolute.absolutePath}")
        println("Same canonical path: ${cacheDir.canonicalFile == cacheDirAbsolute.canonicalFile}")

        println("cache dir: ${cacheDir.absolutePath}")
        println("cache exist: ${cacheDir.exists()}")
        cacheDir.deleteRecursively()
        File(SERVER_COMPILATION_WORKSPACE_DIR).deleteRecursively()
    }


    @Test
    fun testIfAbsentFileIsSavedInCache() = runTest {
        val client = getClient()
        val sourceFile = CachingTest::class.java.getResource("/TestInput.kt")
            ?.toURI()
            ?.let { File(it) }
            ?: throw IllegalStateException("Resource /TestInput.kt not found")

        val sourceFileFingerprint = computeSha256(sourceFile)

        val compileRequestFlow = flow{
            emit(CompilationMetadata(
                    "testingProject",
                    1,
                    listOf(),
                    CompilationOptions(
                        compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                        targetPlatform = CompileService.TargetPlatform.JVM,
                        reportSeverity = 0,
                        reportCategories = arrayOf(),
                        requestedCompilationResults = arrayOf(),
                    )
                ).toGrpc().toCompileRequest())

            emit(
                FileTransferRequest(
                    sourceFile.path,
                    sourceFileFingerprint
                ).toGrpc().toCompileRequest()
            )
        }

        val response = client.compile(compileRequestFlow).first()
        assertTrue { response.hasFileTransferReply() }
        assertFalse { response.fileTransferReply.isPresent }
        Thread.sleep(10000)
        assertTrue { File("$SERVER_SOURCE_FILES_CACHE_DIR/$sourceFileFingerprint").exists() }
    }
}
