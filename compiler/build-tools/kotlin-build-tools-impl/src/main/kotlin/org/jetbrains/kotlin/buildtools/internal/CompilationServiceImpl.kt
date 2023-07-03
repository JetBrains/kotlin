/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File

private val ExitCode.asCompilationResult
    get() = when (this) {
        ExitCode.OK -> CompilationResult.COMPILATION_SUCCESS
        ExitCode.COMPILATION_ERROR -> CompilationResult.COMPILER_INTERNAL_ERROR
        ExitCode.INTERNAL_ERROR -> CompilationResult.COMPILER_INTERNAL_ERROR
        ExitCode.OOM_ERROR -> CompilationResult.COMPILATION_OOM_ERROR
        else -> error("Unexpected exit code: $this")
    }

internal object CompilationServiceImpl : CompilationService {
    override fun calculateClasspathSnapshot(classpathEntry: File): ClasspathEntrySnapshot {
        TODO("Calculating classpath snapshots via the Build Tools API is not yet implemented: KT-57565")
    }

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
        return when (strategyConfig.selectedStrategy) {
            is CompilerExecutionStrategy.InProcess -> compileInProcess(loggerAdapter, sources, arguments)
            is CompilerExecutionStrategy.Daemon -> TODO("The daemon strategy is not yet supported in the Build Tools API")
        }
    }

    override fun finishProjectCompilation(projectId: ProjectId) {

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
        parsedArguments.freeArgs += sources.map { it.absolutePath } // TODO: they're not actually passed yet
        loggerAdapter.report(CompilerMessageSeverity.INFO, arguments.toString())
        return compiler.exec(loggerAdapter, Services.EMPTY, parsedArguments).asCompilationResult
    }
}

internal class CompilationServiceProxy : CompilationService by CompilationServiceImpl