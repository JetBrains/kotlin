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
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.tokens.ValidityTokenFactory
import org.jetbrains.kotlin.psi.KtElement
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@OptIn(InvalidWayOfUsingAnalysisSession::class)
abstract class CachingKtAnalysisSessionProvider<State : Any>(private val project: Project) : KtAnalysisSessionProvider() {
    private val cache = KtAnalysisSessionCache<Pair<State, KClass<out ValidityToken>>>(project)

    protected abstract fun getResolveState(contextElement: KtElement): State
    protected abstract fun getResolveState(contextSymbol: KtSymbol): State

    protected abstract fun createAnalysisSession(
        resolveState: State,
        validityToken: ValidityToken,
        contextElement: KtElement
    ): KtAnalysisSession

    @InvalidWayOfUsingAnalysisSession
    final override fun getAnalysisSession(contextElement: KtElement, factory: ValidityTokenFactory): KtAnalysisSession {
        val resolveState = getResolveState(contextElement)
        return cache.getAnalysisSession(resolveState to factory.identifier) {
            val validityToken = factory.create(project)
            createAnalysisSession(resolveState, validityToken, contextElement)
        }
    }

    final override fun getAnalysisSessionBySymbol(contextSymbol: KtSymbol): KtAnalysisSession {
        val resolveState = getResolveState(contextSymbol)
        val token = contextSymbol.token
        return getCachedAnalysisSession(resolveState, token)
            ?: error("analysis session was not found for ${contextSymbol::class}, symbol.isValid=${contextSymbol.isValid()}")
    }

    private fun getCachedAnalysisSession(resolveState: State, token: ValidityToken): KtAnalysisSession? {
        return cache.getCachedAnalysisSession(resolveState to token::class)
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