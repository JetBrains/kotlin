/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import common.SERVER_COMPILATION_RESULT_DIR
import common.SERVER_SOURCE_FILES_CACHE_DIR
import common.calculateCompilationInputHash
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.runTest
import model.CompilationMetadata
import model.CompilationResult
import model.CompilationResultSource
import model.CompileRequest
import model.FileChunk
import model.FileTransferReply
import model.FileTransferRequest
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.junit.jupiter.api.Test
import server.core.InProcessCompilerService
import java.io.File
import java.nio.file.Paths
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CacheTest : BaseCompilationCompilationTest() {

    @Test
    fun testIfAbsentSourceFileIsSavedInCache() = runTest {
        val client = getGrpcClient()
        val requestChannel = Channel<CompileRequest>(capacity = Channel.UNLIMITED)

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
            )
        )

        requestChannel.send(
            FileTransferRequest(
                sourceFile.path,
                sourceFileFingerprint
            )
        )

        client.compile(requestChannel.receiveAsFlow())
            .takeWhile { it !is FileChunk }
            .collect {
                if (it is FileTransferReply && it.filePath.contains(sourceFile.name)) {
                    assertFalse { it.isPresent }
                    fileChunkingStrategy.chunk(sourceFile).collect { chunk ->
                        requestChannel.send(FileChunk(
                                chunk.filePath,
                                chunk.content,
                                chunk.isLast
                            )
                        )
                    }
                    requestChannel.close()
                }
            }

        assertTrue { File("$SERVER_SOURCE_FILES_CACHE_DIR/$sourceFileFingerprint").exists() }
    }

    @Test
    fun testIfAbsentCompilationResultIsSavedInCache() = runTest {
        val client = getGrpcClient()
        val requestChannel = Channel<CompileRequest>(capacity = Channel.UNLIMITED)

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
            )
        )

        requestChannel.send(
            FileTransferRequest(
                sourceFile.path,
                sourceFileFingerprint
            )
        )

        client.compile(requestChannel.receiveAsFlow()).collect {
            if (it is FileTransferReply && !it.isPresent) {
                fileChunkingStrategy.chunk(sourceFile).collect { chunk ->
                    requestChannel.send(FileChunk(
                            chunk.filePath,
                            chunk.content,
                            chunk.isLast
                        )
                    )
                }
            }
        }

        val compilerInputFingerprint = calculateCompilationInputHash(
            listOf(sourceFile), InProcessCompilerService.buildCompilerArgsWithoutSourceFiles(
                Paths.get("does/not/matter/it/gets/removed/anyway"),
                listOf("")
            ), "2.0"
        )
        assertTrue { File("$SERVER_COMPILATION_RESULT_DIR/$compilerInputFingerprint").exists() }
    }

    @Test
    fun testIfCompilationResultWasReturnedFromCache() = runTest {
        val client = getGrpcClient()
        val requestChannel = Channel<CompileRequest>(capacity = Channel.UNLIMITED)

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
        )

        val fileTransferRequest = FileTransferRequest(
            sourceFile.path,
            sourceFileFingerprint
        )

        requestChannel.send(compilationMetadata)
        requestChannel.send(fileTransferRequest)

        var client1ReceivedCompiledChunks = false
        client.compile(requestChannel.receiveAsFlow()).collect {
            if (it is FileTransferReply && !it.isPresent) {
                fileChunkingStrategy.chunk(sourceFile).collect { chunk ->
                    requestChannel.send(
                        FileChunk(
                            chunk.filePath,
                            chunk.content,
                            chunk.isLast
                        )
                    )
                }
            }
            if (it is CompilationResult) {
                assert(it.compilationResultSource == CompilationResultSource.COMPILER)
            }
            if (it is FileChunk) {
                client1ReceivedCompiledChunks = true
            }
        }

        assertTrue { client1ReceivedCompiledChunks }

        val client2 = getGrpcClient()
        val requestChannel2 = Channel<CompileRequest>(capacity = Channel.UNLIMITED)

        requestChannel2.send(compilationMetadata)
        requestChannel2.send(fileTransferRequest)

        var client2ReceivedCompiledChunks = false
        client2.compile(requestChannel2.receiveAsFlow()).collect {
            if (it is FileTransferReply && !it.isPresent) {
                fileChunkingStrategy.chunk(sourceFile).collect { chunk ->
                    requestChannel.send(
                        FileChunk(
                            chunk.filePath,
                            chunk.content,
                            chunk.isLast
                        )
                    )
                }
            }
            if (it is CompilationResult) {
                assert(it.compilationResultSource == CompilationResultSource.CACHE)
            }
            if (it is FileChunk) {
                client2ReceivedCompiledChunks = true
            }
        }
        assertTrue { client2ReceivedCompiledChunks }
    }
}
