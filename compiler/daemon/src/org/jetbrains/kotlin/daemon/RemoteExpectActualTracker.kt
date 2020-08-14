/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.withMeasure
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import java.io.File

class RemoteExpectActualTracker(
    @Suppress("DEPRECATION") val facade: org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade,
    val profiler: Profiler = DummyProfiler()
): ExpectActualTracker {
    override fun report(expectedFile: File, actualFile: File) {
        profiler.withMeasure(this) {
            facade.expectActualTracker_report(expectedFile.path, actualFile.path)
        }
    }
}