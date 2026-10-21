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
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorWithDiagnosticId
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJs
import org.jetbrains.kotlin.incremental.storage.FileLocations
import java.nio.file.Path

internal class JsKlibCompilationOperationImpl private constructor(
    override val options: Options = Options(JsKlibCompilationOperation::class),
    override val sources: List<Path>,
    override val destination: Path,
    override val compilerArguments: JsArgumentsImpl = JsArgumentsImpl(),
    private val compilerVersion: String,
) : BaseCompilationOperationImpl<JsArgumentsImpl, K2JSCompilerArguments>(),
    JsKlibCompilationOperation, JsKlibCompilationOperation.Builder,
    DeepCopyable<JsKlibCompilationOperationImpl> {
    constructor(
        sources: List<Path>,
        destination: Path,
        compilerArguments: JsArgumentsImpl = JsArgumentsImpl(),
        compilerVersion: String,
    ) : this(
        options = Options(JsKlibCompilationOperation::class),
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
    ): JsHistoryBasedIncrementalCompilationConfiguration.Builder {
        return JsHistoryBasedIncrementalCompilationConfigurationImpl(workingDirectory, sourcesChanges, modulesInformation).also {
            it.rootProjectDir = rootProjectDir
        }
    }

    override fun toBuilder(): JsKlibCompilationOperation.Builder = deepCopy()

    override fun deepCopy(): JsKlibCompilationOperationImpl {
        return JsKlibCompilationOperationImpl(
            options.deepCopy(),
            sources,
            destination,
            compilerArguments.deepCopy(),
            compilerVersion
        ).also { it.copyFrom(this) }
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

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    override fun getRootProjectDir(): Path? {
        return (get(INCREMENTAL_COMPILATION) as? JsHistoryBasedIncrementalCompilationConfigurationImpl)?.rootProjectDir
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
                    outputFiles = aggregatedIcConfiguration.outputDirs?.map { it.toFile() },
                    multiModuleICSettings = MultiModuleICSettings(
                        aggregatedIcConfiguration.historyFile.toFile(),
                        false
                    ),
                    modulesInfo = aggregatedIcConfiguration.modulesInformation.toIncrementalModuleInfo(
                        aggregatedIcConfiguration.rootProjectBuildDir
                    ),
                    rootProjectDir = aggregatedIcConfiguration.rootProjectDir?.toFile(),
                    buildDir = aggregatedIcConfiguration.moduleBuildDir?.toFile(),
                    icFeatures = aggregatedIcConfiguration.extractIncrementalCompilationFeatures(),
                    useJvmFirRunner = false,
                    generateCompilerRefIndex = generateCompilerRefIndex,
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
        return if (icConfiguration.trackConfigurationInputs) {
            ConfigurationInputs(
                mapOf(
                    "rootProjectBuildDir" to "${icConfiguration.rootProjectBuildDir}",
                    "historyFileDir" to "${icConfiguration.historyFileDir}",
                    "rootProjectDir" to "${icConfiguration.rootProjectDir}",
                    "moduleBuildDir" to "${icConfiguration.moduleBuildDir}",
                    "outputDirs" to "${icConfiguration.outputDirs}",
                    "unsafeIncrementalCompilationForMultiplatform" to "${icConfiguration.unsafeIncrementalCompilationForMultiplatform}",
                    "monotonousIncrementalCompileSetExpansion" to "${icConfiguration.monotonousIncrementalCompileSetExpansion}",
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
                "Unexpected incremental compilation configuration: ${icConfig::class}. In this version, it must be an instance of ${JsHistoryBasedIncrementalCompilationConfigurationImpl::class} for incremental compilation, or null for non-incremental compilation."
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
        logger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId,
        executionContext: ExecutionContext
    ): CompilationResult {
        val icConfiguration = get(INCREMENTAL_COMPILATION)
        requireNotNull(icConfiguration) { "Missing INCREMENTAL_COMPILATION option." }
        check(icConfiguration is JsHistoryBasedIncrementalCompilationConfigurationImpl) {
            "Unexpected incremental compilation configuration: ${icConfiguration::class}. In this version, it must be an instance of JsHistoryBasedIncrementalCompilationConfiguration for incremental compilation, or null for non-incremental compilation."
        }

        val projectDir = icConfiguration.rootProjectDir?.toFile()
        val buildDir = icConfiguration.moduleBuildDir?.toFile()

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
                kotlinLogger = logger,
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
                (icConfiguration.rootProjectDir ?: icConfiguration.workingDirectory).toFile(),
                icConfiguration.modulesInformation.toIncrementalModuleInfo(
                    icConfiguration.rootProjectBuildDir
                )
            ),
            CompileScopeExpansionMode.ALWAYS,
            icFeatures,
            executionContext.classloadersCache?.asPluginsLoader(),
            getLookupTrackerAdapter()
        )

        arguments.incrementalCompilation = true
        logCompilerArguments(logger, arguments, compilerArgumentsLogLevel)

        val fileLocations = if (projectDir != null && buildDir != null) {
            FileLocations(projectDir, buildDir)
        } else null
        val compilationResult = incrementalCompiler.compile(
            kotlinSources,
            arguments,
            messageCollector,
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
            preciseCompilationResultsBackup = this.backupClasses,
            keepIncrementalCompilationCachesInMemory = this.keepIcCachesInMemory,
            enableUnsafeIncrementalCompilationForMultiplatform = this.unsafeIncrementalCompilationForMultiplatform,
            enableMonotonousIncrementalCompileSetExpansion = this.monotonousIncrementalCompileSetExpansion,
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
    )
}

