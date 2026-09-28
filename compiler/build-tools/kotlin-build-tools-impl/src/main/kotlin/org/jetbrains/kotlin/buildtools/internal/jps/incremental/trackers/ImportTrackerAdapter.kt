/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jps.incremental.trackers

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerImportTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker

@OptIn(InternalBuildToolsApi::class)
internal class ImportTrackerAdapter(private val tracker: CompilerImportTracker) : ImportTracker {
    override fun report(filePath: String, importedFqName: String) {
        tracker.report(filePath, importedFqName)
    }
}
