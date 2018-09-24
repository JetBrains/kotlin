/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.util.Either
import org.jetbrains.kotlin.name.FqName
import java.io.File

internal fun getClasspathChanges(
    classpath: List<File>,
    changedFiles: ChangedFiles.Known,
    lastBuildInfo: BuildInfo,
    modulesApiHistory: ModulesApiHistory,
    reporter: ICReporter?
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
    if (removedClasspath.isNotEmpty()) return ChangesEither.Unknown("Some files are removed from classpath $removedClasspath")

    if (modifiedClasspath.isEmpty()) return ChangesEither.Known()

    val lastBuildTS = lastBuildInfo.startTS

    val symbols = HashSet<LookupSymbol>()
    val fqNames = HashSet<FqName>()

    val historyFilesEither = modulesApiHistory.historyFilesForChangedFiles(modifiedClasspath)
    val historyFiles = when (historyFilesEither) {
        is Either.Success<Set<File>> -> historyFilesEither.value
        is Either.Error -> return ChangesEither.Unknown(historyFilesEither.reason)
    }

    for (historyFile in historyFiles) {
        val allBuilds = BuildDiffsStorage.readDiffsFromFile(historyFile, reporter = reporter)
            ?: return ChangesEither.Unknown("Could not read diffs from $historyFile")
        val (knownBuilds, newBuilds) = allBuilds.partition { it.ts <= lastBuildTS }
        if (knownBuilds.isEmpty()) {
            return ChangesEither.Unknown("No previously known builds for $historyFile")
        }

        for (buildDiff in newBuilds) {
            if (!buildDiff.isIncremental) return ChangesEither.Unknown("Non-incremental build from dependency $historyFile")

            val dirtyData = buildDiff.dirtyData
            symbols.addAll(dirtyData.dirtyLookupSymbols)
            fqNames.addAll(dirtyData.dirtyClassesFqNames)
        }
    }

    return ChangesEither.Known(symbols, fqNames)
}