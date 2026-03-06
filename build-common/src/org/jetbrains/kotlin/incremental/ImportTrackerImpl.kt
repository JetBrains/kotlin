/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.components.ImportTracker
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
class ImportTrackerImpl: ImportTracker {
    private val filePathToImportedFqNames = ConcurrentHashMap<String, MutableSet<String>>()

    val filePathToImportedFqNamesMap: Map<String, Collection<String>>
        get() = filePathToImportedFqNames

    override fun report(filePath: String, importedFqName: String) {
        filePathToImportedFqNames.getOrPut(filePath) { hashSetOf() }.add(importedFqName)
    }
}