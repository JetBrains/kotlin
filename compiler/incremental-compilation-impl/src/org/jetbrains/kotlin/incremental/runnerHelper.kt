/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.api.ClasspathChanges
import org.jetbrains.kotlin.api.IncrementalModuleInfo
import org.jetbrains.kotlin.api.MultiModuleICSettings
import org.jetbrains.kotlin.api.TargetPlatform
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryAndroid
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJs
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJvm
import java.io.File

@Suppress("UNCHECKED_CAST")
fun createCompiler(targetPlatform: TargetPlatform) = when (targetPlatform) {
    TargetPlatform.JVM -> K2JVMCompiler()
    TargetPlatform.JS -> K2JSCompiler()
    TargetPlatform.METADATA -> K2MetadataCompiler()
    else -> error("Unsupported target platform $targetPlatform")
} as CLICompiler<CommonCompilerArguments>

class ArgumentsParseException(message: String) : Exception(message)

fun prepareAndValidateCompilerArguments(compiler: CLICompiler<*>, rawArguments: List<String>): CommonCompilerArguments {
    val k2PlatformArgs = compiler.createArguments()
    parseCommandLineArguments(rawArguments, k2PlatformArgs)
    val argumentParseError = validateArguments(k2PlatformArgs.errors)

    if (argumentParseError != null) {
        throw ArgumentsParseException(argumentParseError)
    }
    return k2PlatformArgs
}

fun extractKotlinFilesFromFreeArgs(args: CommonCompilerArguments, kotlinScriptExtensions: List<String>): List<File> {
    val allKotlinExtensions = (DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + kotlinScriptExtensions).distinct()
    val dotExtensions = allKotlinExtensions.map { ".$it" }
    val freeArgs = arrayListOf<String>()
    val kotlinFiles = arrayListOf<File>()
    for (arg in args.freeArgs) {
        val file = File(arg)
        if (file.isFile && dotExtensions.any { ext -> file.path.endsWith(ext, ignoreCase = true) }) {
            kotlinFiles.add(file)
        } else {
            freeArgs.add(arg)
        }
    }
    args.freeArgs = freeArgs
    return kotlinFiles
}

fun prepareFileChanges(
    areFilesChangesKnown: Boolean,
    modifiedFiles: List<File>?,
    deletedFiles: List<File>?,
) = if (areFilesChangesKnown) {
    ChangedFiles.Known(modifiedFiles!!, deletedFiles!!)
} else {
    ChangedFiles.Unknown()
}

fun <A : CommonCompilerArguments> execIncrementalCompilerRunner(
    incrementalCompilerRunner: IncrementalCompilerRunnerWithArgs<A>,
    kotlinScriptExtensions: List<String>,
    changedFiles: ChangedFiles,
    projectRoot: File,
    messageCollector: MessageCollector,
): ExitCode {
    val (args, runner) = incrementalCompilerRunner
    val allKotlinFiles = extractKotlinFilesFromFreeArgs(args, kotlinScriptExtensions)
    return try {
        runner.compile(allKotlinFiles, args, messageCollector, changedFiles, projectRoot)
    } finally {
//        reporter.flush()
    }
}

fun createIncrementalJsCompilerRunner(
    args: K2JSCompilerArguments,
    workingDir: File,
    reporter: BuildReporter,
    buildHistoryFile: File,
    modulesInfo: IncrementalModuleInfo,
    withAbiSnapshot: Boolean,
    usePreciseCompilationResultsBackup: Boolean,
    keepIncrementalCompilationCachesInMemory: Boolean,
): IncrementalJsCompilerRunner {
    val modulesApiHistory = ModulesApiHistoryJs(modulesInfo)
    val scopeExpansion = if (args.isIrBackendEnabled()) CompileScopeExpansionMode.ALWAYS else CompileScopeExpansionMode.NEVER
    return IncrementalJsCompilerRunner(
        workingDir = workingDir,
        reporter = reporter,
        buildHistoryFile = buildHistoryFile,
        scopeExpansion = scopeExpansion,
        modulesApiHistory = modulesApiHistory,
        withAbiSnapshot = withAbiSnapshot,
        preciseCompilationResultsBackup = usePreciseCompilationResultsBackup,
        keepIncrementalCompilationCachesInMemory = keepIncrementalCompilationCachesInMemory,
    )
}

fun createIncrementalJvmCompilerRunner(
    workingDir: File,
    reporter: BuildReporter,
    buildHistoryFile: File,
    outputFiles: List<File>,
    usePreciseJavaTracking: Boolean,
    multiModuleICSettings: MultiModuleICSettings,
    modulesInfo: IncrementalModuleInfo,
    kotlinScriptExtensions: List<String>,
    classpathChanges: ClasspathChanges,
    withAbiSnapshot: Boolean,
    usePreciseCompilationResultsBackup: Boolean,
    keepIncrementalCompilationCachesInMemory: Boolean,
): IncrementalJvmCompilerRunner {
    val allKotlinExtensions = (DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + kotlinScriptExtensions).distinct()
    val modulesApiHistory = if (!multiModuleICSettings.useModuleDetection) {
        ModulesApiHistoryJvm(modulesInfo)
    } else {
        ModulesApiHistoryAndroid(modulesInfo)
    }
    reporter.info { "Use module detection: ${multiModuleICSettings.useModuleDetection}" }
    return IncrementalJvmCompilerRunner(
        workingDir,
        reporter,
        buildHistoryFile = buildHistoryFile,
        outputDirs = outputFiles,
        usePreciseJavaTracking = usePreciseJavaTracking,
        modulesApiHistory = modulesApiHistory,
        kotlinSourceFilesExtensions = allKotlinExtensions,
        classpathChanges = classpathChanges,
        withAbiSnapshot = withAbiSnapshot,
        preciseCompilationResultsBackup = usePreciseCompilationResultsBackup,
        keepIncrementalCompilationCachesInMemory = keepIncrementalCompilationCachesInMemory,
    )
}

data class IncrementalCompilerRunnerWithArgs<A : CommonCompilerArguments>(
    val args: A,
    val runner: IncrementalCompilerRunner<A, *>,
)