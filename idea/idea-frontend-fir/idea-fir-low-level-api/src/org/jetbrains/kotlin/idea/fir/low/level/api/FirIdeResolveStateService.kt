/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionProvider
import org.jetbrains.kotlin.idea.util.psiModificationTrackerBasedCachedValue
import java.util.concurrent.ConcurrentHashMap

internal class FirIdeResolveStateService(private val project: Project) {
    private val stateCache by psiModificationTrackerBasedCachedValue(project) {
        ConcurrentHashMap<IdeaModuleInfo, FirModuleResolveStateImpl>()
    }

    private fun createResolveStateFor(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl {
        val firPhaseRunner = FirPhaseRunner()
        val sessionProvider = FirIdeSessionProvider(project)

        val librariesSession = FirIdeSessionFactory.createLibrarySession(moduleInfo as ModuleSourceInfo, sessionProvider, project)
        val sourcesSession = FirIdeSessionFactory.createSourcesSession(
            project,
            moduleInfo,
            firPhaseRunner,
            sessionProvider,
            librariesSession,
        )

        sessionProvider.apply {
            setSourcesSession(sourcesSession)
            setLibrariesSession(librariesSession)
        }

        return FirModuleResolveStateImpl(
            moduleInfo,
            sourcesSession,
            librariesSession,
            sessionProvider,
            sourcesSession.firFileBuilder,
            FirLazyDeclarationResolver(sourcesSession.firFileBuilder),
            sourcesSession.cache
        )
    }

    fun getResolveState(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl =
        stateCache.getOrPut(moduleInfo) { createResolveStateFor(moduleInfo) }

    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService = project.service()
    }
}
