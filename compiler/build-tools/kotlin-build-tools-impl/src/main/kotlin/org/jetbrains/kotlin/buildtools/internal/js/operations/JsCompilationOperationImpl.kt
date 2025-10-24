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
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.CompilerArgumentsLogLevel
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.trackers.LookupTrackerAdapter
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal class JsCompilationOperationImpl(
    private val kotlinSources: List<Path>,
    private val destinationDirectory: Path,
    override val compilerArguments: JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(),
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
) : BuildOperationImpl<CompilationResult>(), JsCompilationOperation {

    private val options: Options = Options(JvmCompilationOperation::class)

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
        setupIdeaStandaloneExecution()
        val arguments = compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.destination = destinationDirectory.absolutePathString()
        }
        return when (val aggregatedIcConfiguration = get(INCREMENTAL_COMPILATION)) {
            null -> { // no IC configuration -> non-incremental compilation
                compileJS(arguments, loggerAdapter)
            }
            else -> error(
                "Unexpected incremental compilation configuration: $aggregatedIcConfiguration. In this version, it must be an instance of JvmSnapshotBasedIncrementalCompilationConfiguration for incremental compilation, or null for non-incremental compilation."
            )
        }
    }

    private fun compileJS(
        arguments: K2JVMCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
//        val compiler = K2JSCompiler()
        val compiler = K2JVMCompiler()
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
        arguments: K2JVMCompilerArguments,
        argumentsLogLevel: CompilerArgumentsLogLevel,
    ) {
        with(loggerAdapter.kotlinLogger) {
            val message = "Kotlin compiler args: ${arguments.toArgumentStrings().joinToString(" ")}"
            when (argumentsLogLevel) {
                CompilerArgumentsLogLevel.ERROR -> error(message)
                CompilerArgumentsLogLevel.WARNING -> warn(message)
                CompilerArgumentsLogLevel.INFO -> info(message)
                CompilerArgumentsLogLevel.DEBUG -> debug(message)
            }
        }
    }

    companion object {
        val INCREMENTAL_COMPILATION: Option<JvmIncrementalCompilationConfiguration?> = Option("INCREMENTAL_COMPILATION", null)

        val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER", null)

        val KOTLINSCRIPT_EXTENSIONS: Option<Array<String>?> = Option("KOTLINSCRIPT_EXTENSIONS", null)

        val COMPILER_ARGUMENTS_LOG_LEVEL: Option<CompilerArgumentsLogLevel> =
            Option("COMPILER_ARGUMENTS_LOG_LEVEL", default = CompilerArgumentsLogLevel.DEBUG)
    }
}