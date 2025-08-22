/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import common.CompilerUtils
import common.SERVER_ARTIFACTS_CACHE_DIR
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.runTest
import model.ArtifactType
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
import java.io.File
import kotlin.test.assertTrue

class CacheTest : BaseCompilationCompilationTest() {

    val compilerArguments = mapOf(
        CompilerUtils.DIRECTORY_ARG to "willbeoverwrittenanyway",
        CompilerUtils.CLASS_PATH_ARG to stdLibFilePath,
        CompilerUtils.SOURCE_FILE_ARG to sourceFile.path,
    )

    private suspend fun sendFile(file: File, artifactType: ArtifactType, channel: Channel<CompileRequest>) {
        fileChunkingStrategy.chunk(file, isDirectory = false, artifactType = artifactType)
            .collect { chunk ->
                channel.send(
                    FileChunk(
                        chunk.filePath,
                        artifactType,
                        chunk.content,
                        isDirectory = false,
                        chunk.isLast
                    )
                )
            }
    }

    private val compilationMetadata = CompilationMetadata(
        projectName,
        1,
        1,
        0,
        compilerArguments,
        CompilationOptions(
            compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            targetPlatform = CompileService.TargetPlatform.JVM,
            reportSeverity = 0,
            reportCategories = arrayOf(),
            requestedCompilationResults = arrayOf(),
        )
    )

    private val sourceFileRequest = FileTransferRequest(
        sourceFile.path,
        sourceFileFingerprint,
        ArtifactType.SOURCE
    )

    private val dependencyFileRequest = FileTransferRequest(
        stdLibFilePath,
        stdLibFileFingerprint,
        ArtifactType.DEPENDENCY
    )


    @Test
    fun testIfSourceAndDependencyFilesWereCached() = runTest {
        val client = getGrpcClient()
        val requestChannel = Channel<CompileRequest>(capacity = Channel.UNLIMITED)

        requestChannel.send(compilationMetadata)
        requestChannel.send(sourceFileRequest)
        requestChannel.send(dependencyFileRequest)

        var uploadedSource = false
        var uploadedStdlib = false

        client.compile(requestChannel.receiveAsFlow())
            .takeWhile { it !is FileChunk }
            .collect {
                if (it is FileTransferReply && !it.isPresent) {
                    when {
                        it.filePath.contains(sourceFile.name) -> {
                            sendFile(sourceFile, ArtifactType.SOURCE, requestChannel)
                            uploadedSource = true
                        }
                        it.filePath == stdLibFilePath || it.filePath.endsWith(File(stdLibFilePath).name) -> {
                            sendFile(File(stdLibFilePath), ArtifactType.DEPENDENCY, requestChannel)
                            uploadedStdlib = true
                        }
                    }
                    if (uploadedSource && uploadedStdlib) {
                        requestChannel.close()
                    }
                }
            }

        assertTrue { SERVER_ARTIFACTS_CACHE_DIR.resolve(sourceFileFingerprint).toFile().exists() }
        assertTrue { SERVER_ARTIFACTS_CACHE_DIR.resolve(stdLibFilePath).toFile().exists() }
    }

    @Test
    fun testIfCompilationResultWasReturnedFromCache() = runTest {
        val client = getGrpcClient()
        val requestChannel = Channel<CompileRequest>(capacity = Channel.UNLIMITED)

        requestChannel.send(compilationMetadata)
        requestChannel.send(sourceFileRequest)
        requestChannel.send(dependencyFileRequest)

        var client1ReceivedCompiledChunks = false
        client.compile(requestChannel.receiveAsFlow()).collect {
            if (it is FileTransferReply && !it.isPresent) {
                when {
                    it.filePath.contains(sourceFile.name) -> {
                        sendFile(sourceFile, ArtifactType.SOURCE, requestChannel)
                    }
                    it.filePath == stdLibFilePath || it.filePath.endsWith(File(stdLibFilePath).name) -> {
                        sendFile(File(stdLibFilePath), ArtifactType.DEPENDENCY, requestChannel)
                    }
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
        requestChannel2.send(sourceFileRequest)
        requestChannel2.send(dependencyFileRequest)

        var client2ReceivedCompiledChunks = false
        client2.compile(requestChannel2.receiveAsFlow()).collect {
            if (it is FileTransferReply) {
                assert(it.isPresent)
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
