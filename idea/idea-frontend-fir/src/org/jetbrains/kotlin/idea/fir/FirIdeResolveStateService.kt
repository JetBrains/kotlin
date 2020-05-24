/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analyzer.TrackableModuleInfo
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo

interface FirIdeResolveStateService {
    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService =
            ServiceManager.getService(project, FirIdeResolveStateService::class.java)!!
    }

    val fallbackModificationTracker: ModificationTracker?

    fun getResolveState(moduleInfo: IdeaModuleInfo): FirModuleResolveState
}

private class FirModuleData(val state: FirModuleResolveState, val modificationTracker: ModificationTracker?) {
    val modificationCount: Long = modificationTracker?.modificationCount ?: Long.MIN_VALUE

    fun isOutOfDate(): Boolean {
        val currentModCount = modificationTracker?.modificationCount
        return currentModCount != null && currentModCount > modificationCount
    }
}

class FirIdeResolveStateServiceImpl(val project: Project) : FirIdeResolveStateService {
    private val stateCache = mutableMapOf<IdeaModuleInfo, FirModuleData>()

    private fun createResolveState(): FirModuleResolveState {
        val provider = FirProjectSessionProvider(project)
        return FirModuleResolveStateImpl(provider)
    }

    private fun createModuleData(moduleInfo: IdeaModuleInfo): FirModuleData {
        val state = createResolveState()
        val modificationTracker = (moduleInfo as? TrackableModuleInfo)?.createModificationTracker() ?: fallbackModificationTracker
        return FirModuleData(state, modificationTracker)
    }

    // TODO: multi thread protection
    override fun getResolveState(moduleInfo: IdeaModuleInfo): FirModuleResolveState {
        var moduleData = stateCache.getOrPut(moduleInfo) {
            createModuleData(moduleInfo)
        }
        if (moduleData.isOutOfDate()) {
            moduleData = createModuleData(moduleInfo)
            stateCache[moduleInfo] = moduleData
        }
        return moduleData.state
    }

    override val fallbackModificationTracker: ModificationTracker? =
        org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService.getInstance(project).outOfBlockModificationTracker
}