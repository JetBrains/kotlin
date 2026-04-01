/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.trackers

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.incremental.components.ImportTracker


/**
 * A tracker for the compiler to report information about imports.
 *
 * It is necessary because information about imports is not available during bytecode analysis after compilation ended.
 *
 * This is an experimental import tracking API. We do not expect it to be used publicly.
 * It may be promoted to stable public API in the future.
 * It is public for easier usage in tests. It is safe because it is located only in impl artifact.
 */
@ExperimentalBuildToolsApi
public interface CompilerImportTracker {
    public fun report(filePath: String, importedFqName: String)
}

internal class ImportTrackerAdapter(private val tracker: CompilerImportTracker) : ImportTracker {
    override fun report(filePath: String, importedFqName: String) {
        tracker.report(filePath, importedFqName)
    }
}