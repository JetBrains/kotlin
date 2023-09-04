/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils.offsetToLineAndColumn
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives

inline fun <T> runReadAction(crossinline runnable: () -> T): T {
    return ApplicationManager.getApplication().runReadAction(Computable { runnable() })
}

fun <R> executeOnPooledThreadInReadAction(action: () -> R): R =
    ApplicationManager.getApplication().executeOnPooledThread<R> { runReadAction(action) }.get()

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

fun String.indented(indent: Int): String {
    val indentString = " ".repeat(indent)
    return indentString + replace("\n", "\n$indentString")
}

fun KtDeclaration.getNameWithPositionString(): String {
    return (presentation?.presentableText ?: name ?: this::class.simpleName) + "@" + position()
}

fun findReferencesAtCaret(mainKtFile: KtFile, caretPosition: Int): List<KtReference> =
    mainKtFile.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()

fun PsiReference.unwrapMultiReferences(): List<PsiReference> = when (this) {
    is KtReference -> listOf(this)
    is PsiMultiReference -> references.flatMap { it.unwrapMultiReferences() }
    else -> error("Unexpected reference $this")
}

/**
 * [AfterAnalysisChecker][org.jetbrains.kotlin.test.model.AfterAnalysisChecker] should be the preferred option
 */
fun RegisteredDirectives.ignoreExceptionIfIgnoreDirectivePresent(ignoreDirective: Directive, action: () -> Unit) {
    var exception: Throwable? = null
    try {
        action()
    } catch (e: Throwable) {
        exception = e
    }

    if (ignoreDirective in this) {
        if (exception != null) return
        error("$ignoreDirective is redundant")
    }

    if (exception != null) {
        throw exception
    }
}
