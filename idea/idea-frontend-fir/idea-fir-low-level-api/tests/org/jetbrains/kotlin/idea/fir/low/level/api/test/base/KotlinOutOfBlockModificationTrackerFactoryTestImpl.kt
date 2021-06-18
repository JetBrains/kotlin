/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.test.base

import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase
import org.jetbrains.kotlin.idea.fir.low.level.api.api.KotlinOutOfBlockModificationTrackerFactory

internal class KotlinOutOfBlockModificationTrackerFactoryTestImpl : KotlinOutOfBlockModificationTrackerFactory() {
    private val projectWide = SimpleModificationTracker()
    private val library = SimpleModificationTracker()
    private val forModule = mutableMapOf<ModuleSourceInfoBase, ModificationTracker>()

    override fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker {
        return projectWide
    }


    override fun createModuleWithoutDependenciesOutOfBlockModificationTracker(moduleInfo: ModuleSourceInfoBase): ModificationTracker {
        return forModule.getOrPut(moduleInfo) { SimpleModificationTracker() }
    }

    override fun createLibraryOutOfBlockModificationTracker(): ModificationTracker {
        return library
    }
}