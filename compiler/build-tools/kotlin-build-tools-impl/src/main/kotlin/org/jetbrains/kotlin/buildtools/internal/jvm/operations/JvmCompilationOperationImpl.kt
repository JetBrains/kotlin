/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.CompositeICReporter
import org.jetbrains.kotlin.build.report.events.EventICReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.endMeasureGc
import org.jetbrains.kotlin.build.report.metrics.startMeasureGc
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
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
import org.jetbrains.kotlin.buildtools.internal.arguments.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.jps.JvmClientManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.jps.createCompilerServicesFacadeBase
import org.jetbrains.kotlin.buildtools.internal.jps.getClientManagedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.internal.jps.registerPlatformServices
import org.jetbrains.kotlin.buildtools.internal.jvm.HasSnapshotBasedIcOptionsAccessor
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.internal.jvm.KaptConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.KaptConfigurationImpl.Companion.SOURCE_OUTPUT_DIR
import org.jetbrains.kotlin.buildtools.internal.jvm.toOptions
import org.jetbrains.kotlin.buildtools.internal.serializability.BtaSerializable
import org.jetbrains.kotlin.buildtools.internal.serializability.CompilationResultSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.LookupMessageVisitor
import org.jetbrains.kotlin.buildtools.internal.serializability.Message
import org.jetbrains.kotlin.buildtools.internal.serializability.MessageVisitor
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.cast
import org.jetbrains.kotlin.buildtools.internal.serializability.throwPropertyNotFound
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorWithDiagnosticId
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.storage.FileLocations
import java.nio.file.Path

@Serializable
internal class JvmCompilationOperationImpl : BaseCompilationOperationImpl<JvmCompilerArgumentsImpl, @Contextual K2JVMCompilerArguments>,
    JvmCompilationOperation,
    JvmCompilationOperation.Builder,
    DeepCopyable<JvmCompilationOperationImpl> {

    override val sources: List<Path>
    override val destinationDirectory: Path
    override val compilerArguments: JvmCompilerArgumentsImpl
    private val compilerVersion: String

    private constructor(other: JvmCompilationOperationImpl) : super(other) {
        sources = other.sources.toList()
        destinationDirectory = other.destinationDirectory
        compilerArguments = other.compilerArguments.deepCopy()
        compilerVersion = other.compilerVersion
        @Suppress("UNCHECKED_CAST")
        incrementalCompilation = (other.incrementalCompilation as? DeepCopyable<JvmIncrementalCompilationConfiguration>)?.deepCopy()
            ?: other.incrementalCompilation
        kotlinScriptExtensions = other.kotlinScriptExtensions?.copyOf()
        kaptConfiguration = other.kaptConfiguration?.deepCopy()
    }

    constructor(
        sources: List<Path>,
        destinationDirectory: Path,
        compilerArguments: JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(),
        compilerVersion: String,
    ) : super() {
        this.sources = sources
        this.destinationDirectory = destinationDirectory
        this.compilerArguments = compilerArguments
        this.compilerVersion = compilerVersion
    }

    internal var incrementalCompilation: JvmIncrementalCompilationConfiguration? = null

    internal var kotlinScriptExtensions: Array<String>? = null

    @Transient // this is converted and added to compiler arguments before sending to daemon
    internal var kaptConfiguration: KaptConfigurationImpl? = null

    override val targetPlatform: CompileService.TargetPlatform = CompileService.TargetPlatform.JVM

    override fun toBuilder(): JvmCompilationOperation.Builder = deepCopy()

    override fun beforeSerialization(logger: KotlinLogger): List<MessageVisitor> = buildList {
        addAll(super.beforeSerialization(logger))
        (incrementalCompilation as? BtaSerializable)?.let { incrementalCompilation ->
            addAll(incrementalCompilation.beforeSerialization(logger))
            // special case for JPS duplicating lookupTracker in its IC configuration
            val lookupVisitorsToIgnore = filterIsInstance<LookupMessageVisitor>().dropLast(1)
            removeAll(lookupVisitorsToIgnore)
        }
    }

    override fun afterDeserialization(messageReporter: (Message) -> Unit, compilerFacade: CompilerServicesFacadeBase?) {
        super.afterDeserialization(messageReporter, compilerFacade)
        (incrementalCompilation as? BtaSerializable)?.afterDeserialization(messageReporter, compilerFacade)
    }

    override fun deepCopy(): JvmCompilationOperationImpl {
        return JvmCompilationOperationImpl(this)
    }

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmCompilationOperation.Option<V>): V = when (key.id) {
        "INCREMENTAL_COMPILATION" -> incrementalCompilation
        "KOTLINSCRIPT_EXTENSIONS" -> kotlinScriptExtensions
        "LOOKUP_TRACKER" -> lookupTracker
        "GENERATE_COMPILER_REF_INDEX" -> generateCompilerRefIndex
        "COMPILER_ARGUMENTS_LOG_LEVEL" -> compilerArgumentsLogLevel
        "KAPT_CONFIGURATION" -> kaptConfiguration
        else -> throwPropertyNotFound(key.id)
    } as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmCompilationOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        when (key.id) {
            "INCREMENTAL_COMPILATION" -> incrementalCompilation = value.cast()
            "KOTLINSCRIPT_EXTENSIONS" -> kotlinScriptExtensions = value.cast()
            "LOOKUP_TRACKER" -> lookupTracker = value.cast()
            "GENERATE_COMPILER_REF_INDEX" -> generateCompilerRefIndex = value.cast()
            "COMPILER_ARGUMENTS_LOG_LEVEL" -> compilerArgumentsLogLevel = value.cast()
            "KAPT_CONFIGURATION" -> kaptConfiguration = value.cast()
            else -> throwPropertyNotFound(key.id)
        }
    }

    override fun build(): JvmCompilationOperation = deepCopy()

    @Deprecated("Use `snapshotBasedIcConfigurationBuilder` instead.")
    @Suppress("DEPRECATION_ERROR")
    fun createSnapshotBasedIcOptions(): org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions {
        return JvmSnapshotBasedIncrementalCompilationOptionsImpl()
    }

    @Deprecated(
        "The shrunkClasspathSnapshot parameter is no longer required.",
        replaceWith = ReplaceWith("snapshotBasedIcConfigurationBuilder(workingDirectory, sourcesChanges, dependenciesSnapshotFiles)"),
        level = DeprecationLevel.ERROR
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
             * This logic will be cleaned up with KT-88357.
             */
            workingDirectory.resolve("shrunk-classpath-snapshot.bin")
        )
    }

    private fun getKotlinFilenameExtensions(): Set<String> =
        DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + (kotlinScriptExtensions ?: emptyArray())

    override fun getRootProjectDir(): Path? {
        return (incrementalCompilation as? JvmSnapshotBasedIncrementalCompilationConfiguration)?.toOptions()?.get(ROOT_PROJECT_DIR)
    }

    override fun createAndPrepareCompilerArguments(): K2JVMCompilerArguments =
        compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.destination = destinationDirectory.absolutePathStringOrThrow()
            compilerArguments.incrementalCompilation = incrementalCompilation != null
            addKaptCompilerPluginOptions(
                (getIcOptionsOrNull(
                    emptyArray(),
                    0,
                    emptyArray(),
                    K2JVMCompilerArguments()
                ) as? IncrementalCompilationOptions)?.workingDir?.toPath()
            )
        }

    override fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
        arguments: K2JVMCompilerArguments,
    ): CompilationOptions? {
        val icConfiguration = incrementalCompilation ?: return null

        return when (icConfiguration) {
            is JvmSnapshotBasedIncrementalCompilationConfiguration -> getSnapshotBasedIncrementalCompilationOptions(
                reportCategories,
                reportSeverity,
                requestedCompilationResults,
                arguments
            )
            is JvmClientManagedIncrementalCompilationConfiguration -> icConfiguration.getClientManagedIncrementalCompilationOptions(
                reportCategories,
                reportSeverity,
            )
            else -> error("Unexpected incremental compilation configuration: ${icConfiguration::class}")
        }
    }

    override fun createCompilerServicesFacade(
        logger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId,
    ): CompilerServicesFacadeBase {
        val icConfiguration = incrementalCompilation
        if (icConfiguration is JvmClientManagedIncrementalCompilationConfiguration) {
            return icConfiguration.createCompilerServicesFacadeBase(logger, messageCollector, cancellationHandle)
        }

        return super.createCompilerServicesFacade(logger, messageCollector)
    }

    private fun getSnapshotBasedIncrementalCompilationOptions(
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
            kotlinScriptExtensions = kotlinScriptExtensions,
            icFeatures = aggregatedIcConfigurationOptions.extractIncrementalCompilationFeatures(),
            useJvmFirRunner = aggregatedIcConfigurationOptions[USE_FIR_RUNNER],
            generateCompilerRefIndex = generateCompilerRefIndex,
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
        return when (val icConfiguration = incrementalCompilation) {
            is JvmSnapshotBasedIncrementalCompilationConfiguration -> true
            null, is JvmClientManagedIncrementalCompilationConfiguration -> false
            else -> error("Unexpected incremental compilation configuration: ${icConfiguration::class}")
        }
    }

    override fun compileInProcess(
        logger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId,
        executionContext: ExecutionContext,
    ): CompilationResult {
        setupIdeaStandaloneExecution()
        return super.compileInProcess(logger, messageCollector, executionContext)
    }

    override fun createCompiler(): CLICompiler<K2JVMCompilerArguments> {
        return K2JVMCompiler()
    }

    override fun K2JVMCompilerArguments.addSources() {
        freeArgs += sources.map { it.absolutePathStringOrThrow() }
    }

    override fun compileIncrementallyInProcess(
        arguments: K2JVMCompilerArguments,
        logger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId,
        executionContext: ExecutionContext,
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
        val icReporter = BuildToolsApiBuildICReporter(
            kotlinLogger = logger,
            rootProjectDir = projectDir,
            buildMetricsReporter = metricsReporter,
        )
        val icEventCollectorFinal = icEventCollector
        val eventReporter = if (icEventCollectorFinal != null) EventICReporter() else null
        val buildReporter = BuildReporter(
            icReporter = if (eventReporter != null) {
                CompositeICReporter(listOf(icReporter, eventReporter))
            } else {
                icReporter
            },
            buildMetricsReporter = metricsReporter
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
                icFeatures,
                executionContext,
            )
        }

        logCompilerArguments(logger, arguments, compilerArgumentsLogLevel)

        val fileLocations = if (projectDir != null && buildDir != null) {
            FileLocations(projectDir, buildDir)
        } else null
        val configurationInputs =
            makeConfigurationInputs(snapshotBasedIcOptionsAccessor, verifiedPreciseJavaTracking)
        val compilationResult = try {
            incrementalCompiler.compile(
                kotlinSources,
                arguments,
                messageCollector,
                snapshotBasedIcOptionsAccessor.sourcesChanges.asChangedFiles,
                fileLocations,
                configurationInputs,
            )
        } finally {
            runCatching {
                eventReporter?.let { reporter ->
                    icEventCollectorFinal?.collectEvents(reporter.events.toIcEvents())
                }
            }.onFailure { collectionError ->
                logger.warn(
                    "Failed to collect incremental compilation events:\n${collectionError.stackTraceToString()}"
                )
            }
        }.asCompilationResult

        metricsReporter.endMeasureGc()
        populateMetricsCollector(metricsReporter)

        return compilationResult
    }

    private fun addKaptCompilerPluginOptions(workingDirectory: Path? = null) {
        kaptConfiguration?.let { kaptConfiguration ->
            val compilerPlugins = compilerArguments.compilerPlugins.toMutableList()
            val kaptBuilder = kaptConfiguration.deepCopy()
            workingDirectory?.let { path ->
                kaptBuilder[KaptConfigurationImpl.INCREMENTAL_DATA_OUTPUT_DIR] = path
            }
            if (kaptBuilder[SOURCE_OUTPUT_DIR] == null) {
                kaptBuilder[SOURCE_OUTPUT_DIR] = destinationDirectory // not used but must be provided
            }
            compilerPlugins += kaptBuilder.toCompilerPlugin()
            compilerArguments.compilerPlugins = compilerPlugins
        }
    }

    override fun Services.Builder.registerPlatformServices(logger: KotlinLogger) {
        val icConfiguration = incrementalCompilation
        if (icConfiguration !is JvmClientManagedIncrementalCompilationConfiguration) return

        icConfiguration.registerPlatformServices(logger)
    }

    private fun getIcOptionsAccessorOrNull(): HasSnapshotBasedIcOptionsAccessor? {
        val icConfiguration = incrementalCompilation ?: return null

        require(icConfiguration is JvmSnapshotBasedIncrementalCompilationConfiguration) {
            "Unexpected incremental compilation configuration: ${icConfiguration::class}. In this version, it must be an instance of " +
                    "JvmSnapshotBasedIncrementalCompilationConfiguration for incremental compilation, or null for non-incremental " +
                    "compilation."
        }

        return icConfiguration.toOptions()
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
        executionContext: ExecutionContext,
    ): IncrementalJvmCompilerRunner =
        IncrementalJvmCompilerRunner(
            workingDirectory.toFile(),
            buildReporter,
            outputDirs = aggregatedIcConfigurationOptions[OUTPUT_DIRS]?.map { it.toFile() },
            classpathChanges = classpathChanges,
            kotlinSourceFilesExtensions = kotlinFilenameExtensions,
            icFeatures = icFeatures,
            compilationCanceledStatus = cancellationHandle,
            generateCompilerRefIndex = generateCompilerRefIndex,
            lookupTrackerDelegate = getLookupTrackerAdapter(),
            pluginsLoader = executionContext.classloadersCache?.asPluginsLoader()
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
            generateCompilerRefIndex = generateCompilerRefIndex,
            lookupTrackerDelegate = getLookupTrackerAdapter(),
        )

    override fun deserializeResult(serializedData: SerializedDataContainer): CompilationResult {
        return KotlinToolchainsImpl.serializer.deserialize(CompilationResultSerializer, serializedData)
    }

    override fun serializeResult(result: CompilationResult): SerializedDataContainer {
        return KotlinToolchainsImpl.serializer.serialize(CompilationResultSerializer, result)
    }
}

private fun checkJvmFirRequirements(
    arguments: JvmCompilerArgumentsImpl,
): Boolean {
    val languageVersion = arguments.languageVersion?.let {
        LanguageVersion.fromVersionString(it.stringValue)
    } ?: LanguageVersion.LATEST_STABLE

    check(languageVersion >= LanguageVersion.KOTLIN_2_0) {
        "FIR incremental compiler runner is only compatible with Kotlin Language Version 2.0"
    }
    check(arguments.useFirIC) {
        "FIR incremental compiler runner requires '-Xuse-fir-ic' to be present in arguments"
    }

    return true
}
