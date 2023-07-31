/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.configureDaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.filterExtractProps
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

private val ExitCode.asCompilationResult
    get() = when (this) {
        ExitCode.OK -> CompilationResult.COMPILATION_SUCCESS
        ExitCode.COMPILATION_ERROR -> CompilationResult.COMPILER_INTERNAL_ERROR
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
        arguments: List<String>
    ): CompilationResult {
        check(strategyConfig is CompilerExecutionStrategyConfigurationImpl) {
            "Initial strategy configuration object must be acquired from the `makeCompilerExecutionStrategyConfiguration` method."
        }
        check(compilationConfig is JvmCompilationConfigurationImpl) {
            "Initial JVM compilation configuration object must be acquired from the `makeJvmCompilationConfiguration` method."
        }
        val loggerAdapter = KotlinLoggerMessageCollectorAdapter(compilationConfig.logger)
        return when (val selectedStrategy = strategyConfig.selectedStrategy) {
            is CompilerExecutionStrategy.InProcess -> compileInProcess(loggerAdapter, sources, arguments)
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
        val file = buildIdToSessionFlagFile.remove(projectId) ?: return
        file.delete()
    }

    private fun compileInProcess(
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        sources: List<File>,
        arguments: List<String>
    ): CompilationResult {
        val compiler = K2JVMCompiler()
        val parsedArguments = compiler.createArguments()
        parseCommandLineArguments(arguments, parsedArguments)
        validateArguments(parsedArguments.errors)?.let {
            throw CompilerArgumentsParseException(it)
        }
        parsedArguments.freeArgs += sources.map { it.absolutePath } // TODO: they're not explicitly passed yet
        loggerAdapter.report(CompilerMessageSeverity.INFO, arguments.toString())
        return compiler.exec(loggerAdapter, Services.EMPTY, parsedArguments).asCompilationResult
    }

    private fun compileWithinDaemon(
        projectId: ProjectId,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        daemonConfiguration: CompilerExecutionStrategy.Daemon,
        compilationConfiguration: JvmCompilationConfigurationImpl,
        sources: List<File>,
        arguments: List<String>
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
            arguments.toTypedArray() + sources.map { it.absolutePath }, // TODO: the sources not explicitly passed yet
            daemonCompileOptions,
            BasicCompilerServicesWithResultsFacadeServer(loggerAdapter),
            DaemonCompilationResults()
        ).get()

        return (ExitCode.entries.find { it.code == exitCode } ?: if (exitCode == 0) {
            ExitCode.OK
        } else {
            ExitCode.COMPILATION_ERROR
        }).asCompilationResult
    }
}

internal class CompilationServiceProxy : CompilationService by CompilationServiceImpl