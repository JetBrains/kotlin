/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.openapi.components.service
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtElement

abstract class KtAnalysisSessionProvider {
    abstract fun getAnalysisSessionFor(contextElement: KtElement): KtAnalysisSession
}

fun getAnalysisSessionFor(contextElement: KtElement): KtAnalysisSession =
    contextElement.project.service<KtAnalysisSessionProvider>().getAnalysisSessionFor(contextElement)

inline fun <R> analyze(contextElement: KtElement, action: KtAnalysisSession.() -> R): R =
    getAnalysisSessionFor(contextElement).action()

inline fun <R> analyzeWithReadAction(
    contextElement: KtElement,
    crossinline action: KtAnalysisSession.() -> R
): R = runReadAction {
    analyze(contextElement, action)
}