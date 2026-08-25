/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.trackers

import org.jetbrains.kotlin.buildtools.api.jvm.CompilerInlineConstTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker

internal class InlineConstTrackerAdapter(private val tracker: CompilerInlineConstTracker) : InlineConstTracker {
    override fun report(filePath: String, owner: String, name: String, constType: String) {
        tracker.report(filePath, owner, name, constType)
    }
}
