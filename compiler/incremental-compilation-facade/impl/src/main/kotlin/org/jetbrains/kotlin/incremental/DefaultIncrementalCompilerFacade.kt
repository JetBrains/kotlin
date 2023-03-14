/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.api.*
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryAndroid
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJvm
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

class DefaultIncrementalCompilerFacade : IncrementalCompilerFacade {
    private fun compileWithDaemon(
        launchOptions: LaunchOptions.Daemon,
        arguments: List<String>,
        options: org.jetbrains.kotlin.api.CompilationOptions
    ) {
        println("Compiling with daemon")
        val compilerId = CompilerId.makeCompilerId(launchOptions.classpath)
        val clientIsAliveFlagFile = File("1")
        val sessionIsAliveFlagFile = File("2")
        val messageCollector = DumbMessageCollector

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
        daemon.compile(sessionId, arguments.toTypedArray(), daemonCompileOptions, BasicCompilerServicesWithResultsFacadeServer(messageCollector), DaemonCompilationResults())
    }

    private fun compileInProcess(arguments: List<String>, options: org.jetbrains.kotlin.api.CompilationOptions) {
        println("Compiling in-process")
        val compiler = K2JVMCompiler()
        val k2PlatformArgs = compiler.createArguments()
        parseCommandLineArguments(arguments, k2PlatformArgs)
        when (options) {
            is org.jetbrains.kotlin.api.CompilationOptions.Incremental -> {
                when (options.targetPlatform) {
                    TargetPlatform.JVM -> {
                        val allKotlinExtensions = (DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS +
                                (options.kotlinScriptExtensions ?: emptyArray())).distinct()
                        val dotExtensions = allKotlinExtensions.map { ".$it" }
                        val modulesApiHistory = options.run {
                            if (!multiModuleICSettings.useModuleDetection) {
                                ModulesApiHistoryJvm(modulesInfo)
                            } else {
                                ModulesApiHistoryAndroid(modulesInfo)
                            }
                        }
                        val allKotlinFiles = arrayListOf<File>()
                        val freeArgs = arrayListOf<String>()
                        for (arg in k2PlatformArgs.freeArgs) {
                            val file = File(arg)
                            if (file.isFile && dotExtensions.any { ext -> file.path.endsWith(ext, ignoreCase = true) }) {
                                allKotlinFiles.add(file)
                            } else {
                                freeArgs.add(arg)
                            }
                        }

                        val changedFiles = if (options.areFileChangesKnown) {
                            ChangedFiles.Known(options.modifiedFiles!!, options.deletedFiles!!)
                        } else {
                            ChangedFiles.Unknown()
                        }

                        k2PlatformArgs.freeArgs = freeArgs
                        k2PlatformArgs.incrementalCompilation = true
                        val runner = IncrementalJvmCompilerRunner(
                            options.workingDir,
                            DoNothingBuildReporter,
                            options.usePreciseJavaTracking,
                            options.multiModuleICSettings.buildHistoryFile,
                            options.outputFiles,
                            modulesApiHistory,
                            allKotlinExtensions,
                            options.classpathChanges,
                            options.withAbiSnapshot,
                            options.preciseCompilationResultsBackup
                        )
                        runner.compile(allKotlinFiles, k2PlatformArgs, DumbMessageCollector, changedFiles, options.modulesInfo.projectRoot)
                    }
                    else -> throw NotImplementedError("Not yet implemented")
                }
            }
            is org.jetbrains.kotlin.api.CompilationOptions.NonIncremental -> {
                when (options.targetPlatform) {
                    TargetPlatform.JVM -> {
//                    val argumentParseError = validateArguments(k2PlatformArgs.errors)
                        compiler
                            .exec(object : MessageCollector {
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

                            }, Services.EMPTY, k2PlatformArgs)
                    }
                    else -> throw NotImplementedError("Not yet implemented")
                }
            }
        }
    }

    override fun compile(launchOptions: LaunchOptions, arguments: List<String>, options: org.jetbrains.kotlin.api.CompilationOptions) {
        when (launchOptions) {
            is LaunchOptions.Daemon -> compileWithDaemon(launchOptions, arguments, options)
            is LaunchOptions.InProcess -> compileInProcess(arguments, options)
        }
    }
}