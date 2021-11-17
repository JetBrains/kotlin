/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils.offsetToLineAndColumn
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration

inline fun <T> runReadAction(crossinline runnable: () -> T): T {
    return ApplicationManager.getApplication().runReadAction(Computable { runnable() })
}

fun <R> executeOnPooledThreadInReadAction(action: () -> R): R =
    ApplicationManager.getApplication().executeOnPooledThread<R> { runReadAction(action) }.get()

inline fun <R> analyseOnPooledThreadInReadAction(context: KtElement, crossinline action: KtAnalysisSession.() -> R): R =
    executeOnPooledThreadInReadAction {
        analyse(context) { action() }
    }

fun PsiElement?.position(): String {
    if (this == null) return "(unknown)"
    return offsetToLineAndColumn(containingFile.viewProvider.document, textRange.startOffset).toString()
}

fun KtSymbol.getNameWithPositionString(): String {
    return when (val psi = this.psi) {
        is KtDeclarationWithBody -> psi.name
        is KtNamedDeclaration -> psi.name
        null -> "null"
        else -> psi::class.simpleName
    } + "@" + psi.position()
}