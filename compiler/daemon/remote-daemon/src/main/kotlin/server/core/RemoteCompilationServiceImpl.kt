/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.core

import common.CompilerUtils
import common.FileChunkingStrategy
import common.FixedSizeChunkingStrategy
import common.RemoteCompilationService
import common.createTarArchiveStream
import common.SERVER_TMP_CACHE_DIR
import common.cleanCompilationResultPath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import model.CompilationMetadata
import model.CompilationResult
import model.CompilationResultSource
import model.CompileRequest
import model.CompileResponse
import model.CompilerMessage
import model.FileChunk
import model.FileTransferReply
import model.FileTransferRequest
import model.ArtifactType
import model.MissingFilesRequest
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompilationResults
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.daemon.common.JpsCompilerServicesFacade
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporter
import org.jetbrains.kotlin.daemon.report.getBuildReporter
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.classpathAsList
import server.grpc.AuthServerInterceptor
import java.io.File
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RemoteCompilationServiceImpl(
    val cacheHandler: CacheHandler,
    val fileChunkingStrategy: FileChunkingStrategy,
    val compilerService: InProcessCompilerService,
    val logging: Boolean = false,
) : RemoteCompilationService {

    val fileChunkStrategy = FixedSizeChunkingStrategy()
    lateinit var workspaceManager: WorkspaceManager

    fun debug(text: String) {
        if (logging) {
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            println("[${LocalDateTime.now().format(formatter)}] [thread=${Thread.currentThread().name}] DEBUG SERVER: $text")
        }
    }

    override suspend fun cleanup() {
        WorkspaceManager.cleanup()
        cacheHandler.cleanup()
    }

    override fun compile(compileRequests: Flow<CompileRequest>): Flow<CompileResponse> {
        return channelFlow {

            var allFilesReady = CompletableDeferred<Unit>()
            val pendingFiles = AtomicInteger(0)

            // <PathFromClientComputer, FileFromUserWorkspace>
            val sourceFiles = ConcurrentHashMap<Path, File>()
            val dependencyFiles = ConcurrentHashMap<Path, File>()
            val compilerPluginFiles = ConcurrentHashMap<Path, File>()
            val classpathEntrySnapshotFiles = ConcurrentHashMap<Path, File>()

            val workspaceFileLockMap = ConcurrentHashMap<Path, Mutex>()
            val cacheFileLockMap = ConcurrentHashMap<Path, Mutex>()

            val fileChunks = mutableMapOf<String, MutableList<FileChunk>>()

            fun signalFileAvailability(clientPath: Path, file: File, artifactType: ArtifactType) {
                when (artifactType) {
                    ArtifactType.SOURCE -> sourceFiles[clientPath] = file
                    ArtifactType.DEPENDENCY -> dependencyFiles[clientPath] = file
                    ArtifactType.COMPILER_PLUGIN -> compilerPluginFiles[clientPath] = file
                    ArtifactType.CLASSPATH_ENTRY_SNAPSHOT -> classpathEntrySnapshotFiles[clientPath] = file
                    ArtifactType.RESULT -> debug("Received illegal file type: $artifactType")
                    ArtifactType.IC_CACHE -> debug("Received illegal file type: $artifactType")
                }
                if (pendingFiles.decrementAndGet() == 0) {
                    allFilesReady.complete(Unit)
                }
            }

            lateinit var compilationMetadata: CompilationMetadata

            // TODO: this general impl is dependent on GRPC auth mechanism, that's wrong
            val userId = AuthServerInterceptor.USER_ID_CONTEXT_KEY.get() ?: "3489439j"

            // here we consume request stream
            launch {
                compileRequests.collect {
                    when (it) {
                        is CompilationMetadata -> {
                            compilationMetadata = it
                            pendingFiles.set(calculateTotalFiles(compilationMetadata))
                            workspaceManager = WorkspaceManager(userId, compilationMetadata.projectName)
                        }
                        is FileTransferRequest -> {
                            launch {
                                val compileResponse = when (cacheHandler.isFileCached(it.fileFingerprint)) {
                                    true -> {
                                        debug("file ${it.filePath} is available in cache")
                                        val cachedFile = cacheHandler.getFile(it.fileFingerprint)
                                        val projectFile = workspaceManager.copyFileToProject(
                                            cachedFilePath = cachedFile.absolutePath,
                                            clientFilePath = it.filePath,
                                            userId,
                                            compilationMetadata.projectName,
                                            workspaceFileLockMap
                                        )
                                        signalFileAvailability(
                                            Paths.get(it.filePath).toAbsolutePath().normalize(),
                                            projectFile,
                                            it.artifactType
                                        )
                                        FileTransferReply(
                                            it.filePath,
                                            isPresent = true,
                                            it.artifactType
                                        )
                                    }
                                    false -> {
                                        debug("file ${it.filePath} is not available in cache")
                                        FileTransferReply(
                                            it.filePath,
                                            isPresent = false,
                                            it.artifactType
                                        )
                                    }
                                }
                                send(compileResponse)
                            }
                        }
                        is FileChunk -> {
                            fileChunks.getOrPut(it.filePath) { mutableListOf() }.add(it)
                            if (it.isLast) {
                                launch {
                                    val allFileChunks = fileChunks.getOrDefault(it.filePath, listOf())
                                    val reconstructedFile = fileChunkingStrategy.reconstruct(allFileChunks, SERVER_TMP_CACHE_DIR)
                                    val cachedFile =
                                        cacheHandler.cacheFile(
                                            reconstructedFile,
                                            it.artifactType,
                                            deleteOriginalFile = true,
                                            cacheFileLockMap
                                        )
                                    val projectFile = workspaceManager.copyFileToProject(
                                        cachedFile.absolutePath,
                                        it.filePath,
                                        userId,
                                        compilationMetadata.projectName,
                                        workspaceFileLockMap
                                    )
                                    debug("Reconstructed ${if (reconstructedFile.isFile) "file" else "directory"}, artifactType=${it.artifactType}, clientPath=${it.filePath}")
                                    signalFileAvailability(
                                        Paths.get(it.filePath).toAbsolutePath().normalize(),
                                        projectFile,
                                        it.artifactType
                                    )
                                }
                            }
                        }
                    }
                }
            }


            // here we collect source files, if all source files are available, we are ready to compile
            launch {
                allFilesReady.join()

                val remoteCompilationOptions = CompilerUtils.getRemoteCompilationOptions(
                    compilationMetadata.compilationOptions as IncrementalCompilationOptions,
                    workspaceManager,
                    sourceFiles,
                    classpathEntrySnapshotFiles,
                )

                val remoteCompilerArguments = CompilerUtils.getRemoteCompilerArguments(
                    compilationMetadata.compilerArguments,
                    workspaceManager,
                    sourceFiles,
                    dependencyFiles,
                    compilerPluginFiles
                )

                if (remoteCompilationOptions is IncrementalCompilationOptions) {
                    val numberOfMissingFiles = requestMissingFilesFromClient(remoteCompilerArguments, this@channelFlow)
                    if (numberOfMissingFiles > 0) {
                        allFilesReady = CompletableDeferred()
                        pendingFiles.set(numberOfMissingFiles)
                        allFilesReady.join()
                    }
                }

                val (isCompilationResultCached, inputFingerprint) = cacheHandler.isCompilationResultCached(remoteCompilerArguments)
                val (outputDir, compilationResult) = if (isCompilationResultCached) {
                    debug("[SERVER COMPILATION] Compilation result is cached, fingerprint: $inputFingerprint")
                    cacheHandler.getCompilationResultDirectory(inputFingerprint) to CompilationResult(
                        0,
                        CompilationResultSource.CACHE
                    )
                } else {
                    debug("[SERVER COMPILATION] Compilation result is NOT cached, fingerprint: $inputFingerprint")
                    val (outputDir, compilationResult) = doCompilation(
                        remoteCompilationOptions,
                        remoteCompilerArguments,
                        this@channelFlow::trySend// TODO: double check trySend
                    )
                    cacheHandler.cacheFile(
                        outputDir,
                        ArtifactType.RESULT,
                        deleteOriginalFile = false,
                        cacheFileLockMap,
                        remoteCompilerArguments
                    )
                    outputDir to compilationResult
                }
                send(compilationResult)
                sendCompiledFilesToClient(outputDir, this@channelFlow)
                if (remoteCompilationOptions is IncrementalCompilationOptions) {
                    val icCacheDir = CompilerUtils.getICCacheFolder(remoteCompilerArguments)
                    println("obtained ic cache dir: $icCacheDir")
                    println("obtained destination dit: ${CompilerUtils.getOutputDir(remoteCompilerArguments)}")
                    sendICCacheToClient(icCacheDir, this@channelFlow)
                }
                this@channelFlow.close()
            }
        }
    }

    private fun calculateTotalFiles(compilationMetadata: CompilationMetadata): Int {
        var total = 0
        when (val co = compilationMetadata.compilationOptions) {
            is IncrementalCompilationOptions -> {
                val cpChanges = co.classpathChanges
                if (cpChanges is ClasspathChanges.ClasspathSnapshotEnabled) {
                    total += cpChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.size
                }
                val srcChanges = co.sourceChanges
                if (srcChanges is SourcesChanges.Known) {
                    total += srcChanges.modifiedFiles.size
                }
            }
            is CompilationOptions -> {
                total += compilationMetadata.sourceFilesCount + compilationMetadata.dependencyFilesCount + compilationMetadata.compilerPluginFilesCount
            }
        }
        return total
    }

    private fun doCompilation(
        remoteCompilationOptions: CompilationOptions,
        remoteCompilerArguments: K2JVMCompilerArguments,
        send: (CompileResponse) -> Unit
    ): Pair<File, CompilationResult> {
        val remoteMessageCollector = RemoteMessageCollector(object : OnReport {
            override fun onReport(msg: CompilerMessage) {
                send(msg)
            }
        })

        val outputsCollector = { x: File, y: List<File> -> println("$x $y") }
        val servicesFacade =
            BasicCompilerServicesWithResultsFacadeServer(remoteMessageCollector, outputsCollector)

        debug("[SERVER COMPILATION] remote compiler arguments are:")
        debug(ArgumentUtils.convertArgumentsToStringList(remoteCompilerArguments).toString())
        val exitCode = compilerService.compileImpl(
            compilerArguments = ArgumentUtils.convertArgumentsToStringList(remoteCompilerArguments)
                .toTypedArray(),
            compilationOptions = remoteCompilationOptions,
            servicesFacade = servicesFacade,
            compilationResults = object : CompilationResults {
                override fun add(compilationResultCategory: Int, value: Serializable) {}
            },
            hasIncrementalCaches = JpsCompilerServicesFacade::hasIncrementalCaches,
            createMessageCollector = { facade, options ->
                remoteMessageCollector
            },
            createReporter = ::DaemonMessageReporter,
            createServices = { facade, eventManager ->
                Services.EMPTY
            },
            getICReporter = { a, b, c ->
                getBuildReporter(a, b!!, c)
            }
        )
        debug("[SERVER COMPILATION] compilation finished and exit code is $exitCode")
        val outputDir = CompilerUtils.getOutputDir(remoteCompilerArguments)
        return outputDir to CompilationResult(exitCode, CompilationResultSource.COMPILER)
    }

    fun getICCacheFolder(remoteICO: IncrementalCompilationOptions): File? {
        // TODO: come up with better and more reliable way of determining cache output folder
        val icCandidateFolder =
            remoteICO.outputFiles?.firstOrNull { it.toPath().toString().contains("compileKotlin") }

        return icCandidateFolder?.let { f ->
            var dir = if (f.isDirectory) f else f.parentFile
            while (dir != null && dir.name != "compileKotlin") {
                dir = dir.parentFile
            }
            dir
        }
    }

    private suspend fun sendCompiledFilesToClient(outputDir: File, outputChannel: ProducerScope<CompileResponse>){
        outputDir.walkTopDown()
            .filter { !Files.isDirectory(it.toPath()) }
            .forEach { file ->
                val clientCleanedPath = cleanCompilationResultPath(file.path)
                fileChunkStrategy.chunk(file, isDirectory = false, ArtifactType.RESULT, file.path).collect { chunk ->
                    outputChannel.send(
                        FileChunk(
                            clientCleanedPath,
                            ArtifactType.RESULT,
                            chunk.content,
                            chunk.isDirectory,
                            chunk.isLast,
                        )
                    )
                }
            }
    }

    suspend fun sendICCacheToClient(icCacheDir: File, outputChannel: ProducerScope<CompileResponse>) {
        createTarArchiveStream(icCacheDir).use { tarStream ->
            fileChunkStrategy
                .chunk(
                    tarStream,
                    true,
                    ArtifactType.IC_CACHE,
                    workspaceManager.removeWorkspaceProjectPrefix(icCacheDir.toPath()).toString()
                ).collect { chunk ->
                    outputChannel.send(chunk)
                }
        }
    }

    suspend fun requestMissingFilesFromClient(args: K2JVMCompilerArguments, outputChannel: ProducerScope<CompileResponse>): Int{
        println(ArgumentUtils.convertArgumentsToStringList(args).toString())
        val missingDependencies = getMissingFiles(CompilerUtils.getDependencyFiles(args))
        val missingSources = getMissingFiles(CompilerUtils.getSourceFiles(args))
        val missingPluginFiles = getMissingFiles(CompilerUtils.getXPluginFiles(args))

        if (missingDependencies.isNotEmpty()) {
            outputChannel.send(
                MissingFilesRequest(
                    missingDependencies, ArtifactType.DEPENDENCY
                )
            )
        }
        if (missingSources.isNotEmpty()) {
            outputChannel.send(
                MissingFilesRequest(
                    missingSources, ArtifactType.SOURCE
                )
            )
        }
        if (missingPluginFiles.isNotEmpty()) {
            outputChannel.send(
                MissingFilesRequest(
                    missingPluginFiles, ArtifactType.COMPILER_PLUGIN
                )
            )
        }
        return missingDependencies.size + missingSources.size + missingPluginFiles.size
    }

    private fun getMissingFiles(files: List<File>): List<String> {
        return files.filter { file -> !file.exists() }.map { it.absolutePath }
    }


}