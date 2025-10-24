/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal.js.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.js.operations.JsCompilationOperation
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.trackers.LookupTrackerAdapter
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal class JsCompilationOperationImpl(
    private val kotlinSources: List<Path>,
    override val compilerArguments: JsArgumentsImpl = JsArgumentsImpl(),
) : BuildOperationImpl<CompilationResult>(), JsCompilationOperation {

    private val options: Options = Options(JsCompilationOperation::class)

    @UseFromImplModuleRestricted
    override fun <V> get(key: JsCompilationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsCompilationOperation.Option<V>, value: V) {
        options[key] = value
    }

    private operator fun <V> get(key: Option<V>): V = options[key]

    @OptIn(UseFromImplModuleRestricted::class)
    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    override fun execute(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): CompilationResult {
        val loggerAdapter =
            logger?.let { KotlinLoggerMessageCollectorAdapter(it) } ?: KotlinLoggerMessageCollectorAdapter(DefaultKotlinLogger)
        return when (executionPolicy) {
            InProcessExecutionPolicyImpl -> {
                compile(loggerAdapter)
            }
            else -> {
                CompilationResult.COMPILATION_ERROR.also {
                    loggerAdapter.kotlinLogger.error("Unknown execution mode: ${executionPolicy::class.qualifiedName}")
                }
            }
        }
    }

    private fun compile(loggerAdapter: KotlinLoggerMessageCollectorAdapter): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling")
        val arguments = compilerArguments.toCompilerArguments()
        val compiler = K2JSCompiler()
        arguments.freeArgs += kotlinSources.map { it.absolutePathString() }
        val services = Services.Builder().apply {
            get(LOOKUP_TRACKER)?.let { tracker: CompilerLookupTracker ->
                register(LookupTracker::class.java, LookupTrackerAdapter(tracker))
            }
        }.build()
        logCompilerArguments(loggerAdapter, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))
        return compiler.exec(loggerAdapter, services, arguments).asCompilationResult
    }

    private fun logCompilerArguments(
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        arguments: K2JSCompilerArguments,
        argumentsLogLevel: JsCompilationOperation.CompilerArgumentsLogLevel,
    ) {
        with(loggerAdapter.kotlinLogger) {
            val message = "Kotlin compiler args: ${arguments.toArgumentStrings().joinToString(" ")}"
            when (argumentsLogLevel) {
                JsCompilationOperation.CompilerArgumentsLogLevel.ERROR -> error(message)
                JsCompilationOperation.CompilerArgumentsLogLevel.WARNING -> warn(message)
                JsCompilationOperation.CompilerArgumentsLogLevel.INFO -> info(message)
                JsCompilationOperation.CompilerArgumentsLogLevel.DEBUG -> debug(message)
            }
        }
    }

    companion object {
        val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER", null)

        val COMPILER_ARGUMENTS_LOG_LEVEL: Option<JsCompilationOperation.CompilerArgumentsLogLevel> =
            Option("COMPILER_ARGUMENTS_LOG_LEVEL", default = JsCompilationOperation.CompilerArgumentsLogLevel.DEBUG)
    }
}