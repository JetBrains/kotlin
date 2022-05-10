/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt

@OptIn(KtAllowAnalysisOnEdt::class)
internal inline fun <E> allowLightClassesOnEdt(action: () -> E): E = allowAnalysisOnEdt(action)

internal inline fun <R> PsiElement.analyzeWithSymbolAsContext(
    contextSymbol: KtSymbol,
    action: KtAnalysisSession.() -> R
): R {
    return project.analyzeWithSymbolAsContext(contextSymbol, action)
}

@OptIn(KtAnalysisApiInternals::class)
internal inline fun <R> Project.analyzeWithSymbolAsContext(
    contextSymbol: KtSymbol,
    action: KtAnalysisSession.() -> R
): R {
    return KtAnalysisSessionProvider.getInstance(this).analyzeWithSymbolAsContext(contextSymbol, action)
}

internal fun PsiElement.nonExistentType(): PsiType =
    JavaPsiFacade.getElementFactory(project).createTypeFromText("error.NonExistentClass", this)
