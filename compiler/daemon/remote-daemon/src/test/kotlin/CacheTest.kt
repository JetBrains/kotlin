/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import client.RequestHandler
import common.OneFileOneChunkStrategy
import common.SERVER_COMPILATION_RESULT_DIR
import common.SERVER_SOURCE_FILES_CACHE_DIR
import common.calculateCompilationInputHash
import common.toCompileRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.runTest
import model.CompilationMetadata
import model.CompilationResultSource
import model.FileTransferRequest
import model.toDomain
import model.toGrpc
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.server.CompileRequestGrpc
import org.junit.jupiter.api.Test
import server.InProcessCompilationService
import java.io.File
import java.nio.file.Paths
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CacheTest : BaseCompilationCompilationTest() {

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
                        .buildFileChunkStream(sourceFile).collect { chunk ->
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
                    .buildFileChunkStream(sourceFile).collect { chunk ->
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

    @Test
    fun testIfCompilationResultWasReturnedFromCache() = runTest {
        val client = getClient()
        val requestChannel = Channel<CompileRequestGrpc>(capacity = Channel.UNLIMITED)

        val compilationMetadata = CompilationMetadata(
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

        val fileTransferRequest = FileTransferRequest(
            sourceFile.path,
            sourceFileFingerprint
        ).toGrpc().toCompileRequest()

        requestChannel.send(compilationMetadata)
        requestChannel.send(fileTransferRequest)

        var client1ReceivedCompiledChunks = false
        client.compile(requestChannel.receiveAsFlow()).collect {
            if (it.hasFileTransferReply() && !it.fileTransferReply.isPresent) {
                RequestHandler(OneFileOneChunkStrategy())
                    .buildFileChunkStream(sourceFile).collect { chunk ->
                        requestChannel.send(chunk)
                    }
            }
            if (it.hasCompilationResult()) {
                assert(it.compilationResult.resultSource.toDomain() == CompilationResultSource.COMPILER)
            }
            if (it.hasCompilationResult()) {
                client1ReceivedCompiledChunks = true
            }
        }

        assertTrue { client1ReceivedCompiledChunks }

        val client2 = getClient()
        val requestChannel2 = Channel<CompileRequestGrpc>(capacity = Channel.UNLIMITED)

        requestChannel2.send(compilationMetadata)
        requestChannel2.send(fileTransferRequest)

        var client2ReceivedCompiledChunks = false
        client2.compile(requestChannel2.receiveAsFlow()).collect {
            if (it.hasFileTransferReply() && !it.fileTransferReply.isPresent) {
                RequestHandler(OneFileOneChunkStrategy())
                    .buildFileChunkStream(sourceFile).collect { chunk ->
                        requestChannel.send(chunk)
                    }
            }
            if (it.hasCompilationResult()) {
                assert(it.compilationResult.resultSource.toDomain() == CompilationResultSource.CACHE)
            }
            if (it.hasCompilationResult()) {
                client2ReceivedCompiledChunks = true
            }
        }
        assertTrue { client2ReceivedCompiledChunks }
    }
}
