/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule

public abstract class KotlinModificationTrackerFactory {
    public abstract fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker
    public abstract fun createModuleWithoutDependenciesOutOfBlockModificationTracker(module: KtSourceModule): ModificationTracker
    public abstract fun createLibrariesModificationTracker(): ModificationTracker

    @TestOnly
    public abstract fun incrementModificationsCount()
}

public fun Project.createProjectWideOutOfBlockModificationTracker(): ModificationTracker =
    ServiceManager.getService(this, KotlinModificationTrackerFactory::class.java)
        .createProjectWideOutOfBlockModificationTracker()

public fun Project.createLibrariesModificationTracker(): ModificationTracker =
    ServiceManager.getService(this, KotlinModificationTrackerFactory::class.java)
        .createLibrariesModificationTracker()


public fun KtSourceModule.createModuleWithoutDependenciesOutOfBlockModificationTracker(project: Project): ModificationTracker =
    ServiceManager.getService(project, KotlinModificationTrackerFactory::class.java)
        .createModuleWithoutDependenciesOutOfBlockModificationTracker(this)