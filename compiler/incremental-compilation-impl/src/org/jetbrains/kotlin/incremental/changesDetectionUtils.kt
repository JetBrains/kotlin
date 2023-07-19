/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.util.Either
import org.jetbrains.kotlin.name.FqName
import java.io.File

internal fun getClasspathChanges(
    classpath: List<File>,
    changedFiles: ChangedFiles.Known,
    lastBuildInfo: BuildInfo,
    modulesApiHistory: ModulesApiHistory,
    reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    abiSnapshots: Map<String, AbiSnapshot>,
    withSnapshot: Boolean,
    caches: IncrementalCacheCommon,
    scopes: Collection<String>
): ChangesEither {
    val classpathSet = HashSet<File>()
    for (file in classpath) {
        when {
            file.isFile -> classpathSet.add(file)
            file.isDirectory -> file.walk().filterTo(classpathSet) { it.isFile }
        }
    }

    val modifiedClasspath = changedFiles.modified.filterTo(HashSet()) { it in classpathSet }
    val removedClasspath = changedFiles.removed.filterTo(HashSet()) { it in classpathSet }

    // todo: removed classes could be processed normally
    if (removedClasspath.isNotEmpty()) {
        reporter.info { "Some files are removed from classpath: $removedClasspath" }
        return ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_REMOVED_ENTRY)
    }

    if (modifiedClasspath.isEmpty()) return ChangesEither.Known()

    if (withSnapshot) {
        fun analyzeJarFiles(): ChangesEither {
            val symbols = HashSet<LookupSymbol>()
            val fqNames = HashSet<FqName>()

            for ((module, abiSnapshot) in abiSnapshots) {
                val actualAbiSnapshot = lastBuildInfo.dependencyToAbiSnapshot[module]
                if (actualAbiSnapshot == null) {

                    reporter.info { "Some jar are removed from classpath $module" }
                    return ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_REMOVED_ENTRY)
                }
                val diffData = AbiSnapshotDiffService.doCompute(abiSnapshot, actualAbiSnapshot, caches, scopes)
                symbols.addAll(diffData.dirtyLookupSymbols)
                fqNames.addAll(diffData.dirtyClassesFqNames)

            }
            return ChangesEither.Known(symbols, fqNames)
        }
        return reporter.measure(GradleBuildTime.IC_ANALYZE_JAR_FILES) {
            analyzeJarFiles()
        }
    } else {
        val lastBuildTS = lastBuildInfo.startTS

        val symbols = HashSet<LookupSymbol>()
        val fqNames = HashSet<FqName>()

        val historyFilesEither =
            reporter.measure(GradleBuildTime.IC_FIND_HISTORY_FILES) {
                modulesApiHistory.historyFilesForChangedFiles(modifiedClasspath)
            }

        val historyFiles = when (historyFilesEither) {
            is Either.Success<Set<File>> -> historyFilesEither.value
            is Either.Error -> {
                reporter.info { "Could not find history files: ${historyFilesEither.reason}" }
                return ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_HISTORY_IS_NOT_FOUND)
            }
        }

        fun analyzeHistoryFiles(): ChangesEither {
            for (historyFile in historyFiles) {
                val allBuilds = BuildDiffsStorage.readDiffsFromFile(historyFile, reporter = reporter)
                    ?: return run {
                        reporter.info { "Could not read diffs from $historyFile" }
                        ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_HISTORY_CANNOT_BE_READ)
                    }

                val (knownBuilds, newBuilds) = allBuilds.partition { it.ts <= lastBuildTS }
                if (knownBuilds.isEmpty()) {
                    reporter.info { "No previously known builds for $historyFile" }
                    return ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_HISTORY_NO_KNOWN_BUILDS)
                }


                for (buildDiff in newBuilds) {
                    if (!buildDiff.isIncremental) {
                        reporter.info { "Non-incremental build from dependency $historyFile" }
                        return ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_NON_INCREMENTAL_BUILD_IN_DEP)

                    }
                    val dirtyData = buildDiff.dirtyData
                    symbols.addAll(dirtyData.dirtyLookupSymbols)
                    fqNames.addAll(dirtyData.dirtyClassesFqNames)
                }
            }

            return ChangesEither.Known(symbols, fqNames)
        }

        return reporter.measure(GradleBuildTime.IC_ANALYZE_HISTORY_FILES) {
            analyzeHistoryFiles()
        }
    }
}