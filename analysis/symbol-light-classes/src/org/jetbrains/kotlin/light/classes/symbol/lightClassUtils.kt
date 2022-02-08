/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.analysis.api.tokens.hackyAllowRunningOnEdt

@OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
internal inline fun <E> allowLightClassesOnEdt(action: () -> E): E = hackyAllowRunningOnEdt(action)

@OptIn(InvalidWayOfUsingAnalysisSession::class)
internal inline fun <R> PsiElement.analyzeWithSymbolAsContext(
    contextSymbol: KtSymbol,
    action: KtAnalysisSession.() -> R
): R {
    return project.analyzeWithSymbolAsContext(contextSymbol, action)
}

@OptIn(InvalidWayOfUsingAnalysisSession::class)
internal inline fun <R> Project.analyzeWithSymbolAsContext(
    contextSymbol: KtSymbol,
    action: KtAnalysisSession.() -> R
): R {
    return KtAnalysisSessionProvider.getInstance(this).analyzeWithSymbolAsContext(contextSymbol, action)
}

internal fun PsiElement.nonExistentType(): PsiType =
    JavaPsiFacade.getElementFactory(project).createTypeFromText("error.NonExistentClass", this)
