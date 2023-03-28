/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.KtModuleStateTracker

public class KotlinStaticModificationTrackerFactory : KotlinModificationTrackerFactory() {
    private val projectWide = SimpleModificationTracker()
    private val librariesWide = SimpleModificationTracker()
    private val moduleOutOfBlock = mutableMapOf<KtSourceModule, SimpleModificationTracker>()
    private val moduleState = mutableMapOf<KtModule, KtModuleStateTrackerImpl>()

    override fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker {
        return projectWide
    }


    override fun createModuleWithoutDependenciesOutOfBlockModificationTracker(module: KtSourceModule): ModificationTracker {
        return moduleOutOfBlock.getOrPut(module) { SimpleModificationTracker() }
    }

    override fun createLibrariesWideModificationTracker(): ModificationTracker {
        return librariesWide
    }

    override fun createModuleStateTracker(module: KtModule): KtModuleStateTracker {
        return moduleState.getOrPut(module) { KtModuleStateTrackerImpl() }
    }

    @TestOnly
    override fun incrementModificationsCount(includeBinaryTrackers: Boolean) {
        projectWide.incModificationCount()
        if (includeBinaryTrackers) {
            librariesWide.incModificationCount()
        }
        moduleOutOfBlock.values.forEach { it.incModificationCount() }
        moduleState.entries.forEach { (ktModule, tracker) ->
            if (ktModule is KtBinaryModule && !includeBinaryTrackers) return@forEach
            tracker.incModificationCount()
        }
    }
}

private class KtModuleStateTrackerImpl: KtModuleStateTracker {
    override val isValid: Boolean get() = true

    private var _rootModificationCount = 0L

    override val rootModificationCount: Long get() = _rootModificationCount

    @TestOnly
    fun incModificationCount() {
        _rootModificationCount++
    }
}