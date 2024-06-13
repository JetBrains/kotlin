/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.modification

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerFactory

class KotlinStandaloneModificationTrackerFactory : KotlinModificationTrackerFactory() {
    private val projectWide = SimpleModificationTracker()
    private val librariesWide = SimpleModificationTracker()

    override fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker {
        return projectWide
    }

    override fun createLibrariesWideModificationTracker(): ModificationTracker {
        return librariesWide
    }

    internal fun incrementModificationsCount(includeBinaryTrackers: Boolean) {
        projectWide.incModificationCount()
        if (includeBinaryTrackers) {
            librariesWide.incModificationCount()
        }
    }

    companion object {
        fun getInstance(project: Project): KotlinStandaloneModificationTrackerFactory =
            KotlinModificationTrackerFactory.getInstance(project) as KotlinStandaloneModificationTrackerFactory
    }
}
