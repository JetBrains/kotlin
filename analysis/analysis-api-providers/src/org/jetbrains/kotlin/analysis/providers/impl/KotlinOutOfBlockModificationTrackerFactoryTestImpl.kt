/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory

public class KotlinStaticModificationTrackerFactory : KotlinModificationTrackerFactory() {
    private val projectWide = SimpleModificationTracker()
    private val library = SimpleModificationTracker()
    private val forModule = mutableMapOf<KtSourceModule, SimpleModificationTracker>()

    override fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker {
        return projectWide
    }


    override fun createModuleWithoutDependenciesOutOfBlockModificationTracker(module: KtSourceModule): ModificationTracker {
        return forModule.getOrPut(module) { SimpleModificationTracker() }
    }

    override fun createLibrariesModificationTracker(): ModificationTracker {
        return library
    }

    @TestOnly
    override fun incrementModificationsCount() {
        projectWide.incModificationCount()
        library.incModificationCount()
        forModule.values.forEach { it.incModificationCount() }
    }
}