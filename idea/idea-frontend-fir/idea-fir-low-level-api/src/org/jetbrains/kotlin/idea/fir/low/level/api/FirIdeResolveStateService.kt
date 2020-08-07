/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.util.psiModificationTrackerBasedCachedValue
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.kotlin.idea.caches.project.*

internal class FirIdeResolveStateService(private val project: Project) {
    private val stateCache by psiModificationTrackerBasedCachedValue(project) {
        ConcurrentHashMap<IdeaModuleInfo, FirModuleResolveStateImpl>()
    }

    private fun createResolveStateFor(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl {
        val sessionProvider = FirIdeSessionProvider(project)
        val firPhaseRunner = FirPhaseRunner()
        val session = FirIdeJavaModuleBasedSession.create(
            project,
            moduleInfo as ModuleSourceInfo,
            firPhaseRunner,
            sessionProvider,
        ).apply { sessionProvider.registerSession(moduleInfo, this) }

        return FirModuleResolveStateImpl(moduleInfo, session, sessionProvider, session.firFileBuilder, session.cache)
    }

    fun getResolveState(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl =
        stateCache.getOrPut(moduleInfo) { createResolveStateFor(moduleInfo) }

    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService = project.service()
    }
}