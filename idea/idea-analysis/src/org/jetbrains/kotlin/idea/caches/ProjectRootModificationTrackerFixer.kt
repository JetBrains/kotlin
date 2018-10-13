/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches

import com.intellij.ProjectTopics
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ProjectRootManager

// This is a workaround until IDEA-200525 is fixed.
class ProjectRootModificationTrackerFixer(project: Project) : AbstractProjectComponent(project) {

    override fun initComponent() {
        myProject.messageBus.connect(myProject).subscribe(
            ProjectTopics.PROJECT_ROOTS,
            object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) {
                    // Forcefully increment modification counter. This would cause invalidation of
                    // all caches that depend on ProjectRootModificationTracker tracker.
                    ProjectRootManager.getInstance(myProject).incModificationCount()
                }
            }
        )
    }
}
