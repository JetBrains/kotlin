/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.compilation.*
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.config.Services

private val ExitCode.asCompilationResult
    get() = when (this) {
        ExitCode.OK -> CompilationResult.COMPILATION_SUCCESS
        ExitCode.COMPILATION_ERROR -> CompilationResult.COMPILER_INTERNAL_ERROR
        ExitCode.INTERNAL_ERROR -> CompilationResult.COMPILER_INTERNAL_ERROR
        ExitCode.OOM_ERROR -> CompilationResult.COMPILATION_OOM_ERROR
        else -> error("Unexpected exit code: $this")
    }


internal class CompilationServiceImpl : CompilationService {
    override fun compile(
        compilationStrategySettings: CompilationStrategySettings,
        arguments: List<String>,
        compilationOptions: CompilationOptions,
    ): CompilationResult {
        val loggerAdapter = compilationOptions.logger?.run { KotlinLoggerMessageCollectorAdapter(this) } ?: DefaultMessageCollectorLoggingAdapter
        return when (compilationStrategySettings) {
            is CompilationStrategySettings.InProcess -> compileInProcess(arguments, compilationOptions, loggerAdapter)
        }
    }

    private fun compileInProcess(
        arguments: List<String>,
        compilationOptions: CompilationOptions,
        loggerAdapter: MessageCollector,
    ) = when (compilationOptions) {
        is NonIncrementalCompilationOptions -> {
            @Suppress("UNCHECKED_CAST")
            val compiler = when (compilationOptions) {
                is JsCompilationOptions -> K2JSCompiler()
                is JvmCompilationOptions -> K2JVMCompiler()
                is MetadataCompilationOptions -> K2MetadataCompiler()
            } as CLITool<CommonCompilerArguments>
            val parsedArguments = compiler.createArguments()
            parseCommandLineArguments(arguments, parsedArguments)
            validateArguments(parsedArguments.errors)?.let {
                throw ParseCompilationArgumentsException(it)
            }
            compiler.exec(loggerAdapter, Services.EMPTY, parsedArguments).asCompilationResult
        }
        else -> error("Unexpected compilation options: $compilationOptions")
    }
}