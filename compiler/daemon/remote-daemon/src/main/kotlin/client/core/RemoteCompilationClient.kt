/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package client.core

import benchmark.RemoteCompilationServiceImplType
import client.grpc.GrpcRemoteCompilationServiceClient
import com.example.KotlinxRpcRemoteCompilationServiceClient
import common.CLIENT_TMP_DIR
import common.CompilerUtils
import common.FixedSizeChunkingStrategy
import common.calculateClasspathSnapshot
import common.computeSha256
import common.createTarArchiveStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.serialization.protobuf.protobuf
import kotlinx.serialization.ExperimentalSerializationApi
import model.Artifact
import model.ArtifactType
import model.CompilationMetadata
import model.CompilationResult
import model.CompileRequest
import model.CompileResponse
import model.CompilerMessage
import model.FileChunk
import model.FileTransferReply
import model.FileTransferRequest
import model.MissingArtifactsRequest
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import org.jetbrains.kotlin.incremental.IncrementalCompilationFeatures
import org.jetbrains.kotlin.incremental.classpathAsList
import java.nio.file.Paths
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name

@OptIn(ExperimentalPathApi::class)
class RemoteCompilationClient(
    val clientImpl: Client,
    val logging: Boolean = false
) {

    val fileChunkStrategy = FixedSizeChunkingStrategy()

    companion object {

        fun getClient(
            implType: RemoteCompilationServiceImplType,
            host: String = "localhost",
            port: Int,
            logging: Boolean = false
        ): RemoteCompilationClient {
            val clientImpl = when (implType) {
                RemoteCompilationServiceImplType.KOTLINX_RPC -> {
                    @OptIn(ExperimentalSerializationApi::class)
                    KotlinxRpcRemoteCompilationServiceClient(
                        host,
                        port,
                        serialization = { protobuf() }
                    )
                }
                RemoteCompilationServiceImplType.GRPC -> {
                    GrpcRemoteCompilationServiceClient(host, port, logging)
                }
            }
            return RemoteCompilationClient(clientImpl, logging)
        }
    }

    fun debug(text: String) {
        if (logging) {
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            println("[${LocalDateTime.now().format(formatter)}] [thread=${Thread.currentThread().name}] DEBUG SERVER: $text")
        }
    }

    suspend fun compile(projectName: String, compilerArguments: List<String>, compilationOptions: CompilationOptions): CompilationResult {
        val parsedArgs = CompilerUtils.parseArgs(compilerArguments)
        val allArtifactsMap = HashMap<File, MutableSet<ArtifactType>>()

        CompilerUtils.getSourceFiles(parsedArgs).forEach { sourceFile ->
            allArtifactsMap.getOrPut(sourceFile) { mutableSetOf() }.add(ArtifactType.SOURCE)
        }
        CompilerUtils.getDependencyFiles(parsedArgs).forEach {
            allArtifactsMap.getOrPut(it) { mutableSetOf() }.add(ArtifactType.DEPENDENCY)
        }
        CompilerUtils.getXPluginFiles(parsedArgs).forEach {
            allArtifactsMap.getOrPut(it) { mutableSetOf() }.add(ArtifactType.COMPILER_PLUGIN)
        }

        // there are cases when a single file can appear in multiple places, e.g. in dependencies and compiler plugins
        // we want to send each file only once to prevent concurrency issues when multiple threads try to deal with the same file
        val allArtifacts = allArtifactsMap.map { Artifact(it.key, it.value) }.toSet()

        val fileChunks = ConcurrentHashMap<String, MutableList<FileChunk>>()

        val requestChannel = Channel<CompileRequest>(capacity = Channel.UNLIMITED)
        val responseChannel = Channel<CompileResponse>(capacity = Channel.UNLIMITED)
        var compilationResult: CompilationResult? = null

        fun calculateTotalFiles(): Int {
            var total = 0
            when (compilationOptions) {
                is IncrementalCompilationOptions -> {
                    val cpChanges = compilationOptions.classpathChanges
                    if (cpChanges is ClasspathChanges.ClasspathSnapshotEnabled) {
                        total += cpChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.size
                        if (cpChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.exists()){
                            total += 1
                        }
                    }
                    val srcChanges = compilationOptions.sourceChanges
                    if (srcChanges is SourcesChanges.Known) {
                        total += srcChanges.modifiedFiles.size
                    }
                }
                is CompilationOptions -> {
                    total += allArtifacts.size
                }
            }
            return total
        }

        coroutineScope {
            // start consuming response
            val responseJob = launch(Dispatchers.IO) {
                clientImpl.compile(requestChannel.receiveAsFlow()).collect { responseChannel.send(it) }
                // when the server stops closes connection, we can close our channels
                responseChannel.close()
                requestChannel.close()
            }

            // responding on server replies
            launch {
                responseChannel.consumeAsFlow().collect {
                    when (it) {
                        is FileTransferReply -> {
                            if (!it.isPresent) {
                                launch(Dispatchers.IO) {
                                    val file = File(it.filePath)
                                    if (Files.isDirectory(file.toPath())) {
                                        createTarArchiveStream(file).use { tarStream ->
                                            fileChunkStrategy
                                                .chunk(
                                                    tarStream,
                                                    true,
                                                    it.artifactTypes,
                                                    it.filePath
                                                ).collect { chunk ->
                                                    requestChannel.send(chunk)
                                                }
                                        }
                                    } else {
                                        fileChunkStrategy.chunk(file, isDirectory = false, it.artifactTypes, it.filePath)
                                            .collect { chunk ->
                                                requestChannel.send(chunk)
                                            }
                                    }
                                }
                            }
                        }
                        is FileChunk -> {
                            launch(Dispatchers.IO) {
                                fileChunks.getOrPut(it.filePath) { mutableListOf() }.add(it)
                                if (it.isLast) {
                                    val allFileChunks = fileChunks.getOrDefault(it.filePath, listOf())
                                    val clientPath = Paths.get(it.filePath)
                                    fileChunkStrategy.reconstruct(
                                        allFileChunks,
                                        clientPath.parent,
                                        clientPath.name
                                    )
                                }
                            }
                        }
                        is MissingArtifactsRequest -> {
                            requestTransferOfArtifacts(it.missingArtifacts, requestChannel)
                        }
                        is CompilationResult -> {
                            compilationResult = it
                            debug("COMPILATION RESULT: $it")
                        }
                        is CompilerMessage -> {
                            debug("COMPILER MESSAGE: $it")
                        }
                    }
                }
            }

            // sending data to the server
            launch {
                // as a first step we want to send compilation metadata
                requestChannel.send(
                    CompilationMetadata(
                        projectName,
                        calculateTotalFiles(),
                        compilerArguments,
                        compilationOptions
                    )
                )

                when (compilationOptions) {
                    is IncrementalCompilationOptions -> {
                        val srcChanges = compilationOptions.sourceChanges
                        if (srcChanges is SourcesChanges.Known) {
                            sendSourceChangesDirectlyWithoutRequest(srcChanges, requestChannel)
                        } else {
                            requestTransferOfArtifacts(allArtifacts, requestChannel)
                        }

                        val cpChanges = compilationOptions.classpathChanges
                        if (cpChanges is ClasspathChanges.ClasspathSnapshotEnabled) {
                            requestTransferOfClasspathSnapshotFiles(cpChanges.classpathSnapshotFiles, requestChannel)
                        }
                    }
                    is CompilationOptions -> {
                        requestTransferOfArtifacts(allArtifacts, requestChannel)
                    }
                }
            }
            responseJob.join()
        }
        return compilationResult ?: throw IllegalStateException("Compilation result is null")
    }

    suspend fun cleanup() {
        clientImpl.cleanup()
    }

    fun close() {
        clientImpl.close()
    }

    private fun CoroutineScope.requestTransferOfClasspathSnapshotFiles(
        classpathSnapshotFiles: ClasspathSnapshotFiles,
        requestChannel: Channel<CompileRequest>
    ) {
        requestTransferOfArtifacts(
            classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.map { Artifact(it, setOf(ArtifactType.CLASSPATH_ENTRY_SNAPSHOT)) }.toSet(),
            requestChannel
        )
        classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.takeIf { it.exists() }?.let {
            requestTransferOfArtifacts(setOf(Artifact(it, setOf(ArtifactType.SHRUNK_CLASSPATH_SNAPSHOT))), requestChannel)
        }
    }

    private fun CoroutineScope.sendSourceChangesDirectlyWithoutRequest(
        sourcesChanges: SourcesChanges.Known,
        requestChannel: Channel<CompileRequest>
    ) {
        sourcesChanges.modifiedFiles.forEach {
            launch(Dispatchers.IO) {
                fileChunkStrategy.chunk(
                    it,
                    isDirectory = Files.isDirectory(it.toPath()),
                    setOf(ArtifactType.SOURCE),
                    it.absolutePath
                ).collect { chunk ->
                    requestChannel.send(chunk)
                }
            }
        }
    }

    private fun CoroutineScope.requestTransferOfArtifacts(
        artifacts: Set<Artifact>,
        requestChannel: Channel<CompileRequest>
    ) {
        artifacts.forEach { artifact ->
            launch(Dispatchers.IO) {
                requestChannel.send(FileTransferRequest(artifact.file.path, computeSha256(artifact.file), artifact.types))
            }
        }
    }

}

// this main function just serves for testing of incremental compilation
@OptIn(ExperimentalBuildToolsApi::class)
suspend fun main() {

    val client = RemoteCompilationClient.getClient(RemoteCompilationServiceImplType.GRPC, "localhost", 8000)

    // taken from ktor-client-android task
    val compilerArguments =
        "-Xallow-no-source-files -classpath /Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-core/build/libs/ktor-client-core-jvm-3.4.0-SNAPSHOT.jar:/Users/michal.svec/Desktop/ktor/ktor-http/ktor-http-cio/build/libs/ktor-http-cio-jvm-3.4.0-SNAPSHOT.jar:/Users/michal.svec/Desktop/ktor/ktor-shared/ktor-websocket-serialization/build/libs/ktor-websocket-serialization-jvm-3.4.0-SNAPSHOT.jar:/Users/michal.svec/Desktop/ktor/ktor-shared/ktor-serialization/build/libs/ktor-serialization-jvm-3.4.0-SNAPSHOT.jar:/Users/michal.svec/Desktop/ktor/ktor-shared/ktor-websockets/build/libs/ktor-websockets-jvm-3.4.0-SNAPSHOT.jar:/Users/michal.svec/Desktop/ktor/ktor-http/build/libs/ktor-http-jvm-3.4.0-SNAPSHOT.jar:/Users/michal.svec/Desktop/ktor/ktor-shared/ktor-events/build/libs/ktor-events-jvm-3.4.0-SNAPSHOT.jar:/Users/michal.svec/Desktop/ktor/ktor-shared/ktor-sse/build/libs/ktor-sse-jvm-3.4.0-SNAPSHOT.jar:/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.10.2/4a9f78ef49483748e2c129f3d124b8fa249dafbf/kotlinx-coroutines-core-jvm-1.10.2.jar:/Users/michal.svec/Desktop/ktor/ktor-network/build/libs/ktor-network-jvm-3.4.0-SNAPSHOT.jar:/Users/michal.svec/Desktop/ktor/ktor-utils/build/libs/ktor-utils-jvm-3.4.0-SNAPSHOT.jar:/Users/michal.svec/Desktop/ktor/ktor-io/build/libs/ktor-io-jvm-3.4.0-SNAPSHOT.jar:/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-serialization-core-jvm/1.9.0/91448df39c558f7c6147b8bd8db01debe16e0cc1/kotlinx-serialization-core-jvm-1.9.0.jar:/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-io-core-jvm/0.8.0/358a9f2ba2dc81c5dc84c3d1853f6e5efba63be1/kotlinx-io-core-jvm-0.8.0.jar:/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-io-bytestring-jvm/0.8.0/89c5399596250e71f2bba6d2415972a078a525c7/kotlinx-io-bytestring-jvm-0.8.0.jar:/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.2.20/5380b19fa1924399b62ce3a1faffebb2b4f82272/kotlin-stdlib-2.2.20.jar:/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/2.0.17/d9e58ac9c7779ba3bf8142aff6c830617a7fe60f/slf4j-api-2.0.17.jar:/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/23.0.0/8cc20c07506ec18e0834947b84a864bfc094484e/annotations-23.0.0.jar:/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/atomicfu-jvm/0.29.0/fad29f53ad0c239ba525b637a152bfcaa8631f0f/atomicfu-jvm-0.29.0.jar -d /Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/build/classes/atomicfu-orig/jvm/main -jdk-home /Users/michal.svec/.gradle/jdks/temurin-8-x86_64-os_x.2/jdk8u452-b09/Contents/Home -jvm-target 1.8 -module-name ktor-client-android -no-reflect -no-stdlib -api-version 2.2 -Xexplicit-api=strict -Xfragment-refines=jvmMain:jvmAndPosixMain,jvmAndPosixMain:commonMain -Xfragment-sources=jvmMain:/Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/jvm/src/io/ktor/client/engine/android/Android.kt,jvmMain:/Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/jvm/src/io/ktor/client/engine/android/AndroidURLConnectionUtils.kt,jvmMain:/Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/jvm/src/io/ktor/client/engine/android/AndroidClientEngine.kt,jvmMain:/Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/jvm/src/io/ktor/client/engine/android/AndroidEngineConfig.kt -Xfragments=jvmMain,jvmAndPosixMain,commonMain -language-version 2.2 -Xmulti-platform -Xplugin=/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-compiler-embeddable/2.2.20/d13136286cbdf06f0926ec858125acc09e76cd72/kotlin-scripting-compiler-embeddable-2.2.20.jar,/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-compiler-impl-embeddable/2.2.20/816d67678440bd87bc8d1392f97195ec338fdaed/kotlin-scripting-compiler-impl-embeddable-2.2.20.jar,/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-jvm/2.2.20/785d5ffea49d9dc289b200d0ff3e0ce6e9ba2395/kotlin-scripting-jvm-2.2.20.jar,/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-common/2.2.20/79e0c2cc15b84711ac1de87e6a257acd151cb999/kotlin-scripting-common-2.2.20.jar,/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.2.20/5380b19fa1924399b62ce3a1faffebb2b4f82272/kotlin-stdlib-2.2.20.jar,/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-script-runtime/2.2.20/4c679cbeac0bb583b75c3a080013da8eaf240807/kotlin-script-runtime-2.2.20.jar,/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar -progressive -Xreport-perf -Xexpect-actual-classes -Xreport-all-warnings -Xrender-internal-diagnostic-names -Xexplicit-api=strict /Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/jvm/src/io/ktor/client/engine/android/Android.kt /Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/jvm/src/io/ktor/client/engine/android/AndroidURLConnectionUtils.kt /Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/jvm/src/io/ktor/client/engine/android/AndroidClientEngine.kt /Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/jvm/src/io/ktor/client/engine/android/AndroidEngineConfig.kt"
            .split(" ")
            .map { it.trim() }

    val outputFiles = listOf(
        "/Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/build/classes/atomicfu-orig/jvm/main",
        "/Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/build/kotlin/compileKotlinJvm/cacheable",
        "/Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/build/kotlin/compileKotlinJvm/local-state"
    ).map { File(it) }

    val workingDir = File("/Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/build/kotlin/compileKotlinJvm/cacheable")
    val shrunkSnapshotDir =
        File("/Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/build/kotlin/compileKotlinJvm/classpath-snapshot")

    // Ensure required directories exist on the first run
    Files.createDirectories(shrunkSnapshotDir.toPath())
    outputFiles.forEach { Files.createDirectories(it.toPath()) }
    Files.createDirectories(workingDir.toPath())

    val parsedArgs = parseCommandLineArguments<K2JVMCompilerArguments>(compilerArguments)
    val classpathSnapshots = parsedArgs.classpathAsList.map {
        val snapshot = calculateClasspathSnapshot(it, ClassSnapshotGranularity.CLASS_LEVEL, true)
        val snapshotOutputFile = CLIENT_TMP_DIR.resolve(it.name.replace('.', '_') + "-snapshot.bin").toFile()
        snapshot.saveSnapshot(snapshotOutputFile)
        snapshotOutputFile
    }

    val classpathSnapshotFiles = ClasspathSnapshotFiles(
        classpathSnapshots,
        shrunkSnapshotDir
    )

    val incrementalSourceChanges = SourcesChanges.Known(
        modifiedFiles = listOf(File("/Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/jvm/src/io/ktor/client/engine/android/Android.kt")),
        removedFiles = listOf(),
    )

    val incrementalClasspathChanges = ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(
        classpathSnapshotFiles
    )

    val sourceChanges = SourcesChanges.Unknown

    val classpathChanges = ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun(
        classpathSnapshotFiles
    )

    client.compile(
        "ktor-client-android",
        compilerArguments,
        IncrementalCompilationOptions(
            sourceChanges = sourceChanges,
            classpathChanges = classpathChanges,
            workingDir = workingDir,
            compilerMode = CompilerMode.INCREMENTAL_COMPILER,
            targetPlatform = CompileService.TargetPlatform.JVM,
            outputFiles = outputFiles,
            reportCategories = emptyArray(),
            reportSeverity = 0,
            requestedCompilationResults = arrayOf(0, 2, 3),
            useJvmFirRunner = false,
            rootProjectDir = null,
            buildDir = null,
            multiModuleICSettings = MultiModuleICSettings(
                useModuleDetection = false,
                buildHistoryFile = File("/Users/michal.svec/Desktop/ktor/ktor-client/ktor-client-android/build/kotlin/compileKotlinJvm/local-state/build-history.bin")
            ),
            icFeatures = IncrementalCompilationFeatures(
                usePreciseJavaTracking = true,
                withAbiSnapshot = false,
                preciseCompilationResultsBackup = true,
                keepIncrementalCompilationCachesInMemory = true,
                enableUnsafeIncrementalCompilationForMultiplatform = false,
                enableMonotonousIncrementalCompileSetExpansion = true
            )
        )
    )

    classpathSnapshots.forEach { it.delete() }
}