/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2.jvm.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.v2.internal.OptionsDelegate
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.v2.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.v2.JvmCompilerArgumentsImpl
import java.nio.file.Path

class JvmCompilationOperationImpl(
    private val kotlinSources: List<Path>,
    private val destinationDirectory: Path,
    override val compilerArguments: JvmCompilerArguments = JvmCompilerArgumentsImpl()
) : BuildOperationImpl<CompilationResult>(), JvmCompilationOperation {
    private val optionsDelegate = OptionsDelegate<JvmCompilationOperation.Option<*>>()

    override fun <V> get(key: JvmCompilationOperation.Option<V>): V? = optionsDelegate[key]
    override fun <V> set(key: JvmCompilationOperation.Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    override fun createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions {
        TODO("Not yet implemented")
    }

    override fun execute(): CompilationResult {
        println("Compiling $kotlinSources into $destinationDirectory")

//        loggerAdapter.kotlinLogger.debug("Compiling using the in-process strategy")
//        setupIdeaStandaloneExecution()
//        val compiler = K2JVMCompiler()
//        val parsedArguments = compiler.createArguments()
//        parseCommandLineArguments(arguments, parsedArguments)
//        validateArguments(parsedArguments.errors)?.let {
//            throw CompilerArgumentsParseException(it)
//        }
//        val kotlinFilenameExtensions = (DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + compilationConfiguration.kotlinScriptFilenameExtensions)
//        val aggregatedIcConfiguration = compilationConfiguration.aggregatedIcConfiguration
//        return when (val options = aggregatedIcConfiguration?.options) {
//            is ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration -> {
//                @Suppress("DEPRECATION") // TODO: get rid of that parsing KT-62759
//                val kotlinSources = extractKotlinSourcesFromFreeCompilerArguments(parsedArguments, kotlinFilenameExtensions) + sources
//
//                @Suppress("UNCHECKED_CAST")
//                val classpathChanges =
//                    (aggregatedIcConfiguration as AggregatedIcConfiguration<ClasspathSnapshotBasedIncrementalCompilationApproachParameters>).classpathChanges
//                val buildReporter = BuildReporter(
//                    icReporter = BuildToolsApiBuildICReporter(loggerAdapter.kotlinLogger, options.rootProjectDir),
//                    buildMetricsReporter = DoNothingBuildMetricsReporter
//                )
//                val verifiedPreciseJavaTracking = parsedArguments.disablePreciseJavaTrackingIfK2(usePreciseJavaTrackingByDefault = options.preciseJavaTrackingEnabled)
//                val icFeatures = options.extractIncrementalCompilationFeatures().copy(
//                    usePreciseJavaTracking = verifiedPreciseJavaTracking
//                )
//                val incrementalCompiler = if (options.isUsingFirRunner && checkJvmFirRequirements(arguments)) {
//                    IncrementalFirJvmCompilerRunner(
//                        aggregatedIcConfiguration.workingDir,
//                        buildReporter,
//                        outputDirs = options.outputDirs,
//                        classpathChanges = classpathChanges,
//                        kotlinSourceFilesExtensions = kotlinFilenameExtensions,
//                        icFeatures = icFeatures
//                    )
//                } else {
//                    IncrementalJvmCompilerRunner(
//                        aggregatedIcConfiguration.workingDir,
//                        buildReporter,
//                        outputDirs = options.outputDirs,
//                        classpathChanges = classpathChanges,
//                        kotlinSourceFilesExtensions = kotlinFilenameExtensions,
//                        icFeatures = icFeatures
//                    )
//                }
//
//                val rootProjectDir = options.rootProjectDir
//                val buildDir = options.buildDir
//                parsedArguments.incrementalCompilation = true
//                incrementalCompiler.compile(
//                    kotlinSources, parsedArguments, loggerAdapter, aggregatedIcConfiguration.sourcesChanges.asChangedFiles,
//                    fileLocations = if (rootProjectDir != null && buildDir != null) {
//                        FileLocations(rootProjectDir, buildDir)
//                    } else null
//                ).asCompilationResult
//            }
//            null -> { // no IC configuration -> non-incremental compilation
//                parsedArguments.freeArgs += sources.filter { it.isKotlinFile(kotlinFilenameExtensions) }.map { it.absolutePath }
//                compiler.exec(loggerAdapter, Services.EMPTY, parsedArguments).asCompilationResult
//            }
//            else -> error(
//                "Unexpected incremental compilation configuration: $options. " +
//                        "In this version, it must be an instance of ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration " +
//                        "for incremental compilation, or null for non-incremental compilation."
//            )
//        }
//

        return CompilationResult.COMPILATION_SUCCESS

    }
}