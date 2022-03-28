/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.withMeasure
import org.jetbrains.kotlin.incremental.components.ReflektTracker
import java.io.File

class RemoteReflektTracker(
    @Suppress("DEPRECATION") val facade: org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade,
    val profiler: Profiler = DummyProfiler()
) : ReflektTracker {
    override fun report(fileSearchedByReflect: File, reflektUsageFile: File) {
        profiler.withMeasure(this) {
            facade.reflektTracker_report(fileSearchedByReflect.path, reflektUsageFile.path)
        }
    }
}