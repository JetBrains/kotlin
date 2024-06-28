/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.daemon.common.*
import java.io.File
import java.io.Serializable
import java.rmi.server.UnicastRemoteObject

internal val JvmCompilationConfigurationImpl.asDaemonCompilationOptions: CompilationOptions
    get() {
        val ktsExtensionsAsArray = if (kotlinScriptFilenameExtensions.isEmpty()) null else kotlinScriptFilenameExtensions.toTypedArray()
        val reportCategories = arrayOf(ReportCategory.COMPILER_MESSAGE.code, ReportCategory.IC_MESSAGE.code) // TODO: automagically compute the value, related to BasicCompilerServicesWithResultsFacadeServer
        val reportSeverity = if (logger.isDebugEnabled) {
            ReportSeverity.DEBUG.code
        } else {
            ReportSeverity.INFO.code
        }
        val aggregatedIcConfiguration = aggregatedIcConfiguration
        return when (val options = aggregatedIcConfiguration?.options) {
            is ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration -> {
                val sourcesChanges = aggregatedIcConfiguration.sourcesChanges
                val requestedCompilationResults = arrayOf(
                    CompilationResultCategory.IC_COMPILE_ITERATION.code,
                )

                @Suppress("UNCHECKED_CAST")
                val classpathChanges =
                    (aggregatedIcConfiguration as AggregatedIcConfiguration<ClasspathSnapshotBasedIncrementalCompilationApproachParameters>).classpathChanges
                IncrementalCompilationOptions(
                    areFileChangesKnown = sourcesChanges is SourcesChanges.Known,
                    modifiedFiles = if (sourcesChanges is SourcesChanges.Known) sourcesChanges.modifiedFiles else null,
                    deletedFiles = if (sourcesChanges is SourcesChanges.Known) sourcesChanges.removedFiles else null,
                    classpathChanges = classpathChanges,
                    workingDir = aggregatedIcConfiguration.workingDir,
                    compilerMode = CompilerMode.INCREMENTAL_COMPILER,
                    targetPlatform = CompileService.TargetPlatform.JVM,
                    reportCategories = reportCategories,
                    reportSeverity = reportSeverity,
                    requestedCompilationResults = requestedCompilationResults,
                    usePreciseJavaTracking = options.preciseJavaTrackingEnabled,
                    outputFiles = options.outputDirs,
                    multiModuleICSettings = null, // required only for the build history approach
                    modulesInfo = null, // required only for the build history approach
                    rootProjectDir = options.rootProjectDir,
                    buildDir = options.buildDir,
                    kotlinScriptExtensions = ktsExtensionsAsArray,
                    icFeatures = options.extractIncrementalCompilationFeatures(),
                )
            }
            else -> CompilationOptions(
                compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                targetPlatform = CompileService.TargetPlatform.JVM,
                reportCategories = reportCategories,
                reportSeverity = reportSeverity,
                requestedCompilationResults = emptyArray(),
                kotlinScriptExtensions = ktsExtensionsAsArray,
            )
        }
    }

internal class DaemonCompilationResults(private val kotlinLogger: KotlinLogger, private val rootProjectDir: File?) : CompilationResults,
    UnicastRemoteObject(
        SOCKET_ANY_FREE_PORT,
        LoopbackNetworkInterface.clientLoopbackSocketFactory,
        LoopbackNetworkInterface.serverLoopbackSocketFactory
    ) {
    /**
     * Possible combinations:
     * 1. [CompilationResultCategory.IC_COMPILE_ITERATION.code]       -> a [CompileIterationResult] instance
     * 2. [CompilationResultCategory.BUILD_REPORT_LINES.code]         -> a [List] of [String]
     * 3. [CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES.code] -> a [List] of [String]
     * 4. [CompilationResultCategory.BUILD_METRICS.code]              -> a [BuildMetrics] instance
     **/
    override fun add(compilationResultCategory: Int, value: Serializable) {
        // TODO propagate the values to the caller via callbacks, requires to make metrics a part of the API
        when (compilationResultCategory) {
            CompilationResultCategory.IC_COMPILE_ITERATION.code -> kotlinLogger.debug(value as? CompileIterationResult, rootProjectDir)
            else -> kotlinLogger.debug("Result category=$compilationResultCategory value=$value")
        }
    }
}

internal val clientIsAliveFile by lazy {
    makeAutodeletingFlagFile()
}

internal fun KotlinLogger.debug(compileIterationResult: CompileIterationResult?, rootProjectDir: File?) {
    if (compileIterationResult != null && isDebugEnabled) {
        if (compileIterationResult.sourceFiles.any()) {
            val sourceFiles = compileIterationResult.sourceFiles
                .let { files ->
                    files.map {
                        val relativePath = if (rootProjectDir != null) it.relativeToOrNull(rootProjectDir)?.path else null
                        return@map relativePath ?: it.normalize().absolutePath
                    }
                }
            debug("compile iteration: ${sourceFiles.joinToString()}")
        }
        debug("compiler exit code: ${compileIterationResult.exitCode}")
    }
}

internal fun createSessionIsAliveFlagFile() = makeAutodeletingFlagFile(keyword = "compilation-session")