/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.dirtyFiles

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.ChangedFiles.DeterminableFiles
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.parsing.classesFqNames
import org.jetbrains.kotlin.incremental.snapshots.isLibrarySetAddedSentinel
import org.jetbrains.kotlin.incremental.snapshots.isLibrarySetRemovedSentinel
import org.jetbrains.kotlin.incremental.util.Either
import org.jetbrains.kotlin.name.FqName
import java.io.File

internal fun getClasspathChanges(
    classpath: List<File>,
    changedFiles: DeterminableFiles.Known,
    lastBuildInfo: BuildInfo,
    modulesApiHistory: ModulesApiHistory,
    reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
    abiSnapshots: Map<String, AbiSnapshot>,
    withSnapshot: Boolean,
    caches: IncrementalCacheCommon,
    scopes: Collection<String>
): ChangesEither {
    val classpathSet = expandClasspathFiles(classpath)

    // additionally, declared libraries adds/removals are covered by tracking compiler arguments
    // either on the client side or by enabling our tracking of the configuration inputs.
    // Sentinels emitted by LibrarySetSnapshotMap encode classpath set membership changes (adds in `modified`,
    // removes in `removed`) that don't correspond to real paths; they're explicitly admitted here so library
    // add/remove triggers a full rebuild as intended.
    val addedClasspath = changedFiles.modified.filterTo(HashSet()) { it.isLibrarySetAddedSentinel() }
    val modifiedClasspath = changedFiles.modified.filterTo(HashSet()) { it in classpathSet }
    val removedClasspath = changedFiles.removed.filterTo(HashSet()) { it in classpathSet || it.isLibrarySetRemovedSentinel() }

    // todo: removed classes could be processed normally
    if (removedClasspath.isNotEmpty()) {
        reporter.info { "Some files are removed from classpath: $removedClasspath" }
        return ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_REMOVED_ENTRY)
    }

    if (addedClasspath.isNotEmpty()) {
        // We've never compiled against these libraries, so any incremental delta from their own histories
        // would be partial — fall back to a full rebuild. See LibrarySetSnapshotMap for the sentinel mechanism.
        reporter.info { "New entries are added to classpath: $addedClasspath" }
        return ChangesEither.Unknown(BuildAttribute.DEP_CHANGE_ADDED_ENTRY)
    }

    if (modifiedClasspath.isEmpty()) return ChangesEither.Known()

    if (withSnapshot) {
        fun analyzeJarFiles(): ChangesEither {
            val symbols = HashSet<LookupSymbol>()
            val fqNames = HashSet<FqName>()

            for ([module, abiSnapshot] in abiSnapshots) {
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
        return reporter.measure(IC_ANALYZE_JAR_FILES) {
            analyzeJarFiles()
        }
    } else {
        val lastBuildTS = lastBuildInfo.startTS

        val symbols = HashSet<LookupSymbol>()
        val fqNames = HashSet<FqName>()

        val historyFilesEither =
            reporter.measure(IC_FIND_HISTORY_FILES) {
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

                val [knownBuilds, newBuilds] = allBuilds.partition { it.ts <= lastBuildTS }
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

        return reporter.measure(IC_ANALYZE_HISTORY_FILES) {
            analyzeHistoryFiles()
        }
    }
}

/**
 * Expands a classpath into the set of file leaves that the IC machinery considers individually.
 */
internal fun expandClasspathFiles(classpath: List<File>): HashSet<File> {
    val result = HashSet<File>()
    val klibManifestPath = "default/manifest"
    for (file in classpath) {
        when {
            file.isFile -> result.add(file)
            // if it's not a file, then it's a non-packed KLIB directory. The manifest contains KLIB fingerprints, so we can rely on snapshotting this file.
            file.isDirectory && file.resolve(klibManifestPath).exists() -> result.add(file.resolve(klibManifestPath))
            // if it's not a non-packed directory, it might be a class directory in the past.
            // Though history files-based IC is used only for KLIB-based backends now, so let's fail here.
            else -> error("Unexpected state: $file is neither a KLIB file nor a KLIB directory")
        }
    }
    return result
}

internal fun getRemovedClassesChanges(
    caches: IncrementalCachesManager<*>,
    changedFiles: DeterminableFiles.Known,
    kotlinSourceFilesExtensions: Set<String>,
    reporter: ICReporter,
): DirtyData {
    val removedClasses = HashSet<String>()
    val dirtyFiles = changedFiles.modified.filterTo(HashSet()) { it.isKotlinFile(kotlinSourceFilesExtensions) }
    val removedFiles = changedFiles.removed.filterTo(HashSet()) { it.isKotlinFile(kotlinSourceFilesExtensions) }

    val existingClasses = classesFqNames(dirtyFiles)
    val previousClasses = caches.platformCache
        .classesFqNamesBySources(dirtyFiles + removedFiles)
        .map { it.asString() }

    for (fqName in previousClasses) {
        if (fqName !in existingClasses) {
            removedClasses.add(fqName)
        }
    }

    val changesCollector = ChangesCollector()
    removedClasses.forEach { changesCollector.collectSignature(FqName(it), areSubclassesAffected = true) }
    return changesCollector.getChangedAndImpactedSymbols(listOf(caches.platformCache), reporter)
}
