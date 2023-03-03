/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.api.IncrementalCompilerFacade
import org.jetbrains.kotlin.api.IncrementalKotlinCompilationOptions
import org.jetbrains.kotlin.api.KotlinCompilationOptions
import org.jetbrains.kotlin.api.TargetPlatform
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryAndroid
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJvm
import java.io.File

class DefaultIncrementalCompilerFacade : IncrementalCompilerFacade {
    override fun doSomething() {
        println("Did something")
    }

    override fun compileInProcess(arguments: List<String>, options: KotlinCompilationOptions) {
        val compiler = K2JVMCompiler()
        val k2PlatformArgs = compiler.createArguments()
        parseCommandLineArguments(arguments, k2PlatformArgs)
        if (options is IncrementalKotlinCompilationOptions) {
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
                    runner.compile(allKotlinFiles, k2PlatformArgs, object : MessageCollector {
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

                    }, changedFiles, options.modulesInfo.projectRoot)
                }
                else -> throw NotImplementedError("Not yet implemented")
            }
        } else {
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