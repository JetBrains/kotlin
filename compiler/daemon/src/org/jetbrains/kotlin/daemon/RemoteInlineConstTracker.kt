/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.withMeasure
import org.jetbrains.kotlin.incremental.components.InlineConstTracker

class RemoteInlineConstTracker(
    @Suppress("DEPRECATION") val facade: org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade,
    val profiler: Profiler = DummyProfiler()
): InlineConstTracker {
    override fun report(filePath: String, owner: String, name: String, constType: String) {
        profiler.withMeasure(this) {
            facade.inlineConstTracker_report(filePath, owner, name, constType)
        }
    }
}