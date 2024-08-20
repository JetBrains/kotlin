/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.sessions

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.lifetime.KaBaseLifetimeTracker
import org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseWriteActionStartedChecker
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.permissions.KaAnalysisPermissionChecker
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.session.KaSessionProvider
import org.jetbrains.kotlin.psi.KtElement

@KaImplementationDetail
abstract class KaBaseSessionProvider(project: Project) : KaSessionProvider(project) {
    // We cache several services to avoid repeated `getService` calls in `analyze`.
    @KaCachedService
    private val permissionChecker by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaAnalysisPermissionChecker.getInstance(project)
    }

    @KaCachedService
    private val lifetimeTracker by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaBaseLifetimeTracker.getInstance(project)
    }

    @KaCachedService
    protected val tokenFactory by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinLifetimeTokenFactory.getInstance(project)
    }

    private val writeActionStartedChecker = KaBaseWriteActionStartedChecker(this)

    override fun beforeEnteringAnalysis(session: KaSession, useSiteElement: KtElement) {
        // Catch issues with analysis on invalid PSI as early as possible.
        PsiUtilCore.ensureValid(useSiteElement)

        beforeEnteringAnalysis(session)
    }

    override fun beforeEnteringAnalysis(session: KaSession, useSiteModule: KaModule) {
        beforeEnteringAnalysis(session)
    }

    private fun beforeEnteringAnalysis(session: KaSession) {
        if (!permissionChecker.isAnalysisAllowed()) {
            throw ProhibitedAnalysisException("Analysis is not allowed: ${permissionChecker.getRejectionReason()}")
        }

        /**
         * The Analysis API is not supposed to work in the dumb mode.
         * See [KaSession] KDoc for more details.
         */
        if (DumbService.isDumb(project)) {
            throw IndexNotReadyException.create()
        }

        lifetimeTracker.beforeEnteringAnalysis(session)
        writeActionStartedChecker.beforeEnteringAnalysis()
    }

    override fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement) {
        afterLeavingAnalysis(session)
    }

    override fun afterLeavingAnalysis(session: KaSession, useSiteModule: KaModule) {
        afterLeavingAnalysis(session)
    }

    private fun afterLeavingAnalysis(session: KaSession) {
        writeActionStartedChecker.afterLeavingAnalysis()
        lifetimeTracker.afterLeavingAnalysis(session)
    }
}

private class ProhibitedAnalysisException(override val message: String) : IllegalStateException()
