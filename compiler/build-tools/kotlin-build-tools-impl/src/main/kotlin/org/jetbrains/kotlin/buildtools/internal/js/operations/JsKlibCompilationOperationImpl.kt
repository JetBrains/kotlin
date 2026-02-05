/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js.operations

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporterImpl
import org.jetbrains.kotlin.build.report.metrics.COMPILE_ITERATION
import org.jetbrains.kotlin.build.report.metrics.endMeasureGc
import org.jetbrains.kotlin.build.report.metrics.startMeasureGc
import org.jetbrains.kotlin.build.report.reportPerformanceData
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.CompilerArgumentsLogLevel
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.trackers.LookupTrackerAdapter
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.nio.file.Path

@OptIn(ExperimentalCompilerArgument::class)
internal class JsKlibCompilationOperationImpl(
    override val options: Options = Options(JsKlibCompilationOperation::class),
    override val sources: List<Path>,
    override val destinationKlib: Path,
    override val compilerArguments: JsArgumentsImpl = JsArgumentsImpl(),
) : CancellableBuildOperationImpl<CompilationResult>(), JsKlibCompilationOperation, JsKlibCompilationOperation.Builder {

    constructor(
        sources: List<Path>,
        destinationKlib: Path,
        compilerArguments: JsArgumentsImpl = JsArgumentsImpl(),
    ) : this(
        options = Options(JsKlibCompilationOperation::class),
        sources = sources,
        destinationKlib = destinationKlib,
        compilerArguments = compilerArguments,
    )

    override fun toBuilder(): JsKlibCompilationOperation.Builder = deepCopy()

    fun deepCopy(): JsKlibCompilationOperationImpl = JsKlibCompilationOperationImpl(
        options.deepCopy(),
        sources,
        destinationKlib,
        compilerArguments.deepCopy(),
    )

    @OptIn(UseFromImplModuleRestricted::class)
    override fun <V> get(key: JsKlibCompilationOperation.Option<V>): V = options[key.id]

    @OptIn(UseFromImplModuleRestricted::class)
    override fun <V> set(key: JsKlibCompilationOperation.Option<V>, value: V) {
        options[key.id] = value
    }

    private operator fun <V> get(key: Option<V>): V = options[key]

    override fun build(): JsKlibCompilationOperation = deepCopy()

    override fun executeCancellableImpl(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): CompilationResult {
        val loggerAdapter =
            logger?.let { KotlinLoggerMessageCollectorAdapter(it) } ?: KotlinLoggerMessageCollectorAdapter(DefaultKotlinLogger)
        return compileInProcess(loggerAdapter)
    }

    private fun compileInProcess(loggerAdapter: KotlinLoggerMessageCollectorAdapter): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling using the in-process strategy")
        setupIdeaStandaloneExecution()
        val arguments = compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.outputDir = destinationKlib.absolutePathStringOrThrow()
        }
//        val kotlinFilenameExtensions = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + (get(KOTLINSCRIPT_EXTENSIONS) ?: emptyArray())
//        return when (val aggregatedIcConfiguration = get(INCREMENTAL_COMPILATION)) {
//            is JvmSnapshotBasedIncrementalCompilationConfiguration -> {
//                aggregatedIcConfiguration.compileInProcess(arguments, loggerAdapter, kotlinFilenameExtensions)
//            }
//            null -> { // no IC configuration -> non-incremental compilation
        return compileInProcessWithoutIc(arguments, loggerAdapter)
//            }
//            else -> error(
//                "Unexpected incremental compilation configuration: $aggregatedIcConfiguration. In this version, it must be an instance of JvmSnapshotBasedIncrementalCompilationConfiguration for incremental compilation, or null for non-incremental compilation."
//            )
//        }
    }

    private fun compileInProcessWithoutIc(
        arguments: K2JSCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        val compiler = K2JSCompiler()
        arguments.freeArgs += sources.map { it.absolutePathStringOrThrow() }
        val services = Services.Builder().apply {
            register(CompilationCanceledStatus::class.java, cancellationHandle)
            get(LOOKUP_TRACKER)?.let { tracker: CompilerLookupTracker ->
                register(LookupTracker::class.java, LookupTrackerAdapter(tracker))
            }
        }.build()
        logCompilerArguments(loggerAdapter, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))
        val metricsReporter = getMetricsReporter()
        metricsReporter.startMeasureGc()
        val compilationResult = compiler.exec(loggerAdapter, services, arguments).asCompilationResult
        metricsReporter.reportPerformanceData(compiler.defaultPerformanceManager.unitStats)
        metricsReporter.addMetric(COMPILE_ITERATION, 1) // in non-IC case there's always 1 iteration
        metricsReporter.endMeasureGc()

        if (this@JsKlibCompilationOperationImpl[XX_KGP_METRICS_COLLECTOR] && metricsReporter is BuildMetricsReporterImpl) {
            this@JsKlibCompilationOperationImpl[XX_KGP_METRICS_COLLECTOR_OUT] = ByteArrayOutputStream().apply {
                ObjectOutputStream(this).writeObject(metricsReporter)
            }.toByteArray()
        }

        return compilationResult
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    private fun logCompilerArguments(
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        arguments: K2JSCompilerArguments,
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
        val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER", null)
        val COMPILER_ARGUMENTS_LOG_LEVEL: Option<CompilerArgumentsLogLevel> =
            Option("COMPILER_ARGUMENTS_LOG_LEVEL", default = CompilerArgumentsLogLevel.DEBUG)
    }
}
