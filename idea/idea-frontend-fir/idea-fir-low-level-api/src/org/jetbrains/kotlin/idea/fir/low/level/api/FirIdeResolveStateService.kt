/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.trackers.KotlinCodeBlockModificationListener
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionProviderStorage
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSourcesSession
import org.jetbrains.kotlin.idea.util.cachedValue
import org.jetbrains.kotlin.idea.util.getValue
import java.util.concurrent.ConcurrentHashMap

internal class FirIdeResolveStateService(project: Project) {
    private val sessionProviderStorage = FirIdeSessionProviderStorage(project)

    private val stateCache by cachedValue(project, KotlinCodeBlockModificationListener.getInstance(project).kotlinOutOfCodeBlockTracker) {
        ConcurrentHashMap<IdeaModuleInfo, FirModuleResolveStateImpl>()
    }

    private fun createResolveStateFor(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl {
        require(moduleInfo is ModuleSourceInfo)
        val sessionProvider = sessionProviderStorage.getSessionProvider(moduleInfo)
        val firFileBuilder = sessionProvider.rootModuleSession.firFileBuilder
        return FirModuleResolveStateImpl(
            moduleInfo,
            sessionProvider,
            firFileBuilder,
            FirLazyDeclarationResolver(firFileBuilder),
        )
    }

    fun getResolveState(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl =
        stateCache.computeIfAbsent(moduleInfo) { createResolveStateFor(moduleInfo) }

    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService = project.service()
    }
}
