/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jps.incremental.trackers

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerInlineConstTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker

@OptIn(InternalBuildToolsApi::class)
internal class InlineConstTrackerAdapter(private val tracker: CompilerInlineConstTracker) : InlineConstTracker {
    override fun report(filePath: String, owner: String, name: String, constType: String) {
        tracker.report(filePath, owner, name, constType)
    }
}
