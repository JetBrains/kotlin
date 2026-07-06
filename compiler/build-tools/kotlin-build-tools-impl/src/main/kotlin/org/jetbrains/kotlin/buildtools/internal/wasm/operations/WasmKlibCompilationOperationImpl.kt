/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal.wasm.operations

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.endMeasureGc
import org.jetbrains.kotlin.build.report.metrics.startMeasureGc
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.wasm.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.wasm.WasmHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.wasm.WasmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.TRACK_CONFIGURATION_INPUTS
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM
import org.jetbrains.kotlin.buildtools.internal.arguments.WasmArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.buildtools.internal.wasm.WasmHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.WasmHistoryBasedIncrementalCompilationConfigurationImpl.Companion.HISTORY_FILE_DIR
import org.jetbrains.kotlin.buildtools.internal.wasm.WasmHistoryBasedIncrementalCompilationConfigurationImpl.Companion.ROOT_PROJECT_BUILD_DIR
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.js.KotlinWasmCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJs
import org.jetbrains.kotlin.incremental.storage.FileLocations
import java.nio.file.Path

internal class WasmKlibCompilationOperationImpl private constructor(
    override val options: Options = Options(WasmKlibCompilationOperation::class),
    override val sources: List<Path>,
    override val destination: Path,
    compilerArguments: WasmArgumentsImpl = WasmArgumentsImpl(),
    private val compilerVersion: String,
) : BaseCompilationOperationImpl<WasmArgumentsImpl, KotlinWasmCompilerArguments>(compilerArguments),
    WasmKlibCompilationOperation, WasmKlibCompilationOperation.Builder,
    DeepCopyable<WasmKlibCompilationOperationImpl> {
    constructor(
        sources: List<Path>,
        destination: Path,
        compilerArguments: WasmArgumentsImpl = WasmArgumentsImpl(),
        compilerVersion: String,
    ) : this(
        options = Options(WasmKlibCompilationOperation::class),
        sources = sources,
        destination = destination,
        compilerArguments = compilerArguments,
        compilerVersion = compilerVersion,
    ) {
        initializeOptions(this::class, options)
    }

    override fun historyBasedIcConfigurationBuilder(
        rootProjectDir: Path,
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        modulesInformation: List<IncrementalModule>,
    ): WasmHistoryBasedIncrementalCompilationConfiguration.Builder {
        return WasmHistoryBasedIncrementalCompilationConfigurationImpl(rootProjectDir, workingDirectory, sourcesChanges, modulesInformation)
    }

    override fun toBuilder(): WasmKlibCompilationOperation.Builder = deepCopy()

    override fun deepCopy(): WasmKlibCompilationOperationImpl {
        return WasmKlibCompilationOperationImpl(
            options.deepCopy(),
            sources,
            destination,
            compilerArguments.deepCopy(),
            compilerVersion
        )
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: WasmKlibCompilationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: WasmKlibCompilationOperation.Option<V>, value: V) {
        options[key] = value
    }

    override fun build(): WasmKlibCompilationOperation = deepCopy()

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    override fun getRootProjectDir(): Path? {
        return (get(INCREMENTAL_COMPILATION) as? WasmHistoryBasedIncrementalCompilationConfigurationImpl)?.get(ROOT_PROJECT_DIR)
    }

    override fun createAndPrepareCompilerArguments(): KotlinWasmCompilerArguments =
        compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.outputDir = destination.absolutePathStringOrThrow()
        }

    override val targetPlatform: CompileService.TargetPlatform = CompileService.TargetPlatform.WASM

    override fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
        arguments: KotlinWasmCompilerArguments,
    ): IncrementalCompilationOptions? {
        return when (val aggregatedIcConfiguration: WasmIncrementalCompilationConfiguration? = get(INCREMENTAL_COMPILATION)) {
            is WasmHistoryBasedIncrementalCompilationConfigurationImpl -> {
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
                    "Unexpected incremental compilation configuration: ${aggregatedIcConfiguration::class}. In this version, it must be an instance of ${WasmHistoryBasedIncrementalCompilationConfiguration::class} for incremental compilation, or null for non-incremental compilation."
                )
            }
        }
    }

    private fun makeConfigurationInputs(
        icConfiguration: WasmHistoryBasedIncrementalCompilationConfigurationImpl,
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
            is WasmHistoryBasedIncrementalCompilationConfiguration -> {
                true
            }
            null -> { // no IC configuration -> non-incremental compilation
                false
            }
            else -> error(
                "Unexpected incremental compilation configuration: ${icConfig::class}. In this version, it must be an instance of ${WasmHistoryBasedIncrementalCompilationConfiguration::class} for incremental compilation, or null for non-incremental compilation."
            )
        }
    }

    override fun createCompiler(): CLICompiler<KotlinWasmCompilerArguments> {
        return KotlinWasmCompiler()
    }

    override fun KotlinWasmCompilerArguments.addSources() {
        freeArgs += sources.map { it.absolutePathStringOrThrow() }
    }

    override fun compileIncrementallyInProcess(
        arguments: KotlinWasmCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        val icConfiguration = get(INCREMENTAL_COMPILATION)
        requireNotNull(icConfiguration) { "Missing INCREMENTAL_COMPILATION option." }
        check(icConfiguration is WasmHistoryBasedIncrementalCompilationConfigurationImpl) {
            "Unexpected incremental compilation configuration: ${icConfiguration::class}. In this version, it must be an instance of WasmHistoryBasedIncrementalCompilationConfiguration for incremental compilation, or null for non-incremental compilation."
        }

        val projectDir = icConfiguration[ROOT_PROJECT_DIR]?.toFile()
        val buildDir = icConfiguration[MODULE_BUILD_DIR]?.toFile()

        @Suppress("DEPRECATION")
        check(
            extractKotlinSourcesFromFreeCompilerArguments(
                arguments, DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS, includeJavaSources = false
            ).isEmpty()
        ) {
            "Unexpected Kotlin sources found in free compiler arguments. Only pass sources through `WasmKlibCompilationOperation.sources`."
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


    private fun WasmHistoryBasedIncrementalCompilationConfigurationImpl.extractIncrementalCompilationFeatures(): IncrementalCompilationFeatures {
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
        val INCREMENTAL_COMPILATION: Option<WasmIncrementalCompilationConfiguration?> = Option("INCREMENTAL_COMPILATION", null)
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
            map.forEach { [_, it] -> (getOrPut(it.name) { mutableSetOf() } as MutableSet<IncrementalModuleEntry>).add(it) }
        },
        emptyMap(),
        map.mapKeys { it.key.output.toFile() }.toMap(),
        emptyMap()
    )
}

