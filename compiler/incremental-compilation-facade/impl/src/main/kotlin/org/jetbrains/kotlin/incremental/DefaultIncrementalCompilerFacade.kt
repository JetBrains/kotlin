/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.api.*
import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import java.io.File

object DumbMessageCollector : MessageCollector {
    override fun clear() {
        println("Clear messages")
    }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
    ) {
        println("Report $severity $message $location")
    }

    override fun hasErrors(): Boolean {
        return false
    }
}

class MessageReporter(private val messageLoggerCallback: MessageLogger) : MessageCollector {
    override fun clear() {}

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        val level = when {
            severity.isError -> FacadeLogLevel.ERROR.also { hasErrors = true }
            severity.isWarning -> FacadeLogLevel.WARNING
            severity == INFO -> FacadeLogLevel.INFO
            severity == LOGGING || severity == OUTPUT -> FacadeLogLevel.DEBUG
            else -> error("Unknown message severity: $severity")
        }
        val formattedMessage = buildString {
            location?.apply {
                val fileUri = File(path).toPath().toUri()
                append("$fileUri")
                if (line > 0 && column > 0) {
                    append(":$line:$column ")
                }
            }
            append(message)
        }
        messageLoggerCallback.report(level, formattedMessage)
    }

    private var hasErrors = false

    override fun hasErrors() = hasErrors
}

class DefaultIncrementalCompilerFacade : IncrementalCompilerFacade {
    private fun compileWithDaemon(
        launchOptions: LaunchOptions.Daemon,
        arguments: List<String>,
        options: org.jetbrains.kotlin.api.CompilationOptions,
        messageCollector: MessageCollector,
    ): ExitCode {
        println("Compiling with daemon")
        val compilerId = CompilerId.makeCompilerId(launchOptions.classpath)
        val clientIsAliveFlagFile = File("1")
        val sessionIsAliveFlagFile = File("2")

        val daemonOptions = configureDaemonJVMOptions(
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = false,
            inheritAdditionalProperties = true
        ).also { opts ->
            if (launchOptions.jvmArguments.isNotEmpty()) {
                opts.jvmParams.addAll(
                    launchOptions.jvmArguments.filterExtractProps(opts.mappers, "", opts.restMapper)
                )
            }
        }
        val (daemon, sessionId) = KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId,
            clientIsAliveFlagFile,
            sessionIsAliveFlagFile,
            messageCollector,
            false,
            daemonJVMOptions = daemonOptions
        ) ?: error("Can't get connection")
        val daemonCompileOptions = when (options) {
            is org.jetbrains.kotlin.api.CompilationOptions.Incremental -> IncrementalCompilationOptions(
                compilerMode = CompilerMode.INCREMENTAL_COMPILER,
                targetPlatform = CompileService.TargetPlatform.JVM,
                reportCategories = options.reportCategories,
                reportSeverity = options.reportSeverity,
                requestedCompilationResults = options.requestedCompilationResults,
                kotlinScriptExtensions = options.kotlinScriptExtensions,
                areFileChangesKnown = options.areFileChangesKnown,
                modifiedFiles = options.modifiedFiles,
                deletedFiles = options.deletedFiles,
                classpathChanges = options.classpathChanges,
                workingDir = options.workingDir,
                usePreciseJavaTracking = options.usePreciseJavaTracking,
                outputFiles = options.outputFiles,
                multiModuleICSettings = MultiModuleICSettings(
                    buildHistoryFile = options.multiModuleICSettings.buildHistoryFile,
                    useModuleDetection = options.multiModuleICSettings.useModuleDetection,
                ),
                modulesInfo = options.modulesInfo,
                withAbiSnapshot = options.withAbiSnapshot,
                preciseCompilationResultsBackup = options.preciseCompilationResultsBackup
            )
            is org.jetbrains.kotlin.api.CompilationOptions.NonIncremental -> CompilationOptions(
                compilerMode = CompilerMode.INCREMENTAL_COMPILER,
                targetPlatform = CompileService.TargetPlatform.JVM,
                reportCategories = options.reportCategories,
                reportSeverity = options.reportSeverity,
                requestedCompilationResults = options.requestedCompilationResults,
                kotlinScriptExtensions = options.kotlinScriptExtensions,
            )
        }
        val exitCode = daemon.compile(
            sessionId,
            arguments.toTypedArray(),
            daemonCompileOptions,
            BasicCompilerServicesWithResultsFacadeServer(messageCollector),
            DaemonCompilationResults()
        ).get()
        return ExitCode.values().find { it.code == exitCode } ?: if (exitCode == 0) {
            ExitCode.OK
        } else {
            ExitCode.COMPILATION_ERROR
        }
    }

    private fun compileInProcess(
        arguments: List<String>,
        options: org.jetbrains.kotlin.api.CompilationOptions,
        messageCollector: MessageCollector,
    ): ExitCode {
        println("Compiling in-process")
        val compiler = createCompiler(options.targetPlatform)
        val parsedArguments = prepareAndValidateCompilerArguments(compiler, arguments)
        return when (options) {
            is org.jetbrains.kotlin.api.CompilationOptions.Incremental -> compileIncrementally(parsedArguments, options, messageCollector)
            is org.jetbrains.kotlin.api.CompilationOptions.NonIncremental -> compiler.exec(
                messageCollector,
                Services.EMPTY,
                parsedArguments
            )
        }
    }

    private fun compileIncrementally(
        args: CommonCompilerArguments,
        options: org.jetbrains.kotlin.api.CompilationOptions.Incremental,
        messageCollector: MessageCollector,
    ): ExitCode {
        val incrementalCompilerRunner = when (options.targetPlatform) {
            TargetPlatform.JVM -> IncrementalCompilerRunnerWithArgs(
                args as K2JVMCompilerArguments,
                createIncrementalJvmCompilerRunner(
                    options.workingDir,
                    DoNothingBuildReporter,
                    options.multiModuleICSettings.buildHistoryFile,
                    options.outputFiles,
                    options.usePreciseJavaTracking,
                    options.multiModuleICSettings,
                    options.modulesInfo,
                    options.kotlinScriptExtensions?.toList() ?: emptyList(),
                    options.classpathChanges,
                    options.withAbiSnapshot,
                    options.preciseCompilationResultsBackup,
                    false,
                )
            )
            TargetPlatform.JS -> IncrementalCompilerRunnerWithArgs(
                args as K2JSCompilerArguments,
                createIncrementalJsCompilerRunner(
                    args,
                    options.workingDir,
                    DoNothingBuildReporter,
                    options.multiModuleICSettings.buildHistoryFile,
                    options.modulesInfo,
                    options.withAbiSnapshot,
                    options.preciseCompilationResultsBackup,
                    false,
                )
            )
            else -> {
                error("Unsupported platform")
            }
        }
        return execIncrementalCompilerRunner(
            incrementalCompilerRunner,
            options.kotlinScriptExtensions?.toList() ?: emptyList(),
            prepareFileChanges(options.areFileChangesKnown, options.modifiedFiles, options.deletedFiles),
            options.modulesInfo.projectRoot,
            messageCollector
        )
    }

    override fun compile(
        launchOptions: LaunchOptions,
        arguments: List<String>,
        options: org.jetbrains.kotlin.api.CompilationOptions,
        callbacks: Callbacks,
    ) {
        val messageCollector = callbacks.messageLogger?.let { MessageReporter(it) } ?: DumbMessageCollector
        val exitCode = when (launchOptions) {
            is LaunchOptions.Daemon -> compileWithDaemon(launchOptions, arguments, options, messageCollector)
            is LaunchOptions.InProcess -> compileInProcess(arguments, options, messageCollector)
        }
        if (exitCode != ExitCode.OK) {
            throw Exception("Compilation failed")
        }
    }
}