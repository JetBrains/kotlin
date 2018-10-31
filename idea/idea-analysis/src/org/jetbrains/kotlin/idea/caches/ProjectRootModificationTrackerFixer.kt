/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches

import com.intellij.ProjectTopics
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ProjectRootManager

// This is a workaround for IDEA < 183. For details, see IDEA-200525.
class ProjectRootModificationTrackerFixer(val project: Project) : ProjectComponent {
    override fun initComponent() {
        project.messageBus.connect(project).subscribe(
            ProjectTopics.PROJECT_ROOTS,
            object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) {
                    // Forcefully increment modification counter. This would cause invalidation of
                    // all caches that depend on ProjectRootModificationTracker tracker.
                    ProjectRootManager.getInstance(project).incModificationCount()
                }
            }
        )
    }
}
