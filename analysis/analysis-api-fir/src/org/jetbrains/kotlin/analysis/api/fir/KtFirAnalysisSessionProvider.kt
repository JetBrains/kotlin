/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.LowMemoryWatcher
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.CachedValueBase
import com.intellij.util.containers.CollectionFactory
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.psi.KtElement
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.KClass

@OptIn(KtAnalysisApiInternals::class)
class KtFirAnalysisSessionProvider(project: Project) : KtAnalysisSessionProvider(project) {
    private val cache: ConcurrentMap<Pair<KtModule, KClass<out KtLifetimeToken>>, CachedValue<KtAnalysisSession>> =
        CollectionFactory.createConcurrentWeakValueMap()

    init {
        LowMemoryWatcher.register(::clearCaches, project)
    }

    override fun getAnalysisSession(useSiteKtElement: KtElement, factory: KtLifetimeTokenFactory): KtAnalysisSession {
        return getAnalysisSessionByUseSiteKtModule(useSiteKtElement.getKtModule(project), factory)
    }

    override fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule, factory: KtLifetimeTokenFactory): KtAnalysisSession {
        val key = Pair(useSiteKtModule, factory.identifier)
        return cache.computeIfAbsent(key) {
            CachedValuesManager.getManager(project).createCachedValue {
                val firResolveSession = useSiteKtModule.getFirResolveSession(project)
                val validityToken = factory.create(project)

                CachedValueProvider.Result(
                    KtFirAnalysisSession.createAnalysisSessionByFirResolveSession(firResolveSession, validityToken),
                    firResolveSession.useSiteFirSession.modificationTracker,
                    ProjectRootModificationTracker.getInstance(project),
                    project.createProjectWideOutOfBlockModificationTracker()
                )
            }
        }.value
    }

    override fun clearCaches() {
        for (cachedValue in cache.values) {
            check(cachedValue is CachedValueBase<*>) {
                "Unsupported 'CachedValue' of type ${cachedValue.javaClass}'"
            }

            cachedValue.clear()
        }
    }
}