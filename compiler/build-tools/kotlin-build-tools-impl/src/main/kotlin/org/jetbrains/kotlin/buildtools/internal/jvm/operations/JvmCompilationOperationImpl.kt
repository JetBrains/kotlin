/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.reportPerformanceData
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.CompilerArgumentsLogLevel
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.DAEMON_RUN_DIR_PATH
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.JVM_ARGUMENTS
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.SHUTDOWN_DELAY_MILLIS
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonCompilerArgumentsImpl.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonCompilerArgumentsImpl.Companion.X_USE_FIR_IC
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonToolArgumentsImpl.Companion.VERBOSE
import org.jetbrains.kotlin.buildtools.internal.arguments.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.jvm.HasSnapshotBasedIcOptionsAccessor
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.internal.jvm.toOptions
import org.jetbrains.kotlin.buildtools.internal.trackers.LookupTrackerAdapter
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.storage.FileLocations
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.URLClassLoader
import java.nio.file.Path
import java.rmi.RemoteException

internal class JvmCompilationOperationImpl private constructor(
    override val options: Options = Options(JvmCompilationOperation::class),
    override val sources: List<Path>,
    override val destinationDirectory: Path,
    override val compilerArguments: JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(),
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
) : CancellableBuildOperationImpl<CompilationResult>(), JvmCompilationOperation, JvmCompilationOperation.Builder,
    DeepCopyable<JvmCompilationOperationImpl> {
    constructor(
        sources: List<Path>,
        destinationDirectory: Path,
        compilerArguments: JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(),
        buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
    ) : this(
        options = Options(JvmCompilationOperation::class),
        sources = sources,
        destinationDirectory = destinationDirectory,
        compilerArguments = compilerArguments,
        buildIdToSessionFlagFile = buildIdToSessionFlagFile
    )

    override fun toBuilder(): JvmCompilationOperation.Builder = deepCopy()

    override fun deepCopy(): JvmCompilationOperationImpl {
        return JvmCompilationOperationImpl(
            options.deepCopy(),
            sources,
            destinationDirectory,
            compilerArguments.deepCopy(),
            buildIdToSessionFlagFile
        )
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmCompilationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmCompilationOperation.Option<V>, value: V) {
        options[key] = value
    }

    override fun build(): JvmCompilationOperation = deepCopy()

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    @Deprecated("Use `snapshotBasedIcConfigurationBuilder` instead.")
    @Suppress("DEPRECATION")
    override fun createSnapshotBasedIcOptions(): org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions {
        return org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl()
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

    override fun executeCancellableImpl(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): CompilationResult {
        val compilerMessageRenderer = this[COMPILER_MESSAGE_RENDERER]
        val kotlinLogger = logger ?: DefaultKotlinLogger
        val loggerAdapter = KotlinLoggerMessageCollectorAdapter(kotlinLogger, compilerMessageRenderer)

        return when (executionPolicy) {
            InProcessExecutionPolicyImpl -> {
                compileInProcess(loggerAdapter)
            }
            is DaemonExecutionPolicyImpl -> {
                compileWithDaemon(projectId, executionPolicy, loggerAdapter)
            }
            else -> {
                CompilationResult.COMPILATION_ERROR.also {
                    loggerAdapter.kotlinLogger.error("Unknown execution mode: ${executionPolicy::class.qualifiedName}")
                }
            }
        }
    }

    private fun toDaemonCompilationOptions(isDebugLoggingEnabled: Boolean): CompilationOptions {
        val ktsExtensionsAsArray = get(KOTLINSCRIPT_EXTENSIONS)

        // TODO: KT-79976 automagically compute the value, related to BasicCompilerServicesWithResultsFacadeServer
        val reportCategories = buildList {
            add(ReportCategory.COMPILER_MESSAGE.code)
            if (get(LOOKUP_TRACKER) != null) {
                add(ReportCategory.COMPILER_LOOKUP.code)
            }
        }.toTypedArray()

        val reportSeverity = if (VERBOSE in compilerArguments && compilerArguments[VERBOSE]) {
            ReportSeverity.DEBUG.code
        } else {
            ReportSeverity.INFO.code
        }
        val generateCompilerRefIndex = get(GENERATE_COMPILER_REF_INDEX)

        val requestedCompilationResults = listOfNotNull(
            CompilationResultCategory.IC_COMPILE_ITERATION.code,
            CompilationResultCategory.BUILD_METRICS.code.takeIf { this[METRICS_COLLECTOR] != null || this[XX_KGP_METRICS_COLLECTOR] },
            // Daemon would report log lines only if debug logging is enabled or metrics are requested
            CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES.code.takeIf { this[METRICS_COLLECTOR] != null || this[XX_KGP_METRICS_COLLECTOR] || isDebugLoggingEnabled },
        ).toTypedArray()

        return when (val aggregatedIcConfiguration: JvmIncrementalCompilationConfiguration? = get(INCREMENTAL_COMPILATION)) {
            is JvmSnapshotBasedIncrementalCompilationConfiguration -> {
                val aggregatedIcConfigurationOptions = aggregatedIcConfiguration.toOptions()
                val sourcesChanges = aggregatedIcConfiguration.sourcesChanges
                val classpathChanges = aggregatedIcConfiguration.classpathChanges
                IncrementalCompilationOptions(
                    sourcesChanges,
                    classpathChanges = classpathChanges,
                    workingDir = aggregatedIcConfiguration.workingDirectory.toFile(),
                    compilerMode = CompilerMode.INCREMENTAL_COMPILER,
                    targetPlatform = CompileService.TargetPlatform.JVM,
                    reportCategories = reportCategories,
                    reportSeverity = reportSeverity,
                    requestedCompilationResults = requestedCompilationResults,
                    outputFiles = aggregatedIcConfigurationOptions[OUTPUT_DIRS]?.map { it.toFile() },
                    multiModuleICSettings = null, // required only for the build history approach
                    modulesInfo = null, // required only for the build history approach
                    rootProjectDir = aggregatedIcConfigurationOptions[ROOT_PROJECT_DIR]?.toFile(),
                    buildDir = aggregatedIcConfigurationOptions[MODULE_BUILD_DIR]?.toFile(),
                    kotlinScriptExtensions = ktsExtensionsAsArray,
                    icFeatures = aggregatedIcConfiguration.extractIncrementalCompilationFeatures(),
                    useJvmFirRunner = aggregatedIcConfigurationOptions[USE_FIR_RUNNER],
                    generateCompilerRefIndex = generateCompilerRefIndex,
                )
            }
            // no IC configuration -> non-incremental compilation
            null -> CompilationOptions(
                compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                targetPlatform = CompileService.TargetPlatform.JVM,
                reportCategories = reportCategories,
                reportSeverity = reportSeverity,
                requestedCompilationResults = requestedCompilationResults,
                kotlinScriptExtensions = ktsExtensionsAsArray,
                generateCompilerRefIndex = generateCompilerRefIndex,
            )
            else -> error(
                "Unexpected incremental compilation configuration: $aggregatedIcConfiguration. In this version, it must be an instance of JvmIncrementalCompilationConfiguration for incremental compilation, or null for non-incremental compilation."
            )
        }
    }


    private fun compileWithDaemon(
        projectId: ProjectId,
        executionPolicy: DaemonExecutionPolicyImpl,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling using the daemon strategy")
        val compilerId = CompilerId.makeCompilerId(getCurrentClasspath())
        val sessionIsAliveFlagFile = buildIdToSessionFlagFile.computeIfAbsent(projectId) {
            createSessionIsAliveFlagFile()
        }

        val additionalJvmArguments = mutableListOf<String>()
        val daemonOptions = configureDaemonOptions(
            DaemonOptions().apply {
                executionPolicy[SHUTDOWN_DELAY_MILLIS]?.let { shutdownDelay ->
                    shutdownDelayMilliseconds = shutdownDelay
                }

                runFilesPath = executionPolicy[DAEMON_RUN_DIR_PATH].absolutePathStringOrThrow()
                additionalJvmArguments += "D${CompilerSystemProperties.COMPILE_DAEMON_CUSTOM_RUN_FILES_PATH_FOR_TESTS.property}=$runFilesPath"
            })

        val jvmOptions = configureDaemonJVMOptions(
            inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true
        ).also { opts ->
            val effectiveJvmArguments = additionalJvmArguments + (executionPolicy[JVM_ARGUMENTS] ?: emptyList())
            if (effectiveJvmArguments.isNotEmpty()) {
                opts.jvmParams.addAll(
                    effectiveJvmArguments.filterExtractProps(opts.mappers, "", opts.restMapper)
                )
            }
        }

        val (daemon, sessionId) = KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId,
            clientIsAliveFile,
            sessionIsAliveFlagFile,
            loggerAdapter,
            loggerAdapter.kotlinLogger.isDebugEnabled || System.getProperty("kotlin.daemon.debug.log")?.toBooleanStrictOrNull() ?: true,
            daemonJVMOptions = jvmOptions,
            daemonOptions = daemonOptions
        ) ?: return ExitCode.INTERNAL_ERROR.asCompilationResult
        onCancel {
            daemon.cancelCompilation(sessionId, compilationId)
        }
        if (loggerAdapter.kotlinLogger.isDebugEnabled) {
            daemon.getDaemonJVMOptions().takeIf { it.isGood }?.let { jvmOpts ->
                loggerAdapter.kotlinLogger.debug("Kotlin compile daemon JVM options: ${jvmOpts.get().mappers.flatMap { it.toArgs("-") }}")
            }
        }

        val daemonCompileOptions = toDaemonCompilationOptions(loggerAdapter.kotlinLogger.isDebugEnabled)
        loggerAdapter.kotlinLogger.info("Options for KOTLIN DAEMON: $daemonCompileOptions")
        val isIncrementalCompilation = daemonCompileOptions is IncrementalCompilationOptions
        if (isIncrementalCompilation && daemonCompileOptions.useJvmFirRunner) {
            checkJvmFirRequirements(compilerArguments)
        }
        val arguments = compilerArguments.toCompilerArguments()
        arguments.freeArgs += sources.map { it.absolutePathStringOrThrow() } // TODO: pass the sources explicitly KT-62759
        arguments.destination = destinationDirectory.absolutePathStringOrThrow()
        val aggregatedIcConfiguration = get(INCREMENTAL_COMPILATION) as? JvmSnapshotBasedIncrementalCompilationConfiguration
        val aggregatedIcConfigurationOptions = aggregatedIcConfiguration?.toOptions()
        val rootProjectDir = aggregatedIcConfigurationOptions?.get(ROOT_PROJECT_DIR)
        logCompilerArguments(loggerAdapter, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))
        val metricsReporter = getMetricsReporter()
        val exitCode = daemon.compile(
            sessionId,
            arguments.toArgumentStrings().toTypedArray(),
            daemonCompileOptions,
            BtaCompilerServicesWithResultsFacade(loggerAdapter, get(LOOKUP_TRACKER)),
            DaemonCompilationResults(
                loggerAdapter.kotlinLogger, rootProjectDir?.toFile(), metricsReporter
            ),
            compilationId
        ).get()

        try {
            daemon.releaseCompileSession(sessionId)
        } catch (e: RemoteException) {
            loggerAdapter.kotlinLogger.warn("Unable to release compile session, maybe daemon is already down: $e")
        }

        return (ExitCode.entries.find { it.code == exitCode } ?: if (exitCode == 0) {
            ExitCode.OK
        } else {
            ExitCode.COMPILATION_ERROR
        }).asCompilationResult.also {
            if (this@JvmCompilationOperationImpl[XX_KGP_METRICS_COLLECTOR] && metricsReporter is BuildMetricsReporterImpl) {
                this@JvmCompilationOperationImpl[XX_KGP_METRICS_COLLECTOR_OUT] = ByteArrayOutputStream().apply {
                    ObjectOutputStream(this).writeObject(metricsReporter)
                }.toByteArray()
            }
        }
    }

    private fun getCurrentClasspath() =
        (JvmCompilationOperationImpl::class.java.classLoader as URLClassLoader).urLs.map { transformUrlToFile(it) }


    private fun compileInProcess(loggerAdapter: KotlinLoggerMessageCollectorAdapter): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling using the in-process strategy")
        setupIdeaStandaloneExecution()
        val arguments = compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.destination = destinationDirectory.absolutePathStringOrThrow()
        }
        val kotlinFilenameExtensions = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + (get(KOTLINSCRIPT_EXTENSIONS) ?: emptyArray())
        return when (val aggregatedIcConfiguration = get(INCREMENTAL_COMPILATION)) {
            is JvmSnapshotBasedIncrementalCompilationConfiguration -> {
                aggregatedIcConfiguration.compileInProcess(arguments, loggerAdapter, kotlinFilenameExtensions)
            }
            null -> { // no IC configuration -> non-incremental compilation
                compileInProcessWithoutIc(arguments, loggerAdapter)
            }
            else -> error(
                "Unexpected incremental compilation configuration: $aggregatedIcConfiguration. In this version, it must be an instance of JvmSnapshotBasedIncrementalCompilationConfiguration for incremental compilation, or null for non-incremental compilation."
            )
        }
    }

    private fun compileInProcessWithoutIc(
        arguments: K2JVMCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        val compiler = K2JVMCompiler()
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

        if (this@JvmCompilationOperationImpl[XX_KGP_METRICS_COLLECTOR] && metricsReporter is BuildMetricsReporterImpl) {
            this@JvmCompilationOperationImpl[XX_KGP_METRICS_COLLECTOR_OUT] = ByteArrayOutputStream().apply {
                ObjectOutputStream(this).writeObject(metricsReporter)
            }.toByteArray()
        }

        return compilationResult
    }

    private fun JvmSnapshotBasedIncrementalCompilationConfiguration.compileInProcess(
        arguments: K2JVMCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        kotlinFilenameExtensions: Set<String>,
    ): CompilationResult {
        arguments.freeArgs += sources.filter { it.toFile().isJavaFile() }.map { it.absolutePathStringOrThrow() }

        val aggregatedIcConfigurationOptions = toOptions()
        val projectDir = aggregatedIcConfigurationOptions[ROOT_PROJECT_DIR]?.toFile()
        val buildDir = aggregatedIcConfigurationOptions[MODULE_BUILD_DIR]?.toFile()

        @Suppress("DEPRECATION") val kotlinSources = extractKotlinSourcesFromFreeCompilerArguments(
            arguments, DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS, includeJavaSources = true
        ) + sources.map { it.toFile() }

        val classpathChanges = classpathChanges
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
            arguments.disablePreciseJavaTrackingIfK2(usePreciseJavaTrackingByDefault = aggregatedIcConfigurationOptions[PRECISE_JAVA_TRACKING])
        val icFeatures = extractIncrementalCompilationFeatures().copy(
            usePreciseJavaTracking = verifiedPreciseJavaTracking
        )
        val incrementalCompiler = if (aggregatedIcConfigurationOptions[USE_FIR_RUNNER] && checkJvmFirRequirements(compilerArguments)) {
            getFirRunner(
                workingDirectory, buildReporter, aggregatedIcConfigurationOptions, classpathChanges, kotlinFilenameExtensions, icFeatures
            )
        } else {
            getNonFirRunner(
                workingDirectory, buildReporter, aggregatedIcConfigurationOptions, classpathChanges, kotlinFilenameExtensions, icFeatures
            )
        }

        arguments.incrementalCompilation = true
        logCompilerArguments(loggerAdapter, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))

        val fileLocations = if (projectDir != null && buildDir != null) {
            FileLocations(projectDir, buildDir)
        } else null
        val compilationResult = incrementalCompiler.compile(
            kotlinSources, arguments, loggerAdapter, sourcesChanges.asChangedFiles, fileLocations
        ).asCompilationResult

        metricsReporter.endMeasureGc()

        if (this@JvmCompilationOperationImpl[XX_KGP_METRICS_COLLECTOR] && metricsReporter is BuildMetricsReporterImpl) {
            this@JvmCompilationOperationImpl[XX_KGP_METRICS_COLLECTOR_OUT] = ByteArrayOutputStream().apply {
                ObjectOutputStream(this).writeObject(metricsReporter)
            }.toByteArray()
        }

        return compilationResult
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

    private fun getLookupTrackerAdapter(): LookupTracker = this@JvmCompilationOperationImpl[LOOKUP_TRACKER]?.let { tracker ->
        LookupTrackerAdapter(tracker)
    } ?: LookupTracker.DO_NOTHING

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

        val GENERATE_COMPILER_REF_INDEX: Option<Boolean> = Option("GENERATE_COMPILER_REF_INDEX", false)

        val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> = Option("COMPILER_MESSAGE_RENDERER", default = DefaultCompilerMessageRenderer)
    }
}

private class BtaCompilerServicesWithResultsFacade(
    loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    val lookupTracker: CompilerLookupTracker? = null,
) :
    BasicCompilerServicesWithResultsFacadeServer(loggerAdapter) {
    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        when (category) {
            ReportCategory.COMPILER_LOOKUP.code -> {
                attachment as LookupInfo?
                if (attachment == null) {
                    lookupTracker?.clear()
                } else {
                    lookupTracker?.recordLookup(
                        attachment.filePath,
                        attachment.scopeFqName,
                        CompilerLookupTracker.ScopeKind.valueOf(attachment.scopeKind.name),
                        attachment.name
                    )
                }
            }
            else -> super.report(category, severity, message, attachment)
        }
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
    @Suppress("DEPRECATION") check(X_USE_FIR_IC in arguments && arguments[X_USE_FIR_IC]) {
        "FIR incremental compiler runner requires '-Xuse-fir-ic' to be present in arguments"
    }

    return true
}