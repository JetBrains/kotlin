/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.TRACK_CONFIGURATION_INPUTS
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl.Companion.HISTORY_FILE_DIR
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl.Companion.ROOT_PROJECT_BUILD_DIR
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJs
import org.jetbrains.kotlin.incremental.storage.FileLocations
import java.io.File
import java.nio.file.Path

internal class JsKlibCompilationOperationImpl private constructor(
    override val options: Options = Options(JsKlibCompilationOperation::class),
    override val sources: List<Path>,
    override val destination: Path,
    compilerArguments: JsArgumentsImpl = JsArgumentsImpl(),
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
    private val compilerVersion: String,
) : BaseCompilationOperationImpl<JsArgumentsImpl, K2JSCompilerArguments>(compilerArguments, buildIdToSessionFlagFile),
    JsKlibCompilationOperation, JsKlibCompilationOperation.Builder,
    DeepCopyable<JsKlibCompilationOperationImpl> {
    constructor(
        sources: List<Path>,
        destination: Path,
        compilerArguments: JsArgumentsImpl = JsArgumentsImpl(),
        buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
        compilerVersion: String,
    ) : this(
        options = Options(JsKlibCompilationOperation::class),
        sources = sources,
        destination = destination,
        compilerArguments = compilerArguments,
        buildIdToSessionFlagFile = buildIdToSessionFlagFile,
        compilerVersion = compilerVersion,
    ) {
        initializeOptions(this::class, options)
    }

    override fun historyBasedIcConfigurationBuilder(
        rootProjectDir: Path,
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        modulesInformation: List<IncrementalModule>,
    ): JsHistoryBasedIncrementalCompilationConfiguration.Builder {
        return JsHistoryBasedIncrementalCompilationConfigurationImpl(rootProjectDir, workingDirectory, sourcesChanges, modulesInformation)
    }

    override fun toBuilder(): JsKlibCompilationOperation.Builder = deepCopy()

    override fun deepCopy(): JsKlibCompilationOperationImpl {
        return JsKlibCompilationOperationImpl(
            options.deepCopy(),
            sources,
            destination,
            compilerArguments.deepCopy(),
            buildIdToSessionFlagFile,
            compilerVersion
        )
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JsKlibCompilationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsKlibCompilationOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
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

    override fun createAndPrepareCompilerArguments(): K2JSCompilerArguments =
        compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.outputDir = destination.absolutePathStringOrThrow()
        }

    override val targetPlatform: CompileService.TargetPlatform = CompileService.TargetPlatform.JS

    override fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
        arguments: K2JSCompilerArguments,
    ): IncrementalCompilationOptions? {
        return when (val aggregatedIcConfiguration: JsIncrementalCompilationConfiguration? = get(INCREMENTAL_COMPILATION)) {
            is JsHistoryBasedIncrementalCompilationConfigurationImpl -> {
                val sourcesChanges = aggregatedIcConfiguration.sourcesChanges
                val classpathChanges = ClasspathChanges.NotAvailableForJSCompiler

                IncrementalCompilationOptions(
                    sourcesChanges,
                    classpathChanges = classpathChanges,
                    workingDir = aggregatedIcConfiguration.workingDirectory.toFile(),
                    compilerMode = CompilerMode.INCREMENTAL_COMPILER,
                    targetPlatform = targetPlatform,
                    reportCategories = reportCategories,
                    reportSeverity = reportSeverity,
                    requestedCompilationResults = requestedCompilationResults,
                    outputFiles = aggregatedIcConfiguration[OUTPUT_DIRS]?.map { it.toFile() },
                    multiModuleICSettings = MultiModuleICSettings(
                        aggregatedIcConfiguration.historyFile.toFile(),
                        false
                    ),
                    modulesInfo = aggregatedIcConfiguration.modulesInformation.toIncrementalModuleInfo(
                        aggregatedIcConfiguration[ROOT_PROJECT_BUILD_DIR]
                    ),
                    rootProjectDir = aggregatedIcConfiguration[ROOT_PROJECT_DIR]?.toFile(),
                    buildDir = aggregatedIcConfiguration[MODULE_BUILD_DIR]?.toFile(),
                    icFeatures = aggregatedIcConfiguration.extractIncrementalCompilationFeatures(),
                    useJvmFirRunner = false,
                    generateCompilerRefIndex = get(GENERATE_COMPILER_REF_INDEX),
                    configurationInputs = makeConfigurationInputs(aggregatedIcConfiguration)
                )
            }
            null -> null
            else -> {
                error(
                    "Unexpected incremental compilation configuration: ${aggregatedIcConfiguration::class}. In this version, it must be an instance of JsHistoryBasedIncrementalCompilationConfiguration for incremental compilation, or null for non-incremental compilation."
                )
            }
        }
    }

    private fun makeConfigurationInputs(
        icConfiguration: JsHistoryBasedIncrementalCompilationConfigurationImpl,
    ): ConfigurationInputs? {
        return if (icConfiguration[TRACK_CONFIGURATION_INPUTS]) {
            ConfigurationInputs(
                mapOf(
                    "rootProjectBuildDir" to "${icConfiguration[ROOT_PROJECT_BUILD_DIR]}",
                    "historyFileDir" to "${icConfiguration[HISTORY_FILE_DIR]}",
                    "rootProjectDir" to "${icConfiguration[ROOT_PROJECT_DIR]}",
                    "moduleBuildDir" to "${icConfiguration[MODULE_BUILD_DIR]}",
                    "outputDirs" to "${icConfiguration[OUTPUT_DIRS]}",
                    "unsafeIncrementalCompilationForMultiplatform" to "${icConfiguration[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM]}",
                    "monotonousIncrementalCompileSetExpansion" to "${icConfiguration[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION]}",
                    "kotlinVersion" to compilerVersion,
                ),
                compilerArguments.toCompilationInputs(),
            )
        } else {
            null
        }
    }

    override fun shouldCompileIncrementally(): Boolean {
        return when (val icConfig = get(INCREMENTAL_COMPILATION)) {
            is JsHistoryBasedIncrementalCompilationConfigurationImpl -> {
                true
            }
            null -> { // no IC configuration -> non-incremental compilation
                false
            }
            else -> error(
                "Unexpected incremental compilation configuration: ${icConfig::class}. In this version, it must be an instance of JvmSnapshotBasedIncrementalCompilationConfiguration for incremental compilation, or null for non-incremental compilation."
            )
        }
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

        @Suppress("DEPRECATION")
        check(
            extractKotlinSourcesFromFreeCompilerArguments(
                arguments, DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS, includeJavaSources = false
            ).isEmpty()
        ) {
            "Unexpected Kotlin sources found in free compiler arguments. Only pass sources through `JsKlibCompilationOperation.sources`."
        }

        val kotlinSources = sources.map { it.toFile() }

        val metricsReporter = getMetricsReporter()
        metricsReporter.startMeasureGc()
        val buildReporter = BuildReporter(
            icReporter = BuildToolsApiBuildICReporter(
                kotlinLogger = loggerAdapter.kotlinLogger,
                rootProjectDir = projectDir,
                buildMetricsReporter = metricsReporter,
            ), buildMetricsReporter = metricsReporter
        )

        val icFeatures = icConfiguration.extractIncrementalCompilationFeatures()
        val incrementalCompiler = IncrementalJsCompilerRunner(
            icConfiguration.workingDirectory.toFile(),
            buildReporter,
            icConfiguration.historyFile.toFile(),
            ModulesApiHistoryJs(
                (icConfiguration[ROOT_PROJECT_DIR] ?: icConfiguration.workingDirectory).toFile(),
                icConfiguration.modulesInformation.toIncrementalModuleInfo(
                    icConfiguration[ROOT_PROJECT_BUILD_DIR]
                )
            ),
            CompileScopeExpansionMode.ALWAYS,
            icFeatures
        )

        arguments.incrementalCompilation = true
        logCompilerArguments(loggerAdapter, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))

        val fileLocations = if (projectDir != null && buildDir != null) {
            FileLocations(projectDir, buildDir)
        } else null
        val compilationResult = incrementalCompiler.compile(
            kotlinSources,
            arguments,
            loggerAdapter,
            icConfiguration.sourcesChanges.asChangedFiles,
            fileLocations,
            makeConfigurationInputs(icConfiguration)
        ).asCompilationResult

        metricsReporter.endMeasureGc()
        populateMetricsCollector(metricsReporter)

        return compilationResult
    }


    private fun JsHistoryBasedIncrementalCompilationConfigurationImpl.extractIncrementalCompilationFeatures(): IncrementalCompilationFeatures {
        return IncrementalCompilationFeatures(
            usePreciseJavaTracking = false,
            withAbiSnapshot = false,
            preciseCompilationResultsBackup = this[BACKUP_CLASSES],
            keepIncrementalCompilationCachesInMemory = this[KEEP_IC_CACHES_IN_MEMORY],
            enableUnsafeIncrementalCompilationForMultiplatform = this[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM],
            enableMonotonousIncrementalCompileSetExpansion = this[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION],
        )
    }

    companion object {
        val INCREMENTAL_COMPILATION: Option<JsIncrementalCompilationConfiguration?> = Option("INCREMENTAL_COMPILATION", null)
    }
}

private fun List<IncrementalModule>.toIncrementalModuleInfo(rootProjectBuildDir: Path? = null): IncrementalModuleInfo {
    val map: Map<IncrementalModule, IncrementalModuleEntry> = associateWith {
        IncrementalModuleEntry(
            it.output.absolutePathStringOrThrow(),
            it.name,
            it.buildDir.toFile(),
            (it.buildHistoryDir ?: it.buildDir).resolve(IncrementalCompilerRunner.BUILD_HISTORY_FILE_NAME).toFile(),
            null
        )
    }
    return IncrementalModuleInfo(
        rootProjectBuildDir?.toFile(),
        map.mapKeys { it.key.output.toFile() },
        buildMap {
            map.forEach { (_, it) -> (getOrPut(it.name) { mutableSetOf() } as MutableSet<IncrementalModuleEntry>).add(it) }
        },
        emptyMap(),
        map.mapKeys { it.key.output.toFile() }.toMap(),
        emptyMap()
    )
}

