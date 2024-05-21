/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.lifetime.KaBaseLifetimeTracker
import org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseWriteActionStartedChecker
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.session.KaSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.KaCachedService
import org.jetbrains.kotlin.analysis.providers.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.analysis.providers.permissions.KaAnalysisPermissionChecker

abstract class KaBaseSessionProvider(project: Project) : KaSessionProvider(project) {
    /**
     * Caches [KaAnalysisPermissionChecker] to avoid repeated [Project.getService] calls in [analyze].
     */
    @KaCachedService
    private val permissionChecker by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaAnalysisPermissionChecker.getInstance(project)
    }

    /**
     * Caches [KaBaseLifetimeTracker] to avoid repeated [Project.getService] calls in [analyze].
     */
    @KaCachedService
    private val lifetimeTracker by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaBaseLifetimeTracker.getInstance(project)
    }

    private val writeActionStartedChecker = KaBaseWriteActionStartedChecker(this)

    override val tokenFactory: KtLifetimeTokenFactory by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KtLifetimeTokenProvider.getService(project).getLifetimeTokenFactory()
    }

    override fun beforeEnteringAnalysis(session: KtAnalysisSession, useSiteElement: KtElement) {
        // Catch issues with analysis on invalid PSI as early as possible.
        PsiUtilCore.ensureValid(useSiteElement)

        beforeEnteringAnalysis(session)
    }

    override fun beforeEnteringAnalysis(session: KtAnalysisSession, useSiteModule: KtModule) {
        beforeEnteringAnalysis(session)
    }

    private fun beforeEnteringAnalysis(session: KtAnalysisSession) {
        if (!permissionChecker.isAnalysisAllowed()) {
            throw ProhibitedAnalysisException("Analysis is not allowed: ${permissionChecker.getRejectionReason()}")
        }

        lifetimeTracker.beforeEnteringAnalysis(session)
        writeActionStartedChecker.beforeEnteringAnalysis()
    }

    override fun afterLeavingAnalysis(session: KtAnalysisSession, useSiteElement: KtElement) {
        afterLeavingAnalysis(session)
    }

    override fun afterLeavingAnalysis(session: KtAnalysisSession, useSiteModule: KtModule) {
        afterLeavingAnalysis(session)
    }

    private fun afterLeavingAnalysis(session: KtAnalysisSession) {
        writeActionStartedChecker.afterLeavingAnalysis()
        lifetimeTracker.afterLeavingAnalysis(session)
    }
}

private class ProhibitedAnalysisException(override val message: String) : IllegalStateException()
