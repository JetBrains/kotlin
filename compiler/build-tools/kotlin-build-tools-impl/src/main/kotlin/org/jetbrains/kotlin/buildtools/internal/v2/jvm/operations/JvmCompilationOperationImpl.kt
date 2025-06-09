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
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.v2.internal.OptionsDelegate
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.CompilationServiceImpl.checkJvmFirRequirements
import org.jetbrains.kotlin.buildtools.internal.v2.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.v2.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.storage.FileLocations
import java.nio.file.Path
import kotlin.io.path.absolutePathString

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
        val kotlinFilenameExtensions = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS //TODO + extensions?
//        arguments.destination = destinationDirectory.absolutePathString()
//        arguments.freeArgs += kotlinSources.filter { it.toFile().isKotlinFile(DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS) }
//            .map { it.absolutePathString() }

        val aggregatedIcConfiguration: JvmIncrementalCompilationConfiguration? =
            optionsDelegate.getOrElse(INCREMENTAL_COMPILATION, null)
        return when (aggregatedIcConfiguration) {
            is JvmSnapshotBasedIncrementalCompilationConfiguration -> {
                val kotlinSources =
                    extractKotlinSourcesFromFreeCompilerArguments(arguments, DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS) + kotlinSources.map { it.toFile() }

//                val classpathChanges = aggregatedIcConfiguration.
//                    (aggregatedIcConfiguration as AggregatedIcConfiguration<ClasspathSnapshotBasedIncrementalCompilationApproachParameters>).classpathChanges
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
                val incrementalCompiler = if (/*TODO options.isUsingFirRunner*/ false && checkJvmFirRequirements(arguments)) {
//                    IncrementalFirJvmCompilerRunner(
//                        aggregatedIcConfiguration.workingDirectory.toFile(),
//                        buildReporter,
//                        outputDirs = aggregatedIcConfiguration.options.outputDirs,
//                        classpathChanges = classpathChanges,
//                        kotlinSourceFilesExtensions = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS,
//                        icFeatures = icFeatures
//                    )
                    TODO()
                } else {
                    IncrementalJvmCompilerRunner(
                        aggregatedIcConfiguration.workingDirectory.toFile(),
                        buildReporter,
                        outputDirs = options.outputDirs,
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
                    fileLocations = if (rootProjectDir != null && buildDir != null) {
                        FileLocations(rootProjectDir.toFile(), buildDir.toFile())
                    } else null
                ).asCompilationResult
            }
            null -> { // no IC configuration -> non-incremental compilation
                arguments.freeArgs += kotlinSources.filter { it.toFile().isKotlinFile(kotlinFilenameExtensions) }.map { it.absolutePath }
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

