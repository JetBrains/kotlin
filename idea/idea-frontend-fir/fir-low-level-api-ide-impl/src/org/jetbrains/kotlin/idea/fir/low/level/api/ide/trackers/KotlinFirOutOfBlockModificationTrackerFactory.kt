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
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase
import org.jetbrains.kotlin.idea.caches.project.LibraryModificationTracker
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.api.KotlinOutOfBlockModificationTrackerFactory

class KotlinFirOutOfBlockModificationTrackerFactory(private val project: Project) : KotlinOutOfBlockModificationTrackerFactory() {
    override fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker =
        KotlinFirOutOfBlockModificationTracker(project)

    override fun createModuleWithoutDependenciesOutOfBlockModificationTracker(moduleInfo: ModuleSourceInfoBase): ModificationTracker {
        require(moduleInfo is ModuleSourceInfo)
        return KotlinFirOutOfBlockModuleModificationTracker(moduleInfo.module)
    }

    override fun createLibraryOutOfBlockModificationTracker(): ModificationTracker {
        return LibraryModificationTracker.getInstance(project)
    }

    @TestOnly
    override fun incrementModificationsCount() {
        project.service<KotlinFirModificationTrackerService>().increaseModificationCountForAllModules()
    }
}

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