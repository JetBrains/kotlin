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
import org.jetbrains.kotlin.analysis.api.session.KtDefaultLifetimeTokenProvider
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
 * Should not be used directly, consider using [analyse]/[analyseWithReadAction]/[analyseInModalWindow] instead
 */
@InvalidWayOfUsingAnalysisSession
public abstract class KtAnalysisSessionProvider(public val project: Project) : Disposable {

    @Suppress("LeakingThis")
    @OptIn(KtInternalApiMarker::class)
    public val noWriteActionInAnalyseCallChecker: NoWriteActionInAnalyseCallChecker = NoWriteActionInAnalyseCallChecker(this)

    @InvalidWayOfUsingAnalysisSession
    public abstract fun getAnalysisSession(contextElement: KtElement, factory: KtLifetimeTokenFactory): KtAnalysisSession

    @InvalidWayOfUsingAnalysisSession
    public abstract fun getAnalysisSessionBySymbol(contextSymbol: KtSymbol): KtAnalysisSession

    @InvalidWayOfUsingAnalysisSession
    public abstract fun getAnalysisSessionByUseSiteKtModule(ktModule: KtModule, factory: KtLifetimeTokenFactory): KtAnalysisSession

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
        contextElement: KtElement,
        nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory?,
        action: KtAnalysisSession.() -> R
    ): R {
        val factory =
            nonDefaultLifetimeTokenFactory ?: KtDefaultLifetimeTokenProvider.getService(project).getDefaultLifetimeTokenFactory()
        return analyse(getAnalysisSession(contextElement, factory), factory, action)
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
public inline fun <R> analyse(
    contextElement: KtElement,
    nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory? = null,
    action: KtAnalysisSession.() -> R
): R =
    KtAnalysisSessionProvider.getInstance(contextElement.project)
        .analyse(contextElement, nonDefaultLifetimeTokenFactory, action)


@OptIn(InvalidWayOfUsingAnalysisSession::class)
public inline fun <R> analyze(
    ktModule: KtModule,
    nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory? = null,
    crossinline action: KtAnalysisSession.() -> R
): R {
    checkNotNull(ktModule.project)
    val sessionProvider = KtAnalysisSessionProvider.getInstance(ktModule.project!!)
    return sessionProvider.analyze(ktModule, nonDefaultLifetimeTokenFactory, action)
}

@OptIn(InvalidWayOfUsingAnalysisSession::class)
public inline fun <R> analyseInDependedAnalysisSession(
    originalFile: KtFile,
    elementToReanalyze: KtElement,
    nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory? = null,
    action: KtAnalysisSession.() -> R
): R =
    KtAnalysisSessionProvider.getInstance(originalFile.project)
        .analyseInDependedAnalysisSession(originalFile, elementToReanalyze, nonDefaultLifetimeTokenFactory, action)

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
    nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory? = null,
    crossinline action: KtAnalysisSession.() -> R
): R = ApplicationManager.getApplication().runReadAction(Computable {
    analyse(contextElement, nonDefaultLifetimeTokenFactory, action)
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
    nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory? = null,
    crossinline action: KtAnalysisSession.() -> R
): R {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val task = object : Task.WithResult<R, Exception>(contextElement.project, windowTitle, /*canBeCancelled*/ true) {
        override fun compute(indicator: ProgressIndicator): R =
            analyseWithReadAction(contextElement, nonDefaultLifetimeTokenFactory) { action() }
    }
    task.queue()
    return task.result
}
