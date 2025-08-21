/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.dirtyFiles

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.ChangedFiles.DeterminableFiles
import org.jetbrains.kotlin.incremental.IncrementalCompilerRunner.CompilationMode
import org.jetbrains.kotlin.incremental.javaInterop.JavaInteropCoordinator
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.snapshots.LazyClasspathSnapshot
import java.io.File


internal interface ImpactedFilesDeterminer {
    fun determineChangedAndImpactedSymbols(): ChangesEither
}

internal class JvmSourcesToCompileCalculator(
    private val caches: IncrementalJvmCachesManager,
    private val changedFiles: DeterminableFiles.Known,

    private val kotlinSourceFilesExtensions: Set<String>,
    private val javaInteropCoordinator: JavaInteropCoordinator,
    private val dirtyFilesProvider: DirtyFilesProvider,
    private val reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
) {

    private fun calculateSourcesToCompileImpl(
        impactedFilesDeterminer: ImpactedFilesDeterminer,
    ): CompilationMode {
        val changedAndImpactedSymbols = impactedFilesDeterminer.determineChangedAndImpactedSymbols()

        return when (changedAndImpactedSymbols) {
            is ChangesEither.Unknown -> {
                reporter.info { "Could not get classpath changes: ${changedAndImpactedSymbols.reason}" }
                CompilationMode.Rebuild(changedAndImpactedSymbols.reason)
            }
            is ChangesEither.Known -> {
                calculateSourcesToCompileWithKnownChanges(
                    changedFiles,
                    changedAndImpactedSymbols
                )
            }
        }
    }

    private fun calculateSourcesToCompileWithKnownChanges(
        changedFiles: DeterminableFiles.Known,
        changedAndImpactedSymbols: ChangesEither.Known,
    ): CompilationMode {
        val dirtyFiles = dirtyFilesProvider.getInitializedDirtyFiles(caches, changedFiles)

        dirtyFiles.addByDirtySymbols(changedAndImpactedSymbols.lookupSymbols)
        dirtyFiles.addByDirtyClasses(changedAndImpactedSymbols.fqNames)

        reporter.measure(GradleBuildTime.IC_ANALYZE_CHANGES_IN_JAVA_SOURCES) {
            val javaRebuildReason = javaInteropCoordinator.analyzeChangesInJavaSources(caches, changedFiles, dirtyFiles)
            if (javaRebuildReason != null) {
                return javaRebuildReason
            }
        }

        val androidLayoutChanges = reporter.measure(GradleBuildTime.IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS) {
            processLookupSymbolsForAndroidLayouts(changedFiles)
        }
        val removedClassesChanges = reporter.measure(GradleBuildTime.IC_DETECT_REMOVED_CLASSES) {
            getRemovedClassesChanges(caches, changedFiles, kotlinSourceFilesExtensions, reporter)
        }

        dirtyFiles.addByDirtySymbols(androidLayoutChanges)
        dirtyFiles.addByDirtySymbols(removedClassesChanges.dirtyLookupSymbols)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNames)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNamesForceRecompile)
        return CompilationMode.Incremental(dirtyFiles)
    }

    fun calculateWithClasspathSnapshot(
        classpathChanges: ClasspathChanges,
        lazyClasspathSnapshot: LazyClasspathSnapshot,
    ): CompilationMode {
        val impactedFilesDeterminer = ClasspathSnapshotBasedImpactDeterminer(
            caches,
            classpathChanges,
            lazyClasspathSnapshot,
            reporter
        )
        return calculateSourcesToCompileImpl(
            impactedFilesDeterminer,
        )
    }

    fun calculateWithBuildHistory(
        args: K2JVMCompilerArguments,
        abiSnapshots: Map<String, AbiSnapshot>,
        modulesApiHistory: ModulesApiHistory,

        buildHistoryFile: File?,
        lastBuildInfoFile: File,

        icFeatures: IncrementalCompilationFeatures,
        messageCollector: MessageCollector,
    ): CompilationMode {
        val impactedFilesDeterminer = HistoryFilesBasedImpactDeterminer(
            args,
            caches,
            changedFiles,
            abiSnapshots,
            modulesApiHistory,
            buildHistoryFile = buildHistoryFile,
            lastBuildInfoFile = lastBuildInfoFile,
            icFeatures,
            reporter,
            messageCollector,
        )
        return calculateSourcesToCompileImpl(
            impactedFilesDeterminer,
        )
    }
}

private fun processLookupSymbolsForAndroidLayouts(changedFiles: DeterminableFiles.Known): Collection<LookupSymbol> {
    val result = mutableListOf<LookupSymbol>()
    for (file in changedFiles.modified + changedFiles.removed) {
        if (file.extension.lowercase() != "xml") continue
        val layoutName = file.name.substringBeforeLast('.')
        result.add(LookupSymbol(ANDROID_LAYOUT_CONTENT_LOOKUP_NAME, layoutName))
    }

    return result
}
