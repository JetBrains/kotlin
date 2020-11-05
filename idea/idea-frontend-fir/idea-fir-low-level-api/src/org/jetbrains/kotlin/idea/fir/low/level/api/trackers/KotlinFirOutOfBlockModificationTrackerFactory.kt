/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trackers

import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.annotations.TestOnly

class KotlinFirOutOfBlockModificationTrackerFactory(private val project: Project) {
    fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker =
        KotlinFirOutOfBlockModificationTracker(project)

    fun createModuleWithoutDependenciesOutOfBlockModificationTracker(module: Module): ModificationTracker =
        KotlinFirOutOfBlockModuleModificationTracker(module)

    @TestOnly
    fun incrementModificationsCount() {
        project.service<KotlinFirModificationTrackerService>().incrementModificationsCount()
    }
}

fun Project.createProjectWideOutOfBlockModificationTracker() =
    service<KotlinFirOutOfBlockModificationTrackerFactory>().createProjectWideOutOfBlockModificationTracker()

fun Module.createModuleWithoutDependenciesOutOfBlockModificationTracker() =
    project.service<KotlinFirOutOfBlockModificationTrackerFactory>().createModuleWithoutDependenciesOutOfBlockModificationTracker(this)

private class KotlinFirOutOfBlockModificationTracker(project: Project) : ModificationTracker {
    private val trackerService = project.service<KotlinFirModificationTrackerService>()

    override fun getModificationCount(): Long =
        trackerService.projectGlobalOutOfBlockInKotlinFilesModificationCount
}

private class KotlinFirOutOfBlockModuleModificationTracker(private val module: Module) : ModificationTracker {
    private val trackerService = module.project.service<KotlinFirModificationTrackerService>()

    override fun getModificationCount(): Long =
        trackerService.getOutOfBlockModificationCountForModules(module)
}