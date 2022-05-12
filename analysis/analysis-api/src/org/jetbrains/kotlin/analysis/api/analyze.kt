/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtAnalysisApiInternals::class)

package org.jetbrains.kotlin.analysis.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.Computable
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * Execute given [action] in [KtAnalysisSession] context
 * Uses [useSiteKtElement] as an [KtElement] which containing module is a use-site module,
 * i.e, the module from which perspective the project will be analyzed.
 *
 *  [nonDefaultLifetimeTokenFactory] represents lifetime and accessibility guaranties
 *  which will be applied to the [org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner] instances created during code analysis.
 *
 * @see KtAnalysisSession
 * @see KtLifetimeTokenFactory
 * @see analyzeWithReadAction
 */
public inline fun <R> analyze(
    useSiteKtElement: KtElement,
    nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory? = null,
    action: KtAnalysisSession.() -> R
): R =
    KtAnalysisSessionProvider.getInstance(useSiteKtElement.project)
        .analyse(useSiteKtElement, nonDefaultLifetimeTokenFactory, action)


/**
 * Execute given [action] in [KtAnalysisSession] context
 * Uses [useSiteKtModule] as use-site module, i.e, the module from which perspective the project will be analyzed.
 *
 *  [nonDefaultLifetimeTokenFactory] represents lifetime and accessibility guaranties
 *  which will be applied to the [org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner] instances created during code analysis.
 *
 * @see KtAnalysisSession
 * @see KtLifetimeTokenFactory
 */
public inline fun <R> analyze(
    useSiteKtModule: KtModule,
    nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory? = null,
    crossinline action: KtAnalysisSession.() -> R
): R {
    checkNotNull(useSiteKtModule.project)
    val sessionProvider = KtAnalysisSessionProvider.getInstance(useSiteKtModule.project!!)
    return sessionProvider.analyze(useSiteKtModule, nonDefaultLifetimeTokenFactory, action)
}


public inline fun <R> analyzeInDependedAnalysisSession(
    originalFile: KtFile,
    elementToReanalyze: KtElement,
    nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory? = null,
    action: KtAnalysisSession.() -> R
): R =
    KtAnalysisSessionProvider.getInstance(originalFile.project)
        .analyseInDependedAnalysisSession(originalFile, elementToReanalyze, nonDefaultLifetimeTokenFactory, action)

/**
 * Execute given [action] in [KtAnalysisSession] context like [analyze] does but execute it in read action
 * Uses [contextElement] to get a module from which you would like to see the other modules
 * Usually [contextElement] is some element form the module you currently analysing now
 *
 * Should be called from read action
 * To analyse something from EDT thread, consider using [analyzeInModalWindow]
 * If you are already in read action, consider using [analyze]
 *
 * @see KtAnalysisSession
 * @see analyze
 */
public inline fun <R> analyzeWithReadAction(
    contextElement: KtElement,
    nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory? = null,
    crossinline action: KtAnalysisSession.() -> R
): R = ApplicationManager.getApplication().runReadAction(Computable {
    analyze(contextElement, nonDefaultLifetimeTokenFactory, action)
})

/**
 * Show a modal window with a progress bar and specified [windowTitle]
 * and execute given [action] task with [KtAnalysisSession] context
 * If [action] throws some exception, then [analyzeInModalWindow] will rethrow it
 * Should be executed from EDT only
 * If you want to analyse something from non-EDT thread, consider using [analyze]/[analyzeWithReadAction]
 */
public inline fun <R> analyzeInModalWindow(
    contextElement: KtElement,
    windowTitle: String,
    nonDefaultLifetimeTokenFactory: KtLifetimeTokenFactory? = null,
    crossinline action: KtAnalysisSession.() -> R
): R {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val task = object : Task.WithResult<R, Exception>(contextElement.project, windowTitle, /*canBeCancelled*/ true) {
        override fun compute(indicator: ProgressIndicator): R =
            analyzeWithReadAction(contextElement, nonDefaultLifetimeTokenFactory) { action() }
    }
    task.queue()
    return task.result
}
