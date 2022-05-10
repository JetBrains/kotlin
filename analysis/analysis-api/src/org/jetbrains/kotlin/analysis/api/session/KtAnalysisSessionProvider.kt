/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtInternalApiMarker
import org.jetbrains.kotlin.analysis.api.NoWriteActionInAnalyseCallChecker
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.tokens.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

@RequiresOptIn("To use analysis session, consider using analyze/analyzeWithReadAction/analyseInModalWindow methods")
public annotation class InvalidWayOfUsingAnalysisSession

@RequiresOptIn
public annotation class KtAnalysisSessionProviderInternals

/**
 * Provides [KtAnalysisSession] by [contextElement]
 * Should not be used directly, consider using [analyse]/[analyzeWithReadAction]/[analyzeInModalWindow] instead
 */
@InvalidWayOfUsingAnalysisSession
public abstract class KtAnalysisSessionProvider(public val project: Project) : Disposable {

    @Suppress("LeakingThis")
    @OptIn(KtInternalApiMarker::class)
    public val noWriteActionInAnalyseCallChecker: NoWriteActionInAnalyseCallChecker = NoWriteActionInAnalyseCallChecker(this)

    @InvalidWayOfUsingAnalysisSession
    public abstract fun getAnalysisSession(useSiteKtElement: KtElement, factory: KtLifetimeTokenFactory): KtAnalysisSession

    @InvalidWayOfUsingAnalysisSession
    public abstract fun getAnalysisSessionBySymbol(contextSymbol: KtSymbol): KtAnalysisSession

    @InvalidWayOfUsingAnalysisSession
    public abstract fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule, factory: KtLifetimeTokenFactory): KtAnalysisSession

    @InvalidWayOfUsingAnalysisSession
    public inline fun <R> analyzeWithSymbolAsContext(
        contextSymbol: KtSymbol,
        action: KtAnalysisSession.() -> R
    ): R {
        val analysisSession = getAnalysisSessionBySymbol(contextSymbol)
        return action(analysisSession)
    }

    @InvalidWayOfUsingAnalysisSession
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

    @InvalidWayOfUsingAnalysisSession
    public inline fun <R> analyse(
        useSiteKtElement: KtElement,
        nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory?,
        action: KtAnalysisSession.() -> R
    ): R {
        val factory =
            nonDefaultLifetimeTokenFactory ?: KtDefaultLifetimeTokenProvider.getService(project).getDefaultLifetimeTokenFactory()
        return analyse(getAnalysisSession(useSiteKtElement, factory), factory, action)
    }

    @InvalidWayOfUsingAnalysisSession
    public inline fun <R> analyze(
        useSiteKtModule: KtModule,
        nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory?,
        action: KtAnalysisSession.() -> R
    ): R {
        val factory =
            nonDefaultLifetimeTokenFactory ?: KtDefaultLifetimeTokenProvider.getService(project).getDefaultLifetimeTokenFactory()

        return analyse(getAnalysisSessionByUseSiteKtModule(useSiteKtModule, factory), factory, action)
    }

    @OptIn(KtAnalysisSessionProviderInternals::class, KtInternalApiMarker::class)
    @InvalidWayOfUsingAnalysisSession
    public inline fun <R> analyse(
        analysisSession: KtAnalysisSession,
        factory: KtLifetimeTokenFactory,
        action: KtAnalysisSession.() -> R
    ): R {
        noWriteActionInAnalyseCallChecker.beforeEnteringAnalysisContext()
        factory.beforeEnteringAnalysisContext()
        return try {
            analysisSession.action()
        } finally {
            factory.afterLeavingAnalysisContext()
            noWriteActionInAnalyseCallChecker.afterLeavingAnalysisContext()
        }
    }

    @TestOnly
    public abstract fun clearCaches()

    override fun dispose() {}

    public companion object {
        public fun getInstance(project: Project): KtAnalysisSessionProvider =
            ServiceManager.getService(project, KtAnalysisSessionProvider::class.java)
    }
}
