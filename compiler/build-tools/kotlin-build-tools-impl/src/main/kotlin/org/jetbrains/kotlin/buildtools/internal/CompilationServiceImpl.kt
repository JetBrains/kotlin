/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.configureDaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.filterExtractProps
import org.jetbrains.kotlin.incremental.IncrementalCompilationFeatures
import org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunner
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.jetbrains.kotlin.incremental.extractKotlinSourcesFromFreeCompilerArguments
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.storage.FileLocations
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.reporter
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsFromClasspathDiscoverySource
import java.io.File
import java.net.URLClassLoader
import java.rmi.RemoteException
import java.util.concurrent.ConcurrentHashMap
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

private val ExitCode.asCompilationResult
    get() = when (this) {
        ExitCode.OK -> CompilationResult.COMPILATION_SUCCESS
        ExitCode.COMPILATION_ERROR -> CompilationResult.COMPILATION_ERROR
        ExitCode.INTERNAL_ERROR -> CompilationResult.COMPILER_INTERNAL_ERROR
        ExitCode.OOM_ERROR -> CompilationResult.COMPILATION_OOM_ERROR
        else -> error("Unexpected exit code: $this")
    }

private fun getCurrentClasspath() = (CompilationServiceImpl::class.java.classLoader as URLClassLoader).urLs.map { File(it.file) }

internal object CompilationServiceImpl : CompilationService {
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File> = ConcurrentHashMap()

    override fun calculateClasspathSnapshot(classpathEntry: File, granularity: ClassSnapshotGranularity) =
        ClasspathEntrySnapshotImpl(ClasspathEntrySnapshotter.snapshot(classpathEntry, granularity, DoNothingBuildMetricsReporter))

    override fun makeCompilerExecutionStrategyConfiguration() = CompilerExecutionStrategyConfigurationImpl()

    override fun makeJvmCompilationConfiguration() = JvmCompilationConfigurationImpl()

    override fun compileJvm(
        projectId: ProjectId,
        strategyConfig: CompilerExecutionStrategyConfiguration,
        compilationConfig: JvmCompilationConfiguration,
        sources: List<File>,
        arguments: List<String>,
    ): CompilationResult {
        check(strategyConfig is CompilerExecutionStrategyConfigurationImpl) {
            "Initial strategy configuration object must be acquired from the `makeCompilerExecutionStrategyConfiguration` method."
        }
        check(compilationConfig is JvmCompilationConfigurationImpl) {
            "Initial JVM compilation configuration object must be acquired from the `makeJvmCompilationConfiguration` method."
        }
        val loggerAdapter = KotlinLoggerMessageCollectorAdapter(compilationConfig.logger)
        return when (val selectedStrategy = strategyConfig.selectedStrategy) {
            is CompilerExecutionStrategy.InProcess -> compileInProcess(loggerAdapter, compilationConfig, sources, arguments)
            is CompilerExecutionStrategy.Daemon -> compileWithinDaemon(
                projectId,
                loggerAdapter,
                selectedStrategy,
                compilationConfig,
                sources,
                arguments
            )
        }
    }

    override fun finishProjectCompilation(projectId: ProjectId) {
        clearJarCaches()
        val file = buildIdToSessionFlagFile.remove(projectId) ?: return
        file.delete()
    }

    private fun clearJarCaches() {
        ZipHandler.clearFileAccessorCache()
        KotlinCoreEnvironment.applicationEnvironment?.apply {
            (jarFileSystem as? CoreJarFileSystem)?.clearHandlersCache()
            (jrtFileSystem as? CoreJrtFileSystem)?.clearRoots()
            idleCleanup()
        }
    }

    override fun getCustomKotlinScriptFilenameExtensions(classpath: List<File>): Collection<String> {
        val definitions = ScriptDefinitionsFromClasspathDiscoverySource(
            classpath,
            defaultJvmScriptingHostConfiguration,
            PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false).reporter
        ).definitions

        return definitions.mapTo(arrayListOf()) { it.fileExtension }
    }

    private fun compileInProcess(
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        compilationConfiguration: JvmCompilationConfigurationImpl,
        sources: List<File>,
        arguments: List<String>,
    ): CompilationResult {
        val compiler = K2JVMCompiler()
        val parsedArguments = compiler.createArguments()
        parseCommandLineArguments(arguments, parsedArguments)
        validateArguments(parsedArguments.errors)?.let {
            throw CompilerArgumentsParseException(it)
        }
        loggerAdapter.report(CompilerMessageSeverity.INFO, arguments.toString())
        val aggregatedIcConfiguration = compilationConfiguration.aggregatedIcConfiguration
        return when (val options = aggregatedIcConfiguration?.options) {
            is ClasspathSnapshotBasedIncrementalJvmCompilationConfigurationImpl -> {
                val kotlinFilenameExtensions =
                    (DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + compilationConfiguration.kotlinScriptFilenameExtensions)

                @Suppress("DEPRECATION") // TODO: get rid of that parsing KT-62759
                val kotlinSources = extractKotlinSourcesFromFreeCompilerArguments(parsedArguments, kotlinFilenameExtensions) + sources

                @Suppress("UNCHECKED_CAST")
                val classpathChanges =
                    (aggregatedIcConfiguration as AggregatedIcConfiguration<ClasspathSnapshotBasedIncrementalCompilationApproachParameters>).classpathChanges
                val buildReporter = BuildReporter(
                    icReporter = BuildToolsApiBuildICReporter(loggerAdapter.kotlinLogger, options.rootProjectDir),
                    buildMetricsReporter = DoNothingBuildMetricsReporter
                )
                val incrementalCompiler = IncrementalJvmCompilerRunner(
                    aggregatedIcConfiguration.workingDir,
                    buildReporter,
                    buildHistoryFile = null,
                    modulesApiHistory = EmptyModulesApiHistory,
                    usePreciseJavaTracking = options.preciseJavaTrackingEnabled,
                    outputDirs = options.outputDirs,
                    kotlinSourceFilesExtensions = kotlinFilenameExtensions,
                    classpathChanges = classpathChanges,
                    icFeatures = options.extractIncrementalCompilationFeatures(),
                )
                val rootProjectDir = options.rootProjectDir
                val buildDir = options.buildDir
                parsedArguments.incrementalCompilation = true
                incrementalCompiler.compile(
                    kotlinSources, parsedArguments, loggerAdapter, aggregatedIcConfiguration.sourcesChanges.asChangedFiles,
                    fileLocations = if (rootProjectDir != null && buildDir != null) {
                        FileLocations(rootProjectDir, buildDir)
                    } else null
                ).asCompilationResult
            }
            else -> {
                parsedArguments.freeArgs += sources.map { it.absolutePath }
                compiler.exec(loggerAdapter, Services.EMPTY, parsedArguments).asCompilationResult
            }
        }
    }

    private fun compileWithinDaemon(
        projectId: ProjectId,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        daemonConfiguration: CompilerExecutionStrategy.Daemon,
        compilationConfiguration: JvmCompilationConfigurationImpl,
        sources: List<File>,
        arguments: List<String>,
    ): CompilationResult {
        val compilerId = CompilerId.makeCompilerId(getCurrentClasspath())
        val sessionIsAliveFlagFile = buildIdToSessionFlagFile.computeIfAbsent(projectId) {
            createSessionIsAliveFlagFile()
        }

        val jvmOptions = configureDaemonJVMOptions(
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = false,
            inheritAdditionalProperties = true
        ).also { opts ->
            if (daemonConfiguration.jvmArguments.isNotEmpty()) {
                opts.jvmParams.addAll(
                    daemonConfiguration.jvmArguments.filterExtractProps(opts.mappers, "", opts.restMapper)
                )
            }
        }

        val (daemon, sessionId) = KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId,
            clientIsAliveFile,
            sessionIsAliveFlagFile,
            loggerAdapter,
            false,
            daemonJVMOptions = jvmOptions
        ) ?: error("Can't get connection")
        val daemonCompileOptions = compilationConfiguration.asDaemonCompilationOptions
        val exitCode = daemon.compile(
            sessionId,
            arguments.toTypedArray() + sources.map { it.absolutePath }, // TODO: pass the sources explicitly KT-62759
            daemonCompileOptions,
            BasicCompilerServicesWithResultsFacadeServer(loggerAdapter),
            DaemonCompilationResults(
                loggerAdapter.kotlinLogger,
                compilationConfiguration.aggregatedIcConfiguration?.options?.rootProjectDir
            )
        ).get()

        try {
            daemon.releaseCompileSession(sessionId)
        } catch (e: RemoteException) {
            loggerAdapter.kotlinLogger.warn("Unable to release compile session, maybe daemon is already down: $e")
        }

        return (ExitCode.entries.find { it.code == exitCode } ?: if (exitCode == 0) {
            ExitCode.OK
        } else {
            ExitCode.COMPILATION_ERROR
        }).asCompilationResult
    }

    override fun getCompilerVersion(): String = KotlinCompilerVersion.VERSION
}

internal class CompilationServiceProxy : CompilationService by CompilationServiceImpl