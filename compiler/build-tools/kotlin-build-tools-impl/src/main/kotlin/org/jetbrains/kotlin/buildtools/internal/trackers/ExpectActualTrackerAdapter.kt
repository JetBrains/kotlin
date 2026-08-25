/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.trackers

import org.jetbrains.kotlin.buildtools.api.jvm.CompilerExpectActualTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import java.io.File

/*
 * The API mirror exchanges file paths as strings, so that the API module does not have to commit to `java.io.File`.
 * `File.path` and `File(String)` round-trip a path unchanged, so the conversions below are lossless.
 */
internal class ExpectActualTrackerAdapter(private val tracker: CompilerExpectActualTracker) : ExpectActualTracker {
    override fun report(expectedFile: File, actualFile: File) {
        tracker.report(expectedFile.path, actualFile.path)
    }

    override fun reportExpectOfLenientStub(expectedFile: File) {
        tracker.reportExpectOfLenientStub(expectedFile.path)
    }
}
