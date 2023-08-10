/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import java.io.Serializable
import java.rmi.server.UnicastRemoteObject

internal val JvmCompilationConfigurationImpl.asDaemonCompilationOptions: CompilationOptions
    get() {
        val ktsExtensionsAsArray = if (kotlinScriptFilenameExtensions.isEmpty()) null else kotlinScriptFilenameExtensions.toTypedArray()
        val reportCategories = arrayOf(ReportCategory.COMPILER_MESSAGE.code, ReportCategory.IC_MESSAGE.code) // TODO: automagically compute the value, related to BasicCompilerServicesWithResultsFacadeServer
        val reportSeverity = ReportSeverity.INFO.code // TODO: automagically compute the value, related to BasicCompilerServicesWithResultsFacadeServer
        val requestedCompilationResults = emptyArray<Int>() // TODO: automagically compute the value, related to DaemonCompilationResults
        val aggregatedIcConfiguration = aggregatedIcConfiguration
        return when (val options = aggregatedIcConfiguration?.options) {
            is ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration -> {
                val sourcesChanges = aggregatedIcConfiguration.sourcesChanges
                val params = aggregatedIcConfiguration.parameters as ClasspathSnapshotBasedIncrementalCompilationApproachParameters
                val snapshotFiles =
                    ClasspathSnapshotFiles(params.newClasspathSnapshotFiles, params.shrunkClasspathSnapshot.parentFile)
                IncrementalCompilationOptions(
                    areFileChangesKnown = sourcesChanges is SourcesChanges.Known,
                    modifiedFiles = if (sourcesChanges is SourcesChanges.Known) sourcesChanges.modifiedFiles else null,
                    deletedFiles = if (sourcesChanges is SourcesChanges.Known) sourcesChanges.removedFiles else null,
                    classpathChanges = when {
                        !params.shrunkClasspathSnapshot.exists() -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot(
                            snapshotFiles
                        )
                        options.forcedNonIncrementalMode -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun(
                            snapshotFiles
                        )
                        options.assuredNoClasspathSnapshotsChanges -> ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges(
                            snapshotFiles
                        )
                        else -> {
                            ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(snapshotFiles)
                        }
                    },
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
                    withAbiSnapshot = false,
                    preciseCompilationResultsBackup = options.preciseCompilationResultsBackupEnabled,
                    keepIncrementalCompilationCachesInMemory = options.incrementalCompilationCachesKeptInMemory,
                )
            }
            else -> CompilationOptions(
                compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                targetPlatform = CompileService.TargetPlatform.JVM,
                reportCategories = reportCategories,
                reportSeverity = reportSeverity,
                requestedCompilationResults = requestedCompilationResults,
                kotlinScriptExtensions = ktsExtensionsAsArray,
            )
        }
    }

internal class DaemonCompilationResults : CompilationResults,
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
        println("Result category=$compilationResultCategory value=$value")
    }
}

internal val clientIsAliveFile by lazy {
    makeAutodeletingFlagFile()
}

internal fun createSessionIsAliveFlagFile() = makeAutodeletingFlagFile(keyword = "compilation-session")