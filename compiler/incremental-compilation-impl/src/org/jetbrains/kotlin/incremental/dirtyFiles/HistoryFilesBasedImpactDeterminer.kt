/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.dirtyFiles

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.ChangedFiles.DeterminableFiles
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import java.io.File

internal class HistoryFilesBasedImpactDeterminer(
    private val args: K2JVMCompilerArguments,
    private val caches: IncrementalJvmCachesManager,
    private val changedFiles: DeterminableFiles.Known,

    private val abiSnapshots: Map<String, AbiSnapshot>,
    private val modulesApiHistory: ModulesApiHistory,

    private val buildHistoryFile: File?,
    private val lastBuildInfoFile: File,
    private val icFeatures: IncrementalCompilationFeatures,
    private val reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    private val messageCollector: MessageCollector,
) : ImpactedFilesDeterminer {

    private fun verifyBuildHistoryFilesState(): BuildAttribute? {
        if (buildHistoryFile == null) {
            error("The build is configured to use the build-history based IC approach, but doesn't specify the buildHistoryFile")
        }
        if (!icFeatures.withAbiSnapshot && buildHistoryFile.isFile != true) {
            // If the previous build was a Gradle cache hit, the build history file must have been deleted as it is marked as
            // @LocalState in the Gradle task. Therefore, this compilation will need to run non-incrementally.
            // (Note that buildHistoryFile is outside workingDir. We don't need to perform the same check for files inside
            // workingDir as workingDir is an @OutputDirectory, so the files must be present in an incremental build.)
            return BuildAttribute.NO_BUILD_HISTORY
        }
        if (!lastBuildInfoFile.exists()) {
            return BuildAttribute.NO_LAST_BUILD_INFO
        }
        return null // no rebuild necessary so far, compilation might proceed
    }

    override fun determineChangedAndImpactedSymbols(): ChangesEither {
        reporter.measure(GradleBuildTime.IC_ANALYZE_CHANGES_IN_DEPENDENCIES) {
            verifyBuildHistoryFilesState()?.let { rebuildReason ->
                return ChangesEither.Unknown(rebuildReason)
            }

            val lastBuildInfo = BuildInfo.read(lastBuildInfoFile, messageCollector)
                ?: return ChangesEither.Unknown(BuildAttribute.INVALID_LAST_BUILD_INFO)

            reporter.debug { "Last Kotlin Build info -- $lastBuildInfo" }
            val scopes = caches.lookupCache.lookupSymbols.map { it.scope.ifBlank { it.name } }.distinct()

            // The old IC currently doesn't compute impacted symbols? KT-75180
            return getClasspathChanges(
                args.classpathAsList,
                changedFiles,
                lastBuildInfo,
                modulesApiHistory,
                reporter,
                abiSnapshots,
                icFeatures.withAbiSnapshot,
                caches.platformCache,
                scopes
            )
        }
    }
}
