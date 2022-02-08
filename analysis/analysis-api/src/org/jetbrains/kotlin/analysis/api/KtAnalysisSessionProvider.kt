/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.tokens.AlwaysAccessibleValidityTokenFactory
import org.jetbrains.kotlin.analysis.api.tokens.ReadActionConfinementValidityTokenFactory
import org.jetbrains.kotlin.analysis.api.tokens.ValidityTokenFactory
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

@RequiresOptIn("To use analysis session, consider using analyze/analyzeWithReadAction/analyseInModalWindow methods")
public annotation class InvalidWayOfUsingAnalysisSession

@RequiresOptIn
public annotation class KtAnalysisSessionProviderInternals

/**
 * Provides [KtAnalysisSession] by [contextElement]
 * Should not be used directly, consider using [analyse]/[analyseWithReadAction]/[analyseInModalWindow] instead
 */
@InvalidWayOfUsingAnalysisSession
public abstract class KtAnalysisSessionProvider : Disposable {
    @Suppress("LeakingThis")
    @OptIn(KtInternalApiMarker::class)
    public val noWriteActionInAnalyseCallChecker: NoWriteActionInAnalyseCallChecker = NoWriteActionInAnalyseCallChecker(this)

    @InvalidWayOfUsingAnalysisSession
    public abstract fun getAnalysisSession(contextElement: KtElement, factory: ValidityTokenFactory): KtAnalysisSession

    @InvalidWayOfUsingAnalysisSession
    public abstract fun getAnalysisSessionBySymbol(contextSymbol: KtSymbol): KtAnalysisSession

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
        action: KtAnalysisSession.() -> R
    ): R {
        val dependedAnalysisSession = getAnalysisSession(originalFile, ReadActionConfinementValidityTokenFactory)
            .createContextDependentCopy(originalFile, elementToReanalyze)
        return analyse(dependedAnalysisSession, ReadActionConfinementValidityTokenFactory, action)
    }

    @InvalidWayOfUsingAnalysisSession
    public inline fun <R> analyse(contextElement: KtElement, tokenFactory: ValidityTokenFactory, action: KtAnalysisSession.() -> R): R =
        analyse(getAnalysisSession(contextElement, tokenFactory), tokenFactory, action)

    @OptIn(KtAnalysisSessionProviderInternals::class, KtInternalApiMarker::class)
    @InvalidWayOfUsingAnalysisSession
    public inline fun <R> analyse(analysisSession: KtAnalysisSession, factory: ValidityTokenFactory, action: KtAnalysisSession.() -> R): R {
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

/**
 * Execute given [action] in [KtAnalysisSession] context
 * Uses [contextElement] to get a module from which you would like to see the other modules
 * Usually [contextElement] is some element form the module you currently analysing now
 *
 * Should not be called from EDT thread
 * Should be called from read action
 * To analyse something from EDT thread, consider using [analyseInModalWindow]
 *
 * @see KtAnalysisSession
 * @see analyseWithReadAction
 */
@OptIn(InvalidWayOfUsingAnalysisSession::class)
public inline fun <R> analyse(contextElement: KtElement, action: KtAnalysisSession.() -> R): R =
    KtAnalysisSessionProvider.getInstance(contextElement.project)
        .analyse(contextElement, ReadActionConfinementValidityTokenFactory, action)

@OptIn(InvalidWayOfUsingAnalysisSession::class)
public inline fun <R> analyseWithCustomToken(
    contextElement: KtElement,
    tokenFactory: ValidityTokenFactory,
    action: KtAnalysisSession.() -> R
): R =
    KtAnalysisSessionProvider.getInstance(contextElement.project)
        .analyse(contextElement, tokenFactory, action)

/**
 * UAST-specific version of [analyse] that executes the given [action] in [KtAnalysisSession] context
 */
@OptIn(InvalidWayOfUsingAnalysisSession::class)
public inline fun <R> analyseForUast(
    contextElement: KtElement,
    action: KtAnalysisSession.() -> R
): R =
    analyseWithCustomToken(contextElement, AlwaysAccessibleValidityTokenFactory, action)

@OptIn(InvalidWayOfUsingAnalysisSession::class)
public inline fun <R> analyseInDependedAnalysisSession(
    originalFile: KtFile,
    elementToReanalyze: KtElement,
    action: KtAnalysisSession.() -> R
): R =
    KtAnalysisSessionProvider.getInstance(originalFile.project)
        .analyseInDependedAnalysisSession(originalFile, elementToReanalyze, action)

/**
 * Execute given [action] in [KtAnalysisSession] context like [analyse] does but execute it in read action
 * Uses [contextElement] to get a module from which you would like to see the other modules
 * Usually [contextElement] is some element form the module you currently analysing now
 *
 * Should be called from read action
 * To analyse something from EDT thread, consider using [analyseInModalWindow]
 * If you are already in read action, consider using [analyse]
 *
 * @see KtAnalysisSession
 * @see analyse
 */
public inline fun <R> analyseWithReadAction(
    contextElement: KtElement,
    crossinline action: KtAnalysisSession.() -> R
): R = ApplicationManager.getApplication().runReadAction(Computable {
    analyse(contextElement, action)
})

/**
 * Show a modal window with a progress bar and specified [windowTitle]
 * and execute given [action] task with [KtAnalysisSession] context
 * If [action] throws some exception, then [analyseInModalWindow] will rethrow it
 * Should be executed from EDT only
 * If you want to analyse something from non-EDT thread, consider using [analyse]/[analyseWithReadAction]
 */
public inline fun <R> analyseInModalWindow(
    contextElement: KtElement,
    windowTitle: String,
    crossinline action: KtAnalysisSession.() -> R
): R {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val task = object : Task.WithResult<R, Exception>(contextElement.project, windowTitle, /*canBeCancelled*/ true) {
        override fun compute(indicator: ProgressIndicator): R = analyseWithReadAction(contextElement) { action() }
    }
    task.queue()
    return task.result
}
