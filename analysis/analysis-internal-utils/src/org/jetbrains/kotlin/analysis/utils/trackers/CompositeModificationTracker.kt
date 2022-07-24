/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.trackers

import com.intellij.openapi.util.ModificationTracker

public class CompositeModificationTracker private constructor(private val trackers: List<ModificationTracker>) : ModificationTracker {
    override fun getModificationCount(): Long = trackers.sumOf { it.modificationCount }

    public companion object {
        public fun create(trackers: List<ModificationTracker>): ModificationTracker = when (trackers.size) {
            0 -> ModificationTracker.NEVER_CHANGED
            1 -> trackers.single()
            else -> CompositeModificationTracker(trackers)
        }
    }
}