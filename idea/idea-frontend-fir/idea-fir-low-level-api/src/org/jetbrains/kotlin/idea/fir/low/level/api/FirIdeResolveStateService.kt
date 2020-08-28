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
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeModuleLibraryDependenciesSession

internal class FirIdeResolveStateService(private val project: Project) {
    private val stateCache by psiModificationTrackerBasedCachedValue(project) {
        ConcurrentHashMap<IdeaModuleInfo, FirModuleResolveStateImpl>()
    }

    private fun createResolveStateFor(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl {
        val sessionProvider = FirIdeSessionProvider(project)
        val firPhaseRunner = FirPhaseRunner()

        val librariesSession = FirIdeModuleLibraryDependenciesSession.create(moduleInfo as ModuleSourceInfo, sessionProvider, project)

        val sourcesSession = FirIdeJavaModuleBasedSession.create(
            project,
            moduleInfo,
            firPhaseRunner,
            sessionProvider,
            librariesSession,
        ).apply { sessionProvider.registerSession(moduleInfo, this) }


        return FirModuleResolveStateImpl(
            moduleInfo,
            sourcesSession,
            librariesSession,
            sessionProvider,
            sourcesSession.firFileBuilder,
            sourcesSession.cache
        )
    }

    fun getResolveState(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl =
        stateCache.getOrPut(moduleInfo) { createResolveStateFor(moduleInfo) }

    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService = project.service()
    }
}