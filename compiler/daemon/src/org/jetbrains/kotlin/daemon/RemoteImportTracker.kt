/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.withMeasure
import org.jetbrains.kotlin.incremental.components.ImportTracker

class RemoteImportTracker(
    @Suppress("DEPRECATION") val facade: CompilerCallbackServicesFacade,
    val profiler: Profiler = DummyProfiler()
) : ImportTracker {
    override fun report(filePath: String, importedFqName: String) {
        profiler.withMeasure(this) {
            facade.importTracker_report(filePath, importedFqName)
        }
    }
}
