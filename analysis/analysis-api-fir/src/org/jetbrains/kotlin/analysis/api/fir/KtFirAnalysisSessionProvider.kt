/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.LowMemoryWatcher
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.KtReadActionConfinementLifetimeToken
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidationListener
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.isStable
import org.jetbrains.kotlin.psi.KtElement
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.KClass

/**
 * [KtFirAnalysisSessionProvider] keeps [KtFirAnalysisSession]s in a cache, which are actively invalidated with their associated underlying
 * [LLFirSession][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession]s.
 */
internal class KtFirAnalysisSessionProvider(project: Project) : KtAnalysisSessionProvider(project) {
    // `KtFirAnalysisSession`s must be soft-referenced to allow simultaneous garbage collection of an unused `KtFirAnalysisSession` together
    // with its `LLFirSession`.
    private val cache: ConcurrentMap<KtModule, KtAnalysisSession> = ContainerUtil.createConcurrentSoftValueMap()

    init {
        LowMemoryWatcher.register(::clearCaches, project)
    }

    override fun getAnalysisSession(useSiteKtElement: KtElement): KtAnalysisSession {
        val module = ProjectStructureProvider.getModule(project, useSiteKtElement, contextualModule = null)
        return getAnalysisSessionByUseSiteKtModule(module)
    }

    override fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule): KtAnalysisSession {
        if (useSiteKtModule is KtDanglingFileModule && !useSiteKtModule.isStable) {
            return createAnalysisSession(useSiteKtModule)
        }

        val identifier = tokenFactory.identifier
        identifier.flushPendingChanges(project)

        return cache.computeIfAbsent(useSiteKtModule, ::createAnalysisSession)
    }

    private fun createAnalysisSession(useSiteKtModule: KtModule): KtFirAnalysisSession {
        val firResolveSession = useSiteKtModule.getFirResolveSession(project)
        val validityToken = tokenFactory.create(project, firResolveSession.useSiteFirSession.createValidityTracker())
        return KtFirAnalysisSession.createAnalysisSessionByFirResolveSession(firResolveSession, validityToken)
    }

    override fun clearCaches() {
        cache.clear()
    }

    /**
     * Note: Races cannot happen because the listener is guaranteed to be invoked in a write action.
     */
    internal class SessionInvalidationListener(val project: Project) : LLFirSessionInvalidationListener {
        private val analysisSessionProvider: KtFirAnalysisSessionProvider
            get() = getInstance(project) as? KtFirAnalysisSessionProvider
                ?: error("Expected the analysis session provider to be a `${KtFirAnalysisSessionProvider::class.simpleName}`.")

        override fun afterInvalidation(modules: Set<KtModule>) {
            modules.forEach { analysisSessionProvider.cache.remove(it) }
        }

        override fun afterGlobalInvalidation() {
            // Session invalidation events currently don't report whether library modules were included in the global invalidation. This is
            // by design to avoid iterating through the whole analysis session cache and to simplify the global session invalidation event.
            // Nevertheless, a `KtFirAnalysisSession`'s validity is based on the underlying `LLFirSession`, so removed analysis sessions for
            // library modules might still be valid. This is not a problem, though, because analysis session caching is not required for
            // correctness, but rather a performance optimization.
            analysisSessionProvider.clearCaches()
        }
    }
}

private fun KClass<out KtLifetimeToken>.flushPendingChanges(project: Project) {
    if (this == KtReadActionConfinementLifetimeToken::class &&
        KtReadActionConfinementLifetimeToken.allowFromWriteAction.get() &&
        ApplicationManager.getApplication().isWriteAccessAllowed
    ) {
        // We must flush modifications to publish local modifications into FIR tree
        @OptIn(LLFirInternals::class)
        LLFirDeclarationModificationService.getInstance(project).flushModifications()
    }
}
