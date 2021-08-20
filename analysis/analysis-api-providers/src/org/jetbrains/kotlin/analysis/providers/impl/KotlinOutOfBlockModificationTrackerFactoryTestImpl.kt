/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase

public class KotlinStaticModificationTrackerFactory : KotlinModificationTrackerFactory() {
    private val projectWide = SimpleModificationTracker()
    private val library = SimpleModificationTracker()
    private val forModule = mutableMapOf<ModuleSourceInfoBase, SimpleModificationTracker>()

    override fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker {
        return projectWide
    }


    override fun createModuleWithoutDependenciesOutOfBlockModificationTracker(moduleInfo: ModuleSourceInfoBase): ModificationTracker {
        return forModule.getOrPut(moduleInfo) { SimpleModificationTracker() }
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