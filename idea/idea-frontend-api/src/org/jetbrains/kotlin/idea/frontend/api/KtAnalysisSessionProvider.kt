/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import org.jetbrains.kotlin.idea.frontend.api.tokens.ReadActionConfinementValidityTokenFactory
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityTokenFactory
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

@RequiresOptIn("To use analysis session, consider using analyze/analyzeWithReadAction/analyseInModalWindow methods")
annotation class InvalidWayOfUsingAnalysisSession

@RequiresOptIn
annotation class KtAnalysisSessionProviderInternals

/**
 * Provides [KtAnalysisSession] by [contextElement]
 * Should not be used directly, consider using [analyse]/[analyseWithReadAction]/[analyseInModalWindow] instead
 */
@InvalidWayOfUsingAnalysisSession
abstract class KtAnalysisSessionProvider : Disposable {
    @Suppress("LeakingThis")
    @OptIn(KtInternalApiMarker::class)
    val noWriteActionInAnalyseCallChecker = NoWriteActionInAnalyseCallChecker(this)

    @InvalidWayOfUsingAnalysisSession
    abstract fun getAnalysisSession(contextElement: KtElement, factory: ValidityTokenFactory): KtAnalysisSession

    @InvalidWayOfUsingAnalysisSession
    inline fun <R> analyseInFakeAnalysisSession(originalFile: KtFile, fakeExpresion: KtElement, action: KtAnalysisSession.() -> R): R {
        val fakeAnalysisSession = getAnalysisSession(originalFile, ReadActionConfinementValidityTokenFactory)
            .createContextDependentCopy(originalFile, fakeExpresion)
        return analyse(fakeAnalysisSession, ReadActionConfinementValidityTokenFactory, action)
    }

    @InvalidWayOfUsingAnalysisSession
    inline fun <R> analyse(contextElement: KtElement, tokenFactory: ValidityTokenFactory, action: KtAnalysisSession.() -> R): R =
        analyse(getAnalysisSession(contextElement, tokenFactory), tokenFactory, action)

    @OptIn(KtAnalysisSessionProviderInternals::class, KtInternalApiMarker::class)
    @InvalidWayOfUsingAnalysisSession
    inline fun <R> analyse(analysisSession: KtAnalysisSession, factory: ValidityTokenFactory, action: KtAnalysisSession.() -> R): R {
        noWriteActionInAnalyseCallChecker.beforeEnteringAnalysisContext()
        factory.beforeEnteringAnalysisContext()
        return try {
            analysisSession.action()
        } finally {
            factory.afterLeavingAnalysisContext()
            noWriteActionInAnalyseCallChecker.afterLeavingAnalysisContext()
        }
    }

    override fun dispose() {}
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
inline fun <R> analyse(contextElement: KtElement, action: KtAnalysisSession.() -> R): R =
    contextElement.project
        .service<KtAnalysisSessionProvider>()
        .analyse(contextElement, ReadActionConfinementValidityTokenFactory, action)

@OptIn(InvalidWayOfUsingAnalysisSession::class)
inline fun <R> analyseWithCustomToken(
    contextElement: KtElement,
    tokenFactory: ValidityTokenFactory,
    action: KtAnalysisSession.() -> R
): R =
    contextElement.project.service<KtAnalysisSessionProvider>().analyse(contextElement, tokenFactory, action)

@OptIn(InvalidWayOfUsingAnalysisSession::class)
inline fun <R> analyseInFakeAnalysisSession(originalFile: KtFile, fakeExpresion: KtElement, action: KtAnalysisSession.() -> R): R =
    originalFile.project.service<KtAnalysisSessionProvider>().analyseInFakeAnalysisSession(originalFile, fakeExpresion, action)

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
inline fun <R> analyseWithReadAction(
    contextElement: KtElement,
    crossinline action: KtAnalysisSession.() -> R
): R = runReadAction {
    analyse(contextElement, action)
}

/**
 * Show a modal window with a progress bar and specified [windowTitle]
 * and execute given [action] task with [KtAnalysisSession] context
 * If [action] throws some exception, then [analyseInModalWindow] will rethrow it
 * Should be executed from EDT only
 * If you want to analyse something from non-EDT thread, consider using [analyse]/[analyseWithReadAction]
 */
inline fun <R> analyseInModalWindow(
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