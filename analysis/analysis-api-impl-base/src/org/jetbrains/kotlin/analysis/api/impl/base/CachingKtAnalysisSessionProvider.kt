/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.psi.KtElement
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@KtAnalysisApiInternals
abstract class CachingKtAnalysisSessionProvider<State : Any>(project: Project) : KtAnalysisSessionProvider(project) {
    private val cache = KtAnalysisSessionCache<Pair<State, KClass<out KtLifetimeToken>>>(project)

    protected abstract fun getFirResolveSession(contextElement: KtElement): State
    protected abstract fun getFirResolveSession(contextSymbol: KtSymbol): State

    protected abstract fun getFirResolveSession(contextModule: KtModule): State

    protected abstract fun createAnalysisSession(
        firResolveSession: State,
        token: KtLifetimeToken,
    ): KtAnalysisSession

    final override fun getAnalysisSession(useSiteKtElement: KtElement, factory: KtLifetimeTokenFactory): KtAnalysisSession {
        val firResolveSession = getFirResolveSession(useSiteKtElement)
        return cache.getAnalysisSession(firResolveSession to factory.identifier) {
            val token = factory.create(project)
            createAnalysisSession(firResolveSession, token)
        }
    }

    final override fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule, factory: KtLifetimeTokenFactory): KtAnalysisSession {
        val firResolveSession = getFirResolveSession(useSiteKtModule)
        return cache.getAnalysisSession(firResolveSession to factory.identifier) {
            val validityToken = factory.create(project)
            createAnalysisSession(firResolveSession, validityToken)
        }
    }

    final override fun getAnalysisSessionBySymbol(contextSymbol: KtSymbol): KtAnalysisSession {
        val firResolveSession = getFirResolveSession(contextSymbol)
        val token = contextSymbol.token
        return getCachedAnalysisSession(firResolveSession, token)
            ?: createAnalysisSession(firResolveSession, contextSymbol.token)
    }

    private fun getCachedAnalysisSession(firResolveSession: State, token: KtLifetimeToken): KtAnalysisSession? {
        return cache.getCachedAnalysisSession(firResolveSession to token::class)
    }

    @TestOnly
    final override fun clearCaches() {
        cache.clear()
    }
}

private class KtAnalysisSessionCache<KEY : Any>(project: Project) {
    private val cache = CachedValuesManager.getManager(project).createCachedValue {
        CachedValueProvider.Result(
            ConcurrentHashMap<KEY, KtAnalysisSession>(),
            PsiModificationTracker.MODIFICATION_COUNT,
            ProjectRootModificationTracker.getInstance(project),
            project.createProjectWideOutOfBlockModificationTracker()
        )
    }

    @TestOnly
    fun clear() {
        cache.value.clear()
    }

    inline fun getAnalysisSession(key: KEY, create: () -> KtAnalysisSession): KtAnalysisSession =
        cache.value.getOrPut(key) { create() }

    fun getCachedAnalysisSession(key: KEY): KtAnalysisSession? =
        cache.value[key]
}