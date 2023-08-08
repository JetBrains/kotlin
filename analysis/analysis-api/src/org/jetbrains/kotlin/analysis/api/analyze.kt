/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtAnalysisApiInternals::class)

package org.jetbrains.kotlin.analysis.api

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * Executes the given [action] in a [KtAnalysisSession] context.
 *
 * The project will be analyzed from the perspective of [useSiteKtElement]'s module, also called the use-site module.
 *
 * @see KtAnalysisSession
 */
public inline fun <R> analyze(
    useSiteKtElement: KtElement,
    action: KtAnalysisSession.() -> R
): R =
    KtAnalysisSessionProvider.getInstance(useSiteKtElement.project)
        .analyse(useSiteKtElement, action)

/**
 * Executes the given [action] in a [KtAnalysisSession] context.
 *
 * The project will be analyzed from the perspective of the given [useSiteKtModule].
 *
 * @see KtAnalysisSession
 * @see KtLifetimeTokenFactory
 */
public inline fun <R> analyze(
    useSiteKtModule: KtModule,
    crossinline action: KtAnalysisSession.() -> R
): R {
    val sessionProvider = KtAnalysisSessionProvider.getInstance(useSiteKtModule.project)
    return sessionProvider.analyze(useSiteKtModule, action)
}


public inline fun <R> analyzeInDependedAnalysisSession(
    originalFile: KtFile,
    elementToReanalyze: KtElement,
    action: KtAnalysisSession.() -> R
): R =
    KtAnalysisSessionProvider.getInstance(originalFile.project)
        .analyseInDependedAnalysisSession(originalFile, elementToReanalyze, action)