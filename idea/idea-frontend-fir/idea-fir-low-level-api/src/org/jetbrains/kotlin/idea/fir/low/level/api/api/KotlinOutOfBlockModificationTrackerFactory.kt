/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase


abstract class KotlinOutOfBlockModificationTrackerFactory {
    abstract fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker
    abstract fun createModuleWithoutDependenciesOutOfBlockModificationTracker(moduleInfo: ModuleSourceInfoBase): ModificationTracker
    abstract fun createLibraryOutOfBlockModificationTracker(): ModificationTracker
}

fun Project.createProjectWideOutOfBlockModificationTracker() =
    ServiceManager.getService(this, KotlinOutOfBlockModificationTrackerFactory::class.java).createProjectWideOutOfBlockModificationTracker()

fun Project.createLibraryOutOfBlockModificationTracker() =
    ServiceManager.getService(this, KotlinOutOfBlockModificationTrackerFactory::class.java).createLibraryOutOfBlockModificationTracker()


fun ModuleSourceInfoBase.createModuleWithoutDependenciesOutOfBlockModificationTracker(project: Project) =
    ServiceManager.getService(project, KotlinOutOfBlockModificationTrackerFactory::class.java)
        .createModuleWithoutDependenciesOutOfBlockModificationTracker(this)