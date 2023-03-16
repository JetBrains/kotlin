/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.api.TargetPlatform
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import java.io.File

fun createCompiler(targetPlatform: TargetPlatform) = when (targetPlatform) {
    TargetPlatform.JVM -> K2JVMCompiler()
    TargetPlatform.JS -> K2JSCompiler()
    TargetPlatform.METADATA -> K2MetadataCompiler()
    else -> error("Unsupported target platform $targetPlatform")
} as CLICompiler<*>

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

private fun extractKotlinFilesFromFreeArgs(args: CommonCompilerArguments, kotlinScriptExtensions: List<String>): List<File> {
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
    incrementalCompilerRunner: IncrementalCompilerRunner<A, *>,
    args: A,
    kotlinScriptExtensions: List<String>,
    changedFiles: ChangedFiles,
    projectRoot: File,
    messageCollector: MessageCollector,
): ExitCode {
    val allKotlinFiles = extractKotlinFilesFromFreeArgs(args, kotlinScriptExtensions)
    return try {
        incrementalCompilerRunner.compile(allKotlinFiles, args, messageCollector, changedFiles, projectRoot)
    } finally {
//        reporter.flush()
    }
}