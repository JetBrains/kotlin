/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.dirtyFiles

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.events.ScopeExpansionReason
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.name.FqName
import java.io.File

class DirtyFilesContainer(
    private val caches: IncrementalCachesManager<*>,
    private val reporter: ICReporter,
    private val sourceFilesExtensions: Set<String>
) {
    private val myDirtyFiles = HashSet<File>()

    fun isEmpty() = myDirtyFiles.isEmpty()

    fun toMutableLinkedSet(): LinkedHashSet<File> =
        LinkedHashSet(myDirtyFiles)

    fun add(files: Iterable<File>, reason: ScopeExpansionReason) {
        val existingKotlinFiles = files.filter { it.isKotlinFile(sourceFilesExtensions) }
        if (existingKotlinFiles.isNotEmpty()) {
            myDirtyFiles.addAll(existingKotlinFiles)
            reporter.reportMarkDirty(existingKotlinFiles, reason.readableString)
        }
    }

    fun addByDirtySymbols(lookupSymbols: Collection<LookupSymbol>, reason: ScopeExpansionReason = ScopeExpansionReason.UNKNOWN) {
        if (lookupSymbols.isEmpty()) return

        val dirtyFilesFromLookups = mapLookupSymbolsToFiles(caches.lookupCache, lookupSymbols, reporter)
        // reason is null, because files are reported in mapLookupSymbolsToFiles
        add(dirtyFilesFromLookups, reason = reason)
    }

    fun addByDirtyClasses(dirtyClassesFqNames: Collection<FqName>, reason: ScopeExpansionReason = ScopeExpansionReason.UNKNOWN) {
        if (dirtyClassesFqNames.isEmpty()) return

        val fqNamesWithSubtypes = dirtyClassesFqNames.flatMap {
            withSubtypes(
                it,
                listOf(caches.platformCache)
            )
        }
        val dirtyFilesFromFqNames =
            mapClassesFqNamesToFiles(listOf(caches.platformCache), fqNamesWithSubtypes, reporter)
        // reason is null, because files are reported in mapClassesFqNamesToFiles
        add(dirtyFilesFromFqNames, reason = reason)
    }
}
