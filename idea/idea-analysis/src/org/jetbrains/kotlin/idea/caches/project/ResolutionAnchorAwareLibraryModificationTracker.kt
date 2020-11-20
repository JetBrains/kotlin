/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionAnchorCacheService
import org.jetbrains.kotlin.idea.caches.trackers.getLatestModificationCount

class ResolutionAnchorAwareLibraryModificationTracker(
    private val libraryInfo: LibraryInfo,
) : ModificationTracker {
    override fun getModificationCount(): Long {
        val anchorDependencyModules = ResolutionAnchorCacheService.getInstance(libraryInfo.project)
            .getDependencyResolutionAnchors(libraryInfo)
            .map { it.module }
        return getLatestModificationCount(anchorDependencyModules)
    }
}
