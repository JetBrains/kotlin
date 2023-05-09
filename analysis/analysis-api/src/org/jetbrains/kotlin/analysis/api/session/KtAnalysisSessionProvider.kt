/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.impl.NoWriteActionInAnalyseCallChecker
import org.jetbrains.kotlin.analysis.api.lifetime.KtDefaultLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * Provides [KtAnalysisSession] by [contextElement]
 * Should not be used directly, consider using [analyse]/[analyzeWithReadAction]/[analyzeInModalWindow] instead
 */
@KtAnalysisApiInternals
public abstract class KtAnalysisSessionProvider(public val project: Project) : Disposable {

    @Suppress("LeakingThis")
    public val noWriteActionInAnalyseCallChecker: NoWriteActionInAnalyseCallChecker = NoWriteActionInAnalyseCallChecker(this)

    public abstract fun getAnalysisSession(useSiteKtElement: KtElement, factory: KtLifetimeTokenFactory): KtAnalysisSession

    public abstract fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule, factory: KtLifetimeTokenFactory): KtAnalysisSession

    public inline fun <R> analyseInDependedAnalysisSession(
        originalFile: KtFile,
        elementToReanalyze: KtElement,
        nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory?,
        action: KtAnalysisSession.() -> R
    ): R {
        val factory =
            nonDefaultLifetimeTokenFactory ?: KtDefaultLifetimeTokenProvider.getService(project).getDefaultLifetimeTokenFactory()

        val originalAnalysisSession = getAnalysisSession(originalFile, factory)
        val dependedAnalysisSession = originalAnalysisSession
            .createContextDependentCopy(originalFile, elementToReanalyze)
        return analyse(dependedAnalysisSession, factory, action)
    }

    public inline fun <R> analyse(
        useSiteKtElement: KtElement,
        nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory?,
        action: KtAnalysisSession.() -> R
    ): R {
        val factory =
            nonDefaultLifetimeTokenFactory ?: KtDefaultLifetimeTokenProvider.getService(project).getDefaultLifetimeTokenFactory()
        return analyse(getAnalysisSession(useSiteKtElement, factory), factory, action)
    }

    public inline fun <R> analyze(
        useSiteKtModule: KtModule,
        nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory?,
        action: KtAnalysisSession.() -> R
    ): R {
        val factory =
            nonDefaultLifetimeTokenFactory ?: KtDefaultLifetimeTokenProvider.getService(project).getDefaultLifetimeTokenFactory()

        return analyse(getAnalysisSessionByUseSiteKtModule(useSiteKtModule, factory), factory, action)
    }

    public inline fun <R> analyse(
        analysisSession: KtAnalysisSession,
        factory: KtLifetimeTokenFactory,
        action: KtAnalysisSession.() -> R
    ): R {
        noWriteActionInAnalyseCallChecker.beforeEnteringAnalysisContext()
        factory.beforeEnteringAnalysisContext(analysisSession.token)
        return try {
            analysisSession.action()
        } finally {
            factory.afterLeavingAnalysisContext(analysisSession.token)
            noWriteActionInAnalyseCallChecker.afterLeavingAnalysisContext()
        }
    }

    @TestOnly
    public abstract fun clearCaches()

    override fun dispose() {}

    public companion object {
        @KtAnalysisApiInternals
        public fun getInstance(project: Project): KtAnalysisSessionProvider =
            project.getService(KtAnalysisSessionProvider::class.java)
    }
}
