/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntry
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntryTable
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.Comparator

class ImportPathComparator(
    private val packageTable: KotlinPackageEntryTable
) : Comparator<ImportPath> {
    override fun compare(import1: ImportPath, import2: ImportPath): Int {
        val index1 = bestMatchIndex(import1)
        val index2 = bestMatchIndex(import2)

        return when {
            index1 == index2 -> import1.toString().compareTo(import2.toString())
            index1 < index2 -> -1
            else -> +1
        }
    }

    private fun bestMatchIndex(path: ImportPath): Int {
        var bestIndex: Int? = null
        var bestEntryMatch: KotlinPackageEntry? = null

        for ((index, entry) in packageTable.getEntries().withIndex()) {
            if (entry.isBetterMatchForPackageThan(bestEntryMatch, path)) {
                bestEntryMatch = entry
                bestIndex = index
            }
        }

        return bestIndex!!
    }
}