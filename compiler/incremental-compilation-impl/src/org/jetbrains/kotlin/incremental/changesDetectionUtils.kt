/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.util.Either
import org.jetbrains.kotlin.name.FqName
import java.io.File

internal fun getClasspathChanges(
    classpath: List<File>,
    changedFiles: ChangedFiles.Known,
    lastBuildInfo: BuildInfo,
    modulesApiHistory: ModulesApiHistory,
    reporter: BuildReporter
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
        reporter.report { "Some files are removed from classpath: $removedClasspath" }
        return ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_REMOVED_ENTRY)
    }

    if (modifiedClasspath.isEmpty()) return ChangesEither.Known()

    val lastBuildTS = lastBuildInfo.startTS

    val symbols = HashSet<LookupSymbol>()
    val fqNames = HashSet<FqName>()

    val historyFilesEither =
        reporter.measure(BuildTime.IC_FIND_HISTORY_FILES) {
            modulesApiHistory.historyFilesForChangedFiles(modifiedClasspath)
        }
    val historyFiles = when (historyFilesEither) {
        is Either.Success<Set<File>> -> historyFilesEither.value
        is Either.Error -> {
            reporter.report { "Could not find history files: ${historyFilesEither.reason}" }
            return ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_HISTORY_IS_NOT_FOUND)
        }
    }

    fun analyzeHistoryFiles(): ChangesEither {
        for (historyFile in historyFiles) {
            val allBuilds = BuildDiffsStorage.readDiffsFromFile(historyFile, reporter = reporter)
                ?: return run {
                    reporter.report { "Could not read diffs from $historyFile" }
                    ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_HISTORY_CANNOT_BE_READ)
                }
            val (knownBuilds, newBuilds) = allBuilds.partition { it.ts <= lastBuildTS }
            if (knownBuilds.isEmpty()) {
                reporter.report { "No previously known builds for $historyFile" }
                return ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_HISTORY_NO_KNOWN_BUILDS)
            }

            for (buildDiff in newBuilds) {
                if (!buildDiff.isIncremental) {
                    reporter.report { "Non-incremental build from dependency $historyFile" }
                    return ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_NON_INCREMENTAL_BUILD_IN_DEP)

                }
                val dirtyData = buildDiff.dirtyData
                symbols.addAll(dirtyData.dirtyLookupSymbols)
                fqNames.addAll(dirtyData.dirtyClassesFqNames)
            }
        }

        return ChangesEither.Known(symbols, fqNames)
    }

    return reporter.measure(BuildTime.IC_ANALYZE_HISTORY_FILES) {
        analyzeHistoryFiles()
    }
}