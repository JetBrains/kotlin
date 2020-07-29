/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.trackers

import com.intellij.openapi.module.Module
import com.intellij.openapi.util.ModificationTracker

object ResolutionAnchorModificationCountService {
    fun getLatestModificationCount(anchorModules: Collection<Module>): Long {
        if (anchorModules.isEmpty())
            return ModificationTracker.NEVER_CHANGED.modificationCount

        val modificationCountUpdater =
            KotlinCodeBlockModificationListener.getInstance(anchorModules.first().project).perModuleOutOfCodeBlockTrackerUpdater
        return anchorModules.maxOfOrNull { modificationCountUpdater.getModificationCount(it) }
            ?: ModificationTracker.NEVER_CHANGED.modificationCount
    }
}
