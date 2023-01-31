/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.LowMemoryWatcher
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.utils.caches.SoftCachedMap
import org.jetbrains.kotlin.psi.KtElement
import kotlin.reflect.KClass

@KtAnalysisApiInternals
abstract class CachingKtAnalysisSessionProvider<State : Any>(project: Project) : KtAnalysisSessionProvider(project) {
    private val cache = KtAnalysisSessionCache(project)

    protected abstract fun getFirResolveSession(contextModule: KtModule): State

    protected abstract fun createAnalysisSession(
        firResolveSession: State,
        token: KtLifetimeToken,
    ): KtAnalysisSession

    final override fun getAnalysisSession(useSiteKtElement: KtElement, factory: KtLifetimeTokenFactory): KtAnalysisSession {
        return getAnalysisSessionByUseSiteKtModule(useSiteKtElement.getKtModule(project), factory)
    }

    final override fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule, factory: KtLifetimeTokenFactory): KtAnalysisSession {
        return cache.getAnalysisSession(useSiteKtModule to factory.identifier) {
            val firResolveSession = getFirResolveSession(useSiteKtModule)
            val validityToken = factory.create(project)
            createAnalysisSession(firResolveSession, validityToken)
        }
    }

    @TestOnly
    final override fun clearCaches() {
        cache.clear()
    }
}

private class KtAnalysisSessionCache(project: Project) {
    private val cache = SoftCachedMap.create<Pair<KtModule, KClass<out KtLifetimeToken>>, KtAnalysisSession>(
        project,
        SoftCachedMap.Kind.STRONG_KEYS_SOFT_VALUES,
        listOf(
            PsiModificationTracker.MODIFICATION_COUNT,
            ProjectRootModificationTracker.getInstance(project),
            project.createProjectWideOutOfBlockModificationTracker()
        )
    )

    init {
        LowMemoryWatcher.register({ cache.clearCachedValues() }, project)
    }

    @TestOnly
    fun clear() {
        cache.clear()
    }

    fun getAnalysisSession(
        key: Pair<KtModule, KClass<out KtLifetimeToken>>,
        create: () -> KtAnalysisSession
    ): KtAnalysisSession =
        cache.getOrPut(key) { create() }
}