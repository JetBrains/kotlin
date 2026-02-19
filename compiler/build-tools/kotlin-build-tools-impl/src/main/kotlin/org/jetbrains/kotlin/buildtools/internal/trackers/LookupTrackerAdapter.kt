/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.trackers

import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind

internal class LookupTrackerAdapter(private val tracker: CompilerLookupTracker) : LookupTracker {
    override val requiresPosition = false

    override fun record(
        filePath: String,
        position: Position,
        scopeFqName: String,
        scopeKind: ScopeKind,
        name: String,
    ) {
        tracker.recordLookup(
            filePath,
            scopeFqName,
            CompilerLookupTracker.ScopeKind.valueOf(scopeKind.name),
            name
        )
    }

    override fun clear() {
        tracker.clear()
    }
}