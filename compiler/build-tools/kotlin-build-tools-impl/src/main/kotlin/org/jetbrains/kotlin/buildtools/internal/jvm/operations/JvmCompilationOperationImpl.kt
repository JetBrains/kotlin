/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.endMeasureGc
import org.jetbrains.kotlin.build.report.metrics.startMeasureGc
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.TRACK_CONFIGURATION_INPUTS
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonCompilerArgumentsImpl.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonCompilerArgumentsImpl.Companion.X_USE_FIR_IC
import org.jetbrains.kotlin.buildtools.internal.arguments.JvmCompilerArgumentValueAdapter
import org.jetbrains.kotlin.buildtools.internal.arguments.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.jvm.HasSnapshotBasedIcOptionsAccessor
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.internal.jvm.toOptions
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.storage.FileLocations
import java.io.File
import java.nio.file.Path

internal class JvmCompilationOperationImpl private constructor(
    override val options: Options = Options(JvmCompilationOperation::class),
    override val sources: List<Path>,
    override val destinationDirectory: Path,
    compilerArguments: JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(JvmCompilerArgumentValueAdapter.getOrNull()),
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
    private val compilerVersion: String,
) : BaseCompilationOperationImpl<JvmCompilerArgumentsImpl, K2JVMCompilerArguments>(compilerArguments, buildIdToSessionFlagFile),
    JvmCompilationOperation,
    JvmCompilationOperation.Builder,
    DeepCopyable<JvmCompilationOperationImpl> {

    constructor(
        sources: List<Path>,
        destinationDirectory: Path,
        compilerArguments: JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(JvmCompilerArgumentValueAdapter.getOrNull()),
        buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
        compilerVersion: String,
    ) : this(
        options = Options(JvmCompilationOperation::class),
        sources = sources,
        destinationDirectory = destinationDirectory,
        compilerArguments = compilerArguments,
        buildIdToSessionFlagFile = buildIdToSessionFlagFile,
        compilerVersion = compilerVersion,
    ) {
        initializeOptions(this::class, options)
    }

    override val targetPlatform: CompileService.TargetPlatform = CompileService.TargetPlatform.JVM

    override fun toBuilder(): JvmCompilationOperation.Builder = deepCopy()

    override fun deepCopy(): JvmCompilationOperationImpl {
        return JvmCompilationOperationImpl(
            options.deepCopy(),
            sources,
            destinationDirectory,
            compilerArguments.deepCopy(),
            buildIdToSessionFlagFile,
            compilerVersion,
        )
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmCompilationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmCompilationOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    override fun build(): JvmCompilationOperation = deepCopy()

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    @Deprecated("Use `snapshotBasedIcConfigurationBuilder` instead.")
    @Suppress("DEPRECATION_ERROR")
    fun createSnapshotBasedIcOptions(): org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions {
        return JvmSnapshotBasedIncrementalCompilationOptionsImpl()
    }

    @Deprecated(
        "The shrunkClasspathSnapshot parameter is no longer required.",
        replaceWith = ReplaceWith("snapshotBasedIcConfigurationBuilder(workingDirectory, sourcesChanges, dependenciesSnapshotFiles)"),
        level = DeprecationLevel.WARNING
    )
    override fun snapshotBasedIcConfigurationBuilder(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        dependenciesSnapshotFiles: List<Path>,
        shrunkClasspathSnapshot: Path,
    ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder {
        return JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
            workingDirectory,
            sourcesChanges,
            dependenciesSnapshotFiles,
            shrunkClasspathSnapshot
        )
    }

    override fun snapshotBasedIcConfigurationBuilder(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        dependenciesSnapshotFiles: List<Path>,
    ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder {
        return JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
            workingDirectory,
            sourcesChanges,
            dependenciesSnapshotFiles,
            /**
             * The filename "shrunk-classpath-snapshot.bin" is a placeholder.
             * ClasspathSnapshotFiles uses only the parent directory (workingDirectory) to create the actual file.
             * This logic will be cleaned up with KT-83937.
             */
            workingDirectory.resolve("shrunk-classpath-snapshot.bin")
        )
    }

    private fun getKotlinFilenameExtensions(): Set<String> =
        DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + (get(KOTLINSCRIPT_EXTENSIONS) ?: emptyArray())

    override fun getRootProjectDir(): Path? {
        return (get(INCREMENTAL_COMPILATION) as? JvmSnapshotBasedIncrementalCompilationConfiguration)?.toOptions()?.get(ROOT_PROJECT_DIR)
    }

    override fun createAndPrepareCompilerArguments(): K2JVMCompilerArguments =
        compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.destination = destinationDirectory.absolutePathStringOrThrow()
        }

    override fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
        arguments: K2JVMCompilerArguments,
    ): IncrementalCompilationOptions? {
        val aggregatedIcConfigurationOptions = getIcOptionsAccessorOrNull() ?: return null

        val sourcesChanges = aggregatedIcConfigurationOptions.sourcesChanges
        val classpathChanges = aggregatedIcConfigurationOptions.classpathChanges
        if (aggregatedIcConfigurationOptions[USE_FIR_RUNNER]) {
            checkJvmFirRequirements(compilerArguments)
        }
        return IncrementalCompilationOptions(
            sourcesChanges,
            classpathChanges = classpathChanges,
            workingDir = aggregatedIcConfigurationOptions.workingDirectory.toFile(),
            compilerMode = CompilerMode.INCREMENTAL_COMPILER,
            targetPlatform = targetPlatform,
            reportCategories = reportCategories,
            reportSeverity = reportSeverity,
            requestedCompilationResults = requestedCompilationResults,
            outputFiles = aggregatedIcConfigurationOptions[OUTPUT_DIRS]?.map { it.toFile() },
            multiModuleICSettings = null, // required only for the build history approach
            modulesInfo = null, // required only for the build history approach
            rootProjectDir = aggregatedIcConfigurationOptions[ROOT_PROJECT_DIR]?.toFile(),
            buildDir = aggregatedIcConfigurationOptions[MODULE_BUILD_DIR]?.toFile(),
            kotlinScriptExtensions = get(KOTLINSCRIPT_EXTENSIONS),
            icFeatures = aggregatedIcConfigurationOptions.extractIncrementalCompilationFeatures(),
            useJvmFirRunner = aggregatedIcConfigurationOptions[USE_FIR_RUNNER],
            generateCompilerRefIndex = get(GENERATE_COMPILER_REF_INDEX),
            configurationInputs = makeConfigurationInputs(
                aggregatedIcConfigurationOptions,
                getEffectivePreciseJavaTrackingState(
                    aggregatedIcConfigurationOptions,
                    arguments,
                ),
            )
        )
    }

    override fun shouldCompileIncrementally(): Boolean {
        return getIcOptionsAccessorOrNull()?.let { true } ?: false
    }

    override fun compileInProcess(loggerAdapter: KotlinLoggerMessageCollectorAdapter): CompilationResult {
        setupIdeaStandaloneExecution()
        return super.compileInProcess(loggerAdapter)
    }

    override fun createCompiler(): CLICompiler<K2JVMCompilerArguments> {
        return K2JVMCompiler()
    }

    override fun K2JVMCompilerArguments.addSources() {
        freeArgs += sources.map { it.absolutePathStringOrThrow() }
    }

    override fun compileIncrementallyInProcess(
        arguments: K2JVMCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        val snapshotBasedIcOptionsAccessor = getIcOptionsAccessorOrNull() ?: error("Missing INCREMENTAL_COMPILATION option.")
        arguments.freeArgs += sources.filter { it.toFile().isJavaFile() }.map { it.absolutePathStringOrThrow() }

        val projectDir = snapshotBasedIcOptionsAccessor[ROOT_PROJECT_DIR]?.toFile()
        val buildDir = snapshotBasedIcOptionsAccessor[MODULE_BUILD_DIR]?.toFile()

        @Suppress("DEPRECATION") val kotlinSources = extractKotlinSourcesFromFreeCompilerArguments(
            arguments, DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS, includeJavaSources = true
        ) + sources.map { it.toFile() }

        val classpathChanges = snapshotBasedIcOptionsAccessor.classpathChanges
        val metricsReporter = getMetricsReporter()
        metricsReporter.startMeasureGc()
        val buildReporter = BuildReporter(
            icReporter = BuildToolsApiBuildICReporter(
                kotlinLogger = loggerAdapter.kotlinLogger,
                rootProjectDir = projectDir,
                buildMetricsReporter = metricsReporter,
            ), buildMetricsReporter = metricsReporter
        )
        val verifiedPreciseJavaTracking = getEffectivePreciseJavaTrackingState(snapshotBasedIcOptionsAccessor, arguments)
        val icFeatures = snapshotBasedIcOptionsAccessor.extractIncrementalCompilationFeatures().copy(
            usePreciseJavaTracking = verifiedPreciseJavaTracking
        )
        val incrementalCompiler = if (snapshotBasedIcOptionsAccessor[USE_FIR_RUNNER] && checkJvmFirRequirements(compilerArguments)) {
            getFirRunner(
                snapshotBasedIcOptionsAccessor.workingDirectory,
                buildReporter,
                snapshotBasedIcOptionsAccessor,
                classpathChanges,
                getKotlinFilenameExtensions(),
                icFeatures
            )
        } else {
            getNonFirRunner(
                snapshotBasedIcOptionsAccessor.workingDirectory,
                buildReporter,
                snapshotBasedIcOptionsAccessor,
                classpathChanges,
                getKotlinFilenameExtensions(),
                icFeatures
            )
        }

        arguments.incrementalCompilation = true
        logCompilerArguments(loggerAdapter, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))

        val fileLocations = if (projectDir != null && buildDir != null) {
            FileLocations(projectDir, buildDir)
        } else null
        val configurationInputs =
            makeConfigurationInputs(snapshotBasedIcOptionsAccessor, verifiedPreciseJavaTracking)
        val compilationResult = incrementalCompiler.compile(
            kotlinSources,
            arguments,
            loggerAdapter,
            snapshotBasedIcOptionsAccessor.sourcesChanges.asChangedFiles,
            fileLocations,
            configurationInputs,
        ).asCompilationResult

        metricsReporter.endMeasureGc()
        populateMetricsCollector(metricsReporter)

        return compilationResult
    }

    private fun getIcOptionsAccessorOrNull(): HasSnapshotBasedIcOptionsAccessor? = get(INCREMENTAL_COMPILATION)?.let { icConfiguration ->
        check(icConfiguration is JvmSnapshotBasedIncrementalCompilationConfiguration) {
            "Unexpected incremental compilation configuration: ${icConfiguration::class}. In this version, it must be an instance of JvmSnapshotBasedIncrementalCompilationConfiguration for incremental compilation, or null for non-incremental compilation."
        }
        icConfiguration.toOptions()
    }

    private fun getEffectivePreciseJavaTrackingState(
        icConfiguration: HasSnapshotBasedIcOptionsAccessor,
        arguments: K2JVMCompilerArguments,
    ): Boolean {
        return arguments.disablePreciseJavaTrackingIfK2(usePreciseJavaTrackingByDefault = icConfiguration[PRECISE_JAVA_TRACKING])
    }

    private fun makeConfigurationInputs(
        icConfiguration: HasSnapshotBasedIcOptionsAccessor,
        verifiedPreciseJavaTracking: Boolean,
    ): ConfigurationInputs? {
        return if (icConfiguration[TRACK_CONFIGURATION_INPUTS]) {
            ConfigurationInputs(
                mapOf(
                    "rootProjectDir" to "${icConfiguration[ROOT_PROJECT_DIR]}",
                    "moduleBuildDir" to "${icConfiguration[MODULE_BUILD_DIR]}",
                    "outputDirs" to "${icConfiguration[OUTPUT_DIRS]}",
                    "useFirRunner" to "${icConfiguration[USE_FIR_RUNNER]}",
                    "unsafeIncrementalCompilationForMultiplatform" to "${icConfiguration[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM]}",
                    "monotonousIncrementalCompileSetExpansion" to "${icConfiguration[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION]}",
                    "usePreciseJavaTracking" to "$verifiedPreciseJavaTracking",
                    "kotlinSourceFileExtensions" to getKotlinFilenameExtensions().sorted().joinToString(","),
                    "kotlinVersion" to compilerVersion,
                ),
                compilerArguments.toCompilationInputs(),
            )
        } else {
            null
        }
    }

    private fun JvmCompilationOperationImpl.getNonFirRunner(
        workingDirectory: Path,
        buildReporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
        aggregatedIcConfigurationOptions: HasSnapshotBasedIcOptionsAccessor,
        classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled,
        kotlinFilenameExtensions: Set<String>,
        icFeatures: IncrementalCompilationFeatures,
    ): IncrementalJvmCompilerRunner =
        IncrementalJvmCompilerRunner(
            workingDirectory.toFile(),
            buildReporter,
            outputDirs = aggregatedIcConfigurationOptions[OUTPUT_DIRS]?.map { it.toFile() },
            classpathChanges = classpathChanges,
            kotlinSourceFilesExtensions = kotlinFilenameExtensions,
            icFeatures = icFeatures,
            compilationCanceledStatus = cancellationHandle,
            generateCompilerRefIndex = get(GENERATE_COMPILER_REF_INDEX),
            lookupTrackerDelegate = getLookupTrackerAdapter(),
        )

    private fun JvmCompilationOperationImpl.getFirRunner(
        workingDirectory: Path,
        buildReporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
        aggregatedIcConfigurationOptions: HasSnapshotBasedIcOptionsAccessor,
        classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled,
        kotlinFilenameExtensions: Set<String>,
        icFeatures: IncrementalCompilationFeatures,
    ): IncrementalFirJvmCompilerRunner =
        IncrementalFirJvmCompilerRunner(
            workingDirectory.toFile(),
            buildReporter,
            outputDirs = aggregatedIcConfigurationOptions[OUTPUT_DIRS]?.map { it.toFile() },
            classpathChanges = classpathChanges,
            kotlinSourceFilesExtensions = kotlinFilenameExtensions,
            icFeatures = icFeatures,
            compilationCanceledStatus = cancellationHandle,
            generateCompilerRefIndex = get(GENERATE_COMPILER_REF_INDEX),
            lookupTrackerDelegate = getLookupTrackerAdapter(),
        )


    companion object {
        val INCREMENTAL_COMPILATION: Option<JvmIncrementalCompilationConfiguration?> = Option("INCREMENTAL_COMPILATION", null)

        val KOTLINSCRIPT_EXTENSIONS: Option<Array<String>?> = Option("KOTLINSCRIPT_EXTENSIONS", null)
    }
}

private fun checkJvmFirRequirements(
    arguments: JvmCompilerArgumentsImpl,
): Boolean {
    val languageVersion = if (LANGUAGE_VERSION in arguments) {
        arguments[LANGUAGE_VERSION]
    } else {
        null
    }?.let {
        LanguageVersion.fromVersionString(it.stringValue)
    } ?: LanguageVersion.LATEST_STABLE

    check(languageVersion >= LanguageVersion.KOTLIN_2_0) {
        "FIR incremental compiler runner is only compatible with Kotlin Language Version 2.0"
    }
    check(X_USE_FIR_IC in arguments && arguments[X_USE_FIR_IC]) {
        "FIR incremental compiler runner requires '-Xuse-fir-ic' to be present in arguments"
    }

    return true
}
