/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jps.incremental.trackers

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerExpectActualTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import java.io.File

@OptIn(InternalBuildToolsApi::class)
internal class ExpectActualTrackerAdapter(private val tracker: CompilerExpectActualTracker) : ExpectActualTracker {
    override fun report(expectedFile: File, actualFile: File) {
        tracker.report(expectedFile.path, actualFile.path)
    }

    override fun reportExpectOfLenientStub(expectedFile: File) {
        tracker.reportExpectOfLenientStub(expectedFile.path)
    }
}
