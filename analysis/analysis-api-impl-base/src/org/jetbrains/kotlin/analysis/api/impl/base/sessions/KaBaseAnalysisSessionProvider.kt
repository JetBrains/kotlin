/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.lifetime.KaBaseLifetimeTracker
import org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseWriteActionStartedChecker
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.psi.KtElement

abstract class KaBaseAnalysisSessionProvider(project: Project) : KtAnalysisSessionProvider(project) {
    private val lifetimeTracker = KaBaseLifetimeTracker.getInstance(project)

    private val writeActionStartedChecker = KaBaseWriteActionStartedChecker(this)

    override val tokenFactory: KtLifetimeTokenFactory by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KtLifetimeTokenProvider.getService(project).getLifetimeTokenFactory()
    }

    override fun beforeEnteringAnalysis(session: KtAnalysisSession, useSiteElement: KtElement) {
        beforeEnteringAnalysis(session)
    }

    override fun beforeEnteringAnalysis(session: KtAnalysisSession, useSiteModule: KtModule) {
        beforeEnteringAnalysis(session)
    }

    private fun beforeEnteringAnalysis(session: KtAnalysisSession) {
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
