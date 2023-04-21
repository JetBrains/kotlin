/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory

public class KotlinStaticModificationTrackerFactory : KotlinModificationTrackerFactory() {
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

    public companion object {
        public fun getInstance(project: Project): KotlinStaticModificationTrackerFactory =
            KotlinModificationTrackerFactory.getInstance(project) as KotlinStaticModificationTrackerFactory
    }
}
