/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.name.FqName
import java.io.File

class DirtyFilesContainer(
    private val caches: IncrementalCachesManager<*>,
    private val reporter: ICReporter
) {
    private val myDirtyFiles = HashSet<File>()

    fun toMutableList(): MutableList<File> =
        ArrayList(myDirtyFiles)

    fun add(files: Iterable<File>) {
        val existingKotlinFiles = files.filter { it.isKotlinFile() }
        if (existingKotlinFiles.isNotEmpty()) {
            myDirtyFiles.addAll(existingKotlinFiles)
        }
    }

    fun addByDirtySymbols(lookupSymbols: Collection<LookupSymbol>) {
        if (lookupSymbols.isEmpty()) return

        val dirtyFilesFromLookups = mapLookupSymbolsToFiles(caches.lookupCache, lookupSymbols, reporter)
        add(dirtyFilesFromLookups)
    }

    fun addByDirtyClasses(dirtyClassesFqNames: Collection<FqName>) {
        if (dirtyClassesFqNames.isEmpty()) return

        val fqNamesWithSubtypes = dirtyClassesFqNames.flatMap {
            withSubtypes(
                it,
                listOf(caches.platformCache)
            )
        }
        val dirtyFilesFromFqNames =
            mapClassesFqNamesToFiles(listOf(caches.platformCache), fqNamesWithSubtypes, reporter)
        add(dirtyFilesFromFqNames)
    }
}