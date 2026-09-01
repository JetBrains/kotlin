/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.trackers

import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.buildtools.api.jvm.CompilerImportTracker as ApiCompilerImportTracker

/**
 * Adapts the import tracker of [org.jetbrains.kotlin.buildtools.api.jvm.JvmClientManagedIncrementalCompilationConfiguration].
 *
 * Not to be confused with [ImportTrackerAdapter], which adapts the impl-only [CompilerImportTracker] behind the
 * undeclared `IMPORT_TRACKER` operation option.
 */
internal class ServicesImportTrackerAdapter(private val tracker: ApiCompilerImportTracker) : ImportTracker {
    override fun report(filePath: String, importedFqName: String) {
        tracker.report(filePath, importedFqName)
    }
}
