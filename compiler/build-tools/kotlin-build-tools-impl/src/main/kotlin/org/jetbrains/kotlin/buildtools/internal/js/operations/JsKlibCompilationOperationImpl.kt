/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal.js.operations

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.endMeasureGc
import org.jetbrains.kotlin.build.report.metrics.startMeasureGc
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModuleEntry
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.CompilerArgumentValueAdapter
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.BaseCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.incremental.disablePreciseJavaTrackingIfK2
import org.jetbrains.kotlin.incremental.extractKotlinSourcesFromFreeCompilerArguments
import org.jetbrains.kotlin.incremental.storage.FileLocations
import java.io.File
import java.nio.file.Path
import kotlin.collections.plus

internal class JsKlibCompilationOperationImpl private constructor(
    override val options: Options = Options(JsKlibCompilationOperation::class),
    override val sources: List<Path>,
    override val destination: Path,
    compilerArguments: JsArgumentsImpl = JsArgumentsImpl(
        CompilerArgumentValueAdapter.getOrNull(JsArguments.JsArgument::class)
    ),
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
) : BaseCompilationOperationImpl<JsArgumentsImpl, K2JSCompilerArguments>(compilerArguments, buildIdToSessionFlagFile),
    JsKlibCompilationOperation, JsKlibCompilationOperation.Builder,
    DeepCopyable<JsKlibCompilationOperationImpl> {
    constructor(
        sources: List<Path>,
        destination: Path,
        compilerArguments: JsArgumentsImpl = JsArgumentsImpl(
            CompilerArgumentValueAdapter.getOrNull(JsArguments.JsArgument::class)
        ),
        buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
    ) : this(
        options = Options(JsKlibCompilationOperation::class),
        sources = sources,
        destination = destination,
        compilerArguments = compilerArguments,
        buildIdToSessionFlagFile = buildIdToSessionFlagFile
    )

    override fun historyBasedIcConfigurationBuilder(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        modulesInformation: List<IncrementalModuleEntry>,
    ): JsHistoryBasedIncrementalCompilationConfiguration.Builder {
        return JsHistoryBasedIncrementalCompilationConfigurationImpl(workingDirectory, sourcesChanges, modulesInformation)
    }

    override fun toBuilder(): JsKlibCompilationOperation.Builder = deepCopy()

    override fun deepCopy(): JsKlibCompilationOperationImpl {
        return JsKlibCompilationOperationImpl(
            options.deepCopy(),
            sources,
            destination,
            compilerArguments.deepCopy(),
            buildIdToSessionFlagFile
        )
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JsKlibCompilationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsKlibCompilationOperation.Option<V>, value: V) {
        options[key] = value
    }

    override fun build(): JsKlibCompilationOperation = deepCopy()

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    override fun getRootProjectDir(): Path? {
        return (get(INCREMENTAL_COMPILATION) as? JsHistoryBasedIncrementalCompilationConfigurationImpl)?.get(ROOT_PROJECT_DIR)
    }

    override fun createAndPrepareCompilerArguments(): K2JSCompilerArguments = compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.outputDir = destination.absolutePathStringOrThrow()
        }

    override val targetPlatform: CompileService.TargetPlatform = CompileService.TargetPlatform.JS

    override fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
    ): IncrementalCompilationOptions? {
        return null
    }

    override fun shouldCompileIncrementally(): Boolean {
        return false
    }

    override fun createCompiler(): CLICompiler<K2JSCompilerArguments> {
        return K2JSCompiler()
    }

    override fun K2JSCompilerArguments.addSources() {
        freeArgs += sources.map { it.absolutePathStringOrThrow() }
    }

    override fun compileIncrementallyInProcess(
        arguments: K2JSCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        val icConfiguration = get(INCREMENTAL_COMPILATION)
        requireNotNull(icConfiguration) { "Missing INCREMENTAL_COMPILATION option." }
        check(icConfiguration is JsHistoryBasedIncrementalCompilationConfigurationImpl) {
            "Unexpected incremental compilation configuration: ${icConfiguration::class}. In this version, it must be an instance of JsHistoryBasedIncrementalCompilationConfiguration for incremental compilation, or null for non-incremental compilation."
        }

        val projectDir = icConfiguration[ROOT_PROJECT_DIR]?.toFile()
        val buildDir = icConfiguration[MODULE_BUILD_DIR]?.toFile()

        @Suppress("DEPRECATION") val kotlinSources = extractKotlinSourcesFromFreeCompilerArguments(
            arguments, DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS, includeJavaSources = true
        ) + sources.map { it.toFile() }

        val classpathChanges = icConfiguration.classpathChanges
        val metricsReporter = getMetricsReporter()
        metricsReporter.startMeasureGc()
        val buildReporter = BuildReporter(
            icReporter = BuildToolsApiBuildICReporter(
                kotlinLogger = loggerAdapter.kotlinLogger,
                rootProjectDir = projectDir,
                buildMetricsReporter = metricsReporter,
            ), buildMetricsReporter = metricsReporter
        )
        val verifiedPreciseJavaTracking =
            arguments.disablePreciseJavaTrackingIfK2(usePreciseJavaTrackingByDefault = icConfiguration[PRECISE_JAVA_TRACKING])
        val icFeatures = icConfiguration.extractIncrementalCompilationFeatures().copy(
            usePreciseJavaTracking = verifiedPreciseJavaTracking
        )
        val kotlinFilenameExtensions = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + (get(JvmCompilationOperationImpl.Companion.KOTLINSCRIPT_EXTENSIONS) ?: emptyArray())
        val incrementalCompiler = if (icConfiguration[USE_FIR_RUNNER] && checkJvmFirRequirements(compilerArguments)) {
            getFirRunner(
                icConfiguration.workingDirectory,
                buildReporter,
                icConfiguration,
                classpathChanges,
                kotlinFilenameExtensions,
                icFeatures
            )
        } else {
            getNonFirRunner(
                icConfiguration.workingDirectory,
                buildReporter,
                icConfiguration,
                classpathChanges,
                kotlinFilenameExtensions,
                icFeatures
            )
        }

        arguments.incrementalCompilation = true
        logCompilerArguments(loggerAdapter, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))

        val fileLocations = if (projectDir != null && buildDir != null) {
            FileLocations(projectDir, buildDir)
        } else null
        val compilationResult = incrementalCompiler.compile(
            kotlinSources, arguments, loggerAdapter, icConfiguration.sourcesChanges.asChangedFiles, fileLocations
        ).asCompilationResult

        metricsReporter.endMeasureGc()
        populateMetricsCollector(metricsReporter)

        return compilationResult
    }

    companion object {
        val INCREMENTAL_COMPILATION: Option<JsIncrementalCompilationConfiguration?> = Option("INCREMENTAL_COMPILATION", null)
    }
}