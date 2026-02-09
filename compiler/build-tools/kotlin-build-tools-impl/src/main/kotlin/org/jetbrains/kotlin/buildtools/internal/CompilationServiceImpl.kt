/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.buildtools.internal

import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.jetbrains.kotlin.incremental.storage.FileLocations
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.reporter
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsFromClasspathDiscoverySource
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.rmi.RemoteException
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.toPath
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

internal val ExitCode.asCompilationResult
    get() = when (this) {
        ExitCode.OK -> CompilationResult.COMPILATION_SUCCESS
        ExitCode.COMPILATION_ERROR -> CompilationResult.COMPILATION_ERROR
        ExitCode.INTERNAL_ERROR -> CompilationResult.COMPILER_INTERNAL_ERROR
        ExitCode.OOM_ERROR -> CompilationResult.COMPILATION_OOM_ERROR
        else -> error("Unexpected exit code: $this")
    }

private fun getCurrentClasspath() = (CompilationServiceImpl::class.java.classLoader as URLClassLoader).urLs.map { transformUrlToFile(it) }

/**
 * Transforms a given URL to a File object with proper handling of escapable characters like whitespace, hashbang.
 *
 * Example: URL containing "some%20path" should be transformed to a File object pointing to "some path"
 */
internal fun transformUrlToFile(url: URL) = url.toURI().toPath().toFile()

internal object CompilationServiceImpl : CompilationService {
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File> = ConcurrentHashMap()

    override fun calculateClasspathSnapshot(
        classpathEntry: File,
        granularity: ClassSnapshotGranularity,
        parseInlinedLocalClasses: Boolean
    ): ClasspathEntrySnapshot {
        return ClasspathEntrySnapshotImpl(
            ClasspathEntrySnapshotter.snapshot(
                classpathEntry,
                ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses),
                DoNothingBuildMetricsReporter
            )
        )
    }

    override fun calculateClasspathSnapshot(
        classpathEntry: File,
        granularity: ClassSnapshotGranularity
    ): ClasspathEntrySnapshot {
        return calculateClasspathSnapshot(classpathEntry, granularity, parseInlinedLocalClasses = true)
    }

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
        val loggerAdapter = KotlinLoggerMessageCollectorAdapter(compilationConfig.logger, DefaultCompilerMessageRenderer)
        val kotlinFilenameExtensions =
            (DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + compilationConfig.kotlinScriptFilenameExtensions)
        val (filteredSources, unknownSources) = sources.partition { it.isJavaFile() || it.isKotlinFile(kotlinFilenameExtensions) }
        if (unknownSources.isNotEmpty()) {
            compilationConfig.logger.warn("Sources with unknown extensions were passed, they will be skipped: ${unknownSources.joinToString()}")
        }
        return when (val selectedStrategy = strategyConfig.selectedStrategy) {
            is CompilerExecutionStrategy.InProcess -> compileInProcess(
                loggerAdapter,
                compilationConfig,
                kotlinFilenameExtensions,
                filteredSources,
                arguments,
            )
            is CompilerExecutionStrategy.Daemon -> compileWithinDaemon(
                projectId,
                loggerAdapter,
                selectedStrategy,
                compilationConfig,
                filteredSources,
                arguments,
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
        @OptIn(K1Deprecation::class)
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
        kotlinFilenameExtensions: Set<String>,
        sources: List<File>,
        arguments: List<String>,
    ): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling using the in-process strategy")
        setupIdeaStandaloneExecution()
        val compiler = K2JVMCompiler()
        val parsedArguments = compiler.createArguments()
        parseCommandLineArguments(arguments, parsedArguments)
        validateArguments(parsedArguments.errors)?.let {
            throw CompilerArgumentsParseException(it)
        }
        val aggregatedIcConfiguration = compilationConfiguration.aggregatedIcConfiguration
        return when (val options = aggregatedIcConfiguration?.options) {
            is ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration -> {
                @Suppress("DEPRECATION") // TODO: get rid of that parsing KT-62759
                val allSources = extractKotlinSourcesFromFreeCompilerArguments(
                    parsedArguments,
                    kotlinFilenameExtensions,
                    includeJavaSources = true
                ) + sources
                val javaSources = allSources.filter { it.isJavaFile() }.map { it.absolutePath }

                @Suppress("UNCHECKED_CAST")
                val classpathChanges =
                    (aggregatedIcConfiguration as AggregatedIcConfiguration<ClasspathSnapshotBasedIncrementalCompilationApproachParameters>).classpathChanges
                val buildReporter = BuildReporter(
                    icReporter = BuildToolsApiBuildICReporter(loggerAdapter.kotlinLogger, options.rootProjectDir, null),
                    buildMetricsReporter = DoNothingBuildMetricsReporter,
                )
                val verifiedPreciseJavaTracking = parsedArguments.disablePreciseJavaTrackingIfK2(usePreciseJavaTrackingByDefault = options.preciseJavaTrackingEnabled)
                val icFeatures = options.extractIncrementalCompilationFeatures().copy(
                    usePreciseJavaTracking = verifiedPreciseJavaTracking
                )
                val incrementalCompiler = if (options.isUsingFirRunner && checkJvmFirRequirements(arguments)) {
                    IncrementalFirJvmCompilerRunner(
                        aggregatedIcConfiguration.workingDir,
                        buildReporter,
                        outputDirs = options.outputDirs,
                        classpathChanges = classpathChanges,
                        kotlinSourceFilesExtensions = kotlinFilenameExtensions,
                        icFeatures = icFeatures
                    )
                } else {
                    IncrementalJvmCompilerRunner(
                        aggregatedIcConfiguration.workingDir,
                        buildReporter,
                        outputDirs = options.outputDirs,
                        classpathChanges = classpathChanges,
                        kotlinSourceFilesExtensions = kotlinFilenameExtensions,
                        icFeatures = icFeatures
                    )
                }

                val rootProjectDir = options.rootProjectDir
                val buildDir = options.buildDir
                parsedArguments.incrementalCompilation = true
                parsedArguments.freeArgs += javaSources
                incrementalCompiler.compile(
                    allSources, parsedArguments, loggerAdapter, aggregatedIcConfiguration.sourcesChanges.asChangedFiles,
                    fileLocations = if (rootProjectDir != null && buildDir != null) {
                        FileLocations(rootProjectDir, buildDir)
                    } else null
                ).asCompilationResult
            }
            null -> { // no IC configuration -> non-incremental compilation
                parsedArguments.freeArgs += sources.map { it.absolutePath }
                compiler.exec(loggerAdapter, Services.EMPTY, parsedArguments).asCompilationResult
            }
            else -> error(
                "Unexpected incremental compilation configuration: $options. " +
                        "In this version, it must be an instance of ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration " +
                        "for incremental compilation, or null for non-incremental compilation."
            )
        }
    }

    private fun checkJvmFirRequirements(
        arguments: List<String>,
    ): Boolean {
        val languageVersion: LanguageVersion = arguments.find { it.startsWith("-language-version") }
            ?.let {
                LanguageVersion.fromVersionString(it.substringAfter("="))
            }
            ?: LanguageVersion.LATEST_STABLE

        check(languageVersion >= LanguageVersion.KOTLIN_2_0) {
            "FIR incremental compiler runner is only compatible with Kotlin Language Version 2.0"
        }
        check(arguments.contains("-Xuse-fir-ic")) {
            "FIR incremental compiler runner requires '-Xuse-fir-ic' to be present in arguments"
        }

        return true
    }

    private fun compileWithinDaemon(
        projectId: ProjectId,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        daemonConfiguration: CompilerExecutionStrategy.Daemon,
        compilationConfiguration: JvmCompilationConfigurationImpl,
        sources: List<File>,
        arguments: List<String>,
    ): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling using the daemon strategy")
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

        val daemonOptions = configureDaemonOptions(
            DaemonOptions().apply {
                if (daemonConfiguration.shutdownDelay != null) {
                    shutdownDelayMilliseconds = daemonConfiguration.shutdownDelay.toMillis()
                }
            }
        )

        val (daemon, sessionId) = KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId,
            clientIsAliveFile,
            sessionIsAliveFlagFile,
            loggerAdapter,
            isDebugEnabled = true, // actually, prints daemon messages even unrelated to debug logs
            daemonJVMOptions = jvmOptions,
            daemonOptions = daemonOptions,
        ) ?: return ExitCode.INTERNAL_ERROR.asCompilationResult
        val daemonCompileOptions = compilationConfiguration.asDaemonCompilationOptions
        val isIncrementalCompilation = daemonCompileOptions is IncrementalCompilationOptions

        if (isIncrementalCompilation && daemonCompileOptions.useJvmFirRunner) {
            checkJvmFirRequirements(arguments)
        }

        val exitCode = daemon.compile(
            sessionId,
            arguments.toTypedArray() + sources.map { it.absolutePath }, // TODO: pass the sources explicitly KT-62759
            daemonCompileOptions,
            BasicCompilerServicesWithResultsFacadeServer(loggerAdapter),
            DaemonCompilationResults(
                loggerAdapter.kotlinLogger,
                compilationConfiguration.aggregatedIcConfiguration?.options?.rootProjectDir,
                DoNothingBuildMetricsReporter,
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
