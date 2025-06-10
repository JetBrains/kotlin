/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2.jvm.operations

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XUSE_FIR_IC
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.v2.internal.OptionsDelegate
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.v2.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.v2.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.storage.FileLocations
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class JvmCompilationOperationImpl(
    private val kotlinSources: List<Path>,
    private val destinationDirectory: Path,
    override val compilerArguments: JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(),
) : BuildOperationImpl<CompilationResult>(), JvmCompilationOperation {
    private val optionsDelegate = OptionsDelegate<JvmCompilationOperation.Option<*>>()

    override fun <V> get(key: JvmCompilationOperation.Option<V>): V = optionsDelegate[key]
    override fun <V> set(key: JvmCompilationOperation.Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    override fun createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions {
        return JvmSnapshotBasedIncrementalCompilationOptionsImpl()
    }

    override fun execute(executionPolicy: ExecutionPolicy?, logger: KotlinLogger?): CompilationResult {
        val loggerAdapter =
            logger?.let { KotlinLoggerMessageCollectorAdapter(it) } ?: KotlinLoggerMessageCollectorAdapter(DefaultKotlinLogger)
        return if (executionPolicy == null || executionPolicy[ExecutionPolicy.EXECUTION_MODE] == ExecutionPolicy.ExecutionMode.IN_PROCESS) {
            compileInProcess(loggerAdapter)
        } else if (executionPolicy[ExecutionPolicy.EXECUTION_MODE] == ExecutionPolicy.ExecutionMode.DAEMON) {
            compileWithDaemon(executionPolicy, loggerAdapter)
        } else {
            CompilationResult.COMPILATION_ERROR.also {
                loggerAdapter.kotlinLogger.error("Unknown execution mode: ${executionPolicy[ExecutionPolicy.EXECUTION_MODE]}")
            }
        }
    }

    private fun compileWithDaemon(
        executionPolicy: ExecutionPolicy,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        TODO("todo")
    }

    private fun compileInProcess(loggerAdapter: KotlinLoggerMessageCollectorAdapter): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling using the in-process strategy")
        setupIdeaStandaloneExecution()
        val compiler = K2JVMCompiler()
        val arguments = compilerArguments.toCompilerArguments()
        val kotlinFilenameExtensions = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + optionsDelegate.getOrElse(JvmCompilationOperation.Companion.KOTLINSCRIPT_EXTENSIONS, emptyArray())
        arguments.destination = destinationDirectory.absolutePathString()
//        arguments.freeArgs += kotlinSources.filter { it.toFile().isKotlinFile(DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS) }
//            .map { it.absolutePathString() }

        val aggregatedIcConfiguration: JvmIncrementalCompilationConfiguration? =
            optionsDelegate.getOrElse(INCREMENTAL_COMPILATION, null)
        return when (aggregatedIcConfiguration) {
            is JvmSnapshotBasedIncrementalCompilationConfiguration -> {
                @Suppress("DEPRECATION")
                val kotlinSources =
                    extractKotlinSourcesFromFreeCompilerArguments(arguments, DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS) + kotlinSources.map { it.toFile() }

                val classpathChanges = aggregatedIcConfiguration.classpathChanges
                val buildReporter = BuildReporter(
                    icReporter = BuildToolsApiBuildICReporter(
                        loggerAdapter.kotlinLogger,
                        aggregatedIcConfiguration.options[ROOT_PROJECT_DIR].toFile()
                    ),
                    buildMetricsReporter = DoNothingBuildMetricsReporter
                )
                val verifiedPreciseJavaTracking =
                    arguments.disablePreciseJavaTrackingIfK2(usePreciseJavaTrackingByDefault = aggregatedIcConfiguration.options[PRECISE_JAVA_TRACKING])
                val icFeatures = aggregatedIcConfiguration.extractIncrementalCompilationFeatures().copy(
                    usePreciseJavaTracking = verifiedPreciseJavaTracking
                )
                val incrementalCompiler = if (aggregatedIcConfiguration.options[USE_FIR_RUNNER] && checkJvmFirRequirements(compilerArguments)) {
                        IncrementalFirJvmCompilerRunner(
                            aggregatedIcConfiguration.workingDirectory.toFile(),
                            buildReporter,
                            outputDirs = aggregatedIcConfiguration.options[OUTPUT_DIRS].map { it.toFile() },
                            classpathChanges = classpathChanges,
                            kotlinSourceFilesExtensions = kotlinFilenameExtensions,
                            icFeatures = icFeatures
                        )
                    } else {
                        IncrementalJvmCompilerRunner(
                            aggregatedIcConfiguration.workingDirectory.toFile(),
                            buildReporter,
                            outputDirs = aggregatedIcConfiguration.options[OUTPUT_DIRS].map { it.toFile() },
                            classpathChanges = classpathChanges,
                            kotlinSourceFilesExtensions = kotlinFilenameExtensions,
                            icFeatures = icFeatures
                        )
                    }

                val rootProjectDir = aggregatedIcConfiguration.options[ROOT_PROJECT_DIR]
                val buildDir = aggregatedIcConfiguration.options[MODULE_BUILD_DIR]
                arguments.incrementalCompilation = true
                incrementalCompiler.compile(
                    kotlinSources, arguments, loggerAdapter, aggregatedIcConfiguration.sourcesChanges.asChangedFiles,
                    fileLocations = FileLocations(rootProjectDir.toFile(), buildDir.toFile())
                ).asCompilationResult
            }
            null -> { // no IC configuration -> non-incremental compilation
                arguments.freeArgs += kotlinSources.filter { it.toFile().isKotlinFile(kotlinFilenameExtensions) }
                    .map { it.absolutePathString() }
                compiler.exec(loggerAdapter, Services.EMPTY, arguments).asCompilationResult
            }
            else -> error(
                "Unexpected incremental compilation configuration: $aggregatedIcConfiguration. " +
                        "In this version, it must be an instance of JvmSnapshotBasedIncrementalCompilationConfiguration " +
                        "for incremental compilation, or null for non-incremental compilation."
            )
        }
    }
}

private fun JvmSnapshotBasedIncrementalCompilationConfiguration.extractIncrementalCompilationFeatures(): IncrementalCompilationFeatures =
    IncrementalCompilationFeatures(
        usePreciseJavaTracking = options[PRECISE_JAVA_TRACKING],
        withAbiSnapshot = false,
        preciseCompilationResultsBackup = options[BACKUP_CLASSES], //TODO is it this option?
        keepIncrementalCompilationCachesInMemory = options[KEEP_IC_CACHES_IN_MEMORY],
    )



internal val JvmSnapshotBasedIncrementalCompilationConfiguration.classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled
    get() {
        val snapshotFiles = ClasspathSnapshotFiles(dependenciesSnapshotFiles.map { it.toFile() }, shrunkClasspathSnapshot.toFile().parentFile)
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


internal fun checkJvmFirRequirements(
    arguments: JvmCompilerArgumentsImpl,
): Boolean {
    val languageVersion: LanguageVersion = try { arguments[LANGUAGE_VERSION] } catch (_: Exception) { null }
        ?.let {
            LanguageVersion.fromVersionString(it.value)
        } ?: LanguageVersion.LATEST_STABLE

    check(languageVersion >= LanguageVersion.KOTLIN_2_0) {
        "FIR incremental compiler runner is only compatible with Kotlin Language Version 2.0"
    }
    @Suppress("DEPRECATION")
    check(arguments[XUSE_FIR_IC]) {
        "FIR incremental compiler runner requires '-Xuse-fir-ic' to be present in arguments"
    }

    return true
}