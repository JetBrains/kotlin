/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntry
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntry.Companion.ALL_OTHER_ALIAS_IMPORTS_ENTRY
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntry.Companion.ALL_OTHER_IMPORTS_ENTRY
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntryTable
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.Comparator

internal class ImportPathComparator(private val packageTable: KotlinPackageEntryTable) : Comparator<ImportPath> {

    override fun compare(import1: ImportPath, import2: ImportPath): Int {
        val ignoreAlias = import1.hasAlias() && import2.hasAlias()

        return compareValuesBy(
            import1,
            import2,
            { import -> bestEntryMatchIndex(import, ignoreAlias) },
            { import -> import.toString() }
        )
    }

    private fun bestEntryMatchIndex(path: ImportPath, ignoreAlias: Boolean): Int {
        var bestEntryMatch: KotlinPackageEntry? = null
        var bestIndex: Int = -1

        for ((index, entry) in packageTable.getEntries().withIndex()) {
            if (entry.isBetterMatchForPackageThan(bestEntryMatch, path, ignoreAlias)) {
                bestEntryMatch = entry
                bestIndex = index
            }
        }

        return bestIndex
    }
}

private fun KotlinPackageEntry.isBetterMatchForPackageThan(entry: KotlinPackageEntry?, path: ImportPath, ignoreAlias: Boolean): Boolean {
    if (!matchesImportPath(path, ignoreAlias)) return false
    if (entry == null) return true

    // Any matched package is better than ALL_OTHER_IMPORTS_ENTRY
    if (this == ALL_OTHER_IMPORTS_ENTRY) return false
    if (entry == ALL_OTHER_IMPORTS_ENTRY) return true

    if (entry.withSubpackages != withSubpackages) return !withSubpackages

    return entry.packageName.count { it == '.' } < packageName.count { it == '.' }
}

/**
 * In current implementation we assume that aliased import can be matched only by
 * [ALL_OTHER_ALIAS_IMPORTS_ENTRY] which is always present.
 */
private fun KotlinPackageEntry.matchesImportPath(importPath: ImportPath, ignoreAlias: Boolean): Boolean {
    if (!ignoreAlias && importPath.hasAlias()) {
        return this == ALL_OTHER_ALIAS_IMPORTS_ENTRY
    }

    if (this == ALL_OTHER_IMPORTS_ENTRY) return true

    return matchesPackageName(importPath.pathStr)
}
