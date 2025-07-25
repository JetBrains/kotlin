/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.JVM_ARGUMENTS
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.SHUTDOWN_DELAY
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonCompilerArgumentsImpl.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonCompilerArgumentsImpl.Companion.X_USE_FIR_IC
import org.jetbrains.kotlin.buildtools.internal.arguments.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.USE_FIR_RUNNER
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
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.incremental.storage.FileLocations
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.rmi.RemoteException
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class JvmCompilationOperationImpl(
    private val kotlinSources: List<Path>,
    private val destinationDirectory: Path,
    override val compilerArguments: JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(),
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
) : BuildOperationImpl<CompilationResult>(), JvmCompilationOperation {

    private val optionsDelegate = OptionsDelegate()

    init {
        this[INCREMENTAL_COMPILATION] = null
        this[LOOKUP_TRACKER] = null
        this[KOTLINSCRIPT_EXTENSIONS] = null
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmCompilationOperation.Option<V>): V = optionsDelegate[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmCompilationOperation.Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    @OptIn(UseFromImplModuleRestricted::class)
    private operator fun <V> get(key: Option<V>): V = optionsDelegate[key]

    @OptIn(UseFromImplModuleRestricted::class)
    private operator fun <V> set(key: Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    override fun createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions {
        return JvmSnapshotBasedIncrementalCompilationOptionsImpl()
    }

    override fun execute(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): CompilationResult {
        val loggerAdapter =
            logger?.let { KotlinLoggerMessageCollectorAdapter(it) } ?: KotlinLoggerMessageCollectorAdapter(DefaultKotlinLogger)
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

    private fun toDaemonCompilationOptions(logger: KotlinLogger): CompilationOptions {
        val ktsExtensionsAsArray = get(KOTLINSCRIPT_EXTENSIONS)
        val reportCategories = arrayOf(
            ReportCategory.COMPILER_MESSAGE.code, ReportCategory.IC_MESSAGE.code
        ) // TODO: automagically compute the value, related to BasicCompilerServicesWithResultsFacadeServer
        val reportSeverity = if (logger.isDebugEnabled) {
            ReportSeverity.DEBUG.code
        } else {
            ReportSeverity.INFO.code
        }
        val aggregatedIcConfiguration: JvmIncrementalCompilationConfiguration? = get(INCREMENTAL_COMPILATION)
        return when (aggregatedIcConfiguration) {
            is JvmSnapshotBasedIncrementalCompilationConfiguration -> {
                val aggregatedIcConfigurationOptions =
                    aggregatedIcConfiguration.options as JvmSnapshotBasedIncrementalCompilationOptionsImpl
                val sourcesChanges = aggregatedIcConfiguration.sourcesChanges
                val requestedCompilationResults = arrayOf(
                    CompilationResultCategory.IC_COMPILE_ITERATION.code,
                )
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
                    rootProjectDir = aggregatedIcConfigurationOptions[ROOT_PROJECT_DIR].toFile(),
                    buildDir = aggregatedIcConfigurationOptions[MODULE_BUILD_DIR].toFile(),
                    kotlinScriptExtensions = ktsExtensionsAsArray,
                    icFeatures = aggregatedIcConfiguration.extractIncrementalCompilationFeatures(),
                    useJvmFirRunner = aggregatedIcConfigurationOptions[USE_FIR_RUNNER],
                )
            }
            // no IC configuration -> non-incremental compilation
            null -> CompilationOptions(
                compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                targetPlatform = CompileService.TargetPlatform.JVM,
                reportCategories = reportCategories,
                reportSeverity = reportSeverity,
                requestedCompilationResults = emptyArray(),
                kotlinScriptExtensions = ktsExtensionsAsArray,
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

        val jvmOptions = configureDaemonJVMOptions(
            inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true
        ).also { opts ->
            executionPolicy[JVM_ARGUMENTS]?.takeIf { it.isNotEmpty() }?.let { daemonJvmArguments ->
                opts.jvmParams.addAll(
                    daemonJvmArguments.filterExtractProps(opts.mappers, "", opts.restMapper)
                )
            }
        }

        val daemonOptions = configureDaemonOptions(
            DaemonOptions().apply {
                executionPolicy[SHUTDOWN_DELAY]?.let { shutdownDelay ->
                    shutdownDelayMilliseconds = shutdownDelay.inWholeMilliseconds
                }
            }
        )

        val (daemon, sessionId) = KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId,
            clientIsAliveFile,
            sessionIsAliveFlagFile,
            loggerAdapter,
            false,
            daemonJVMOptions = jvmOptions,
            daemonOptions = daemonOptions
        ) ?: return ExitCode.INTERNAL_ERROR.asCompilationResult
        val daemonCompileOptions = toDaemonCompilationOptions(loggerAdapter.kotlinLogger)
        val isIncrementalCompilation = daemonCompileOptions is IncrementalCompilationOptions
        if (isIncrementalCompilation && daemonCompileOptions.useJvmFirRunner) {
            checkJvmFirRequirements(compilerArguments)
        }
        val arguments = compilerArguments.toCompilerArguments()
        arguments.freeArgs += kotlinSources.map { it.absolutePathString() } // TODO: pass the sources explicitly KT-62759
        arguments.destination = destinationDirectory.absolutePathString()
        val aggregatedIcConfiguration = get(INCREMENTAL_COMPILATION) as? JvmSnapshotBasedIncrementalCompilationConfiguration
        val aggregatedIcConfigurationOptions = aggregatedIcConfiguration?.options as? JvmSnapshotBasedIncrementalCompilationOptionsImpl
        val rootProjectDir = aggregatedIcConfigurationOptions?.get(ROOT_PROJECT_DIR)
        val exitCode = daemon.compile(
            sessionId,
            arguments.toArgumentStrings().toTypedArray(),
            daemonCompileOptions,
            BasicCompilerServicesWithResultsFacadeServer(loggerAdapter),
            DaemonCompilationResults(
                loggerAdapter.kotlinLogger, rootProjectDir?.toFile()
            )
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
        }).asCompilationResult

    }

    private fun getCurrentClasspath() =
        (JvmCompilationOperationImpl::class.java.classLoader as URLClassLoader).urLs.map { transformUrlToFile(it) }


    private fun compileInProcess(loggerAdapter: KotlinLoggerMessageCollectorAdapter): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling using the in-process strategy")
        setupIdeaStandaloneExecution()
        val arguments = compilerArguments.toCompilerArguments().also {
            it.destination = destinationDirectory.absolutePathString()
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
                "Unexpected incremental compilation configuration: $aggregatedIcConfiguration. "
                        + "In this version, it must be an instance of JvmSnapshotBasedIncrementalCompilationConfiguration for incremental "
                        + "compilation, or null for non-incremental compilation."
            )
        }
    }

    private fun compileInProcessWithoutIc(
        arguments: K2JVMCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        val compiler = K2JVMCompiler()
        arguments.freeArgs += kotlinSources.map { it.absolutePathString() }
        val services = Services.Builder().apply {
            get(LOOKUP_TRACKER)?.let { tracker: CompilerLookupTracker ->
                register(LookupTracker::class.java, object : LookupTracker {
                    override val requiresPosition = false

                    override fun record(
                        filePath: String,
                        position: Position,
                        scopeFqName: String,
                        scopeKind: ScopeKind,
                        name: String,
                    ) {
                        tracker.recordLookup(
                            filePath,
                            scopeFqName,
                            CompilerLookupTracker.ScopeKind.valueOf(scopeKind.name),
                            name
                        )
                    }

                    override fun clear() {
                        tracker.clear()
                    }
                })
            }
        }.build()
        return compiler.exec(loggerAdapter, services, arguments).asCompilationResult
    }

    private fun JvmSnapshotBasedIncrementalCompilationConfiguration.compileInProcess(
        arguments: K2JVMCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        kotlinFilenameExtensions: Set<String>,
    ): CompilationResult {
        val aggregatedIcConfigurationOptions =
            options as JvmSnapshotBasedIncrementalCompilationOptionsImpl
        @Suppress("DEPRECATION") val kotlinSources = extractKotlinSourcesFromFreeCompilerArguments(
            arguments, DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS, includeJavaSources = true
        ) + kotlinSources.map { it.toFile() }

        val classpathChanges = classpathChanges
        val buildReporter = BuildReporter(
            icReporter = BuildToolsApiBuildICReporter(
                loggerAdapter.kotlinLogger, aggregatedIcConfigurationOptions[ROOT_PROJECT_DIR].toFile()
            ), buildMetricsReporter = DoNothingBuildMetricsReporter
        )
        val verifiedPreciseJavaTracking =
            arguments.disablePreciseJavaTrackingIfK2(usePreciseJavaTrackingByDefault = aggregatedIcConfigurationOptions[PRECISE_JAVA_TRACKING])
        val icFeatures = extractIncrementalCompilationFeatures().copy(
            usePreciseJavaTracking = verifiedPreciseJavaTracking
        )
        val incrementalCompiler =
            if (aggregatedIcConfigurationOptions[USE_FIR_RUNNER] && checkJvmFirRequirements(compilerArguments)) {
                IncrementalFirJvmCompilerRunner(
                    workingDirectory.toFile(),
                    buildReporter,
                    outputDirs = aggregatedIcConfigurationOptions[OUTPUT_DIRS]?.map { it.toFile() },
                    classpathChanges = classpathChanges,
                    kotlinSourceFilesExtensions = kotlinFilenameExtensions,
                    icFeatures = icFeatures
                )
            } else {
                IncrementalJvmCompilerRunner(
                    workingDirectory.toFile(),
                    buildReporter,
                    outputDirs = aggregatedIcConfigurationOptions[OUTPUT_DIRS]?.map { it.toFile() },
                    classpathChanges = classpathChanges,
                    kotlinSourceFilesExtensions = kotlinFilenameExtensions,
                    icFeatures = icFeatures
                )
            }

        arguments.incrementalCompilation = true
        return incrementalCompiler.compile(
            kotlinSources,
            arguments,
            loggerAdapter,
            sourcesChanges.asChangedFiles,
            fileLocations = FileLocations(
                aggregatedIcConfigurationOptions[ROOT_PROJECT_DIR].toFile(),
                aggregatedIcConfigurationOptions[MODULE_BUILD_DIR].toFile()
            )
        ).asCompilationResult
    }

    companion object {
        val INCREMENTAL_COMPILATION: Option<JvmIncrementalCompilationConfiguration?> = Option("INCREMENTAL_COMPILATION")

        val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER")

        val KOTLINSCRIPT_EXTENSIONS: Option<Array<String>?> = Option("KOTLINSCRIPT_EXTENSIONS")
    }
}

private fun JvmSnapshotBasedIncrementalCompilationConfiguration.extractIncrementalCompilationFeatures(): IncrementalCompilationFeatures {
    val options = options as JvmSnapshotBasedIncrementalCompilationOptionsImpl
    return IncrementalCompilationFeatures(
        usePreciseJavaTracking = options[PRECISE_JAVA_TRACKING],
        withAbiSnapshot = false,
        preciseCompilationResultsBackup = options[BACKUP_CLASSES],
        keepIncrementalCompilationCachesInMemory = options[KEEP_IC_CACHES_IN_MEMORY],
    )
}

private val JvmSnapshotBasedIncrementalCompilationConfiguration.classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled
    get() {
        val options = options as JvmSnapshotBasedIncrementalCompilationOptionsImpl
        val snapshotFiles =
            ClasspathSnapshotFiles(dependenciesSnapshotFiles.map { it.toFile() }, shrunkClasspathSnapshot.toFile().parentFile)
        return when {
            !shrunkClasspathSnapshot.exists() -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot(
                snapshotFiles
            )
            options[FORCE_RECOMPILATION] -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun(
                snapshotFiles
            )
            options[ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] -> ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges(
                snapshotFiles
            )
            else -> {
                ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(snapshotFiles)
            }
        }
    }

private fun checkJvmFirRequirements(
    arguments: JvmCompilerArgumentsImpl,
): Boolean {
    val languageVersion: LanguageVersion = try {
        arguments[LANGUAGE_VERSION]
    } catch (_: Exception) {
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