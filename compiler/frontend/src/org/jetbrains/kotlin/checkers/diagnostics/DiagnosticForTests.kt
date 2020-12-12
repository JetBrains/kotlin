/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.checkers.diagnostics.factories.SyntaxErrorDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtElement

class DebugInfoDiagnostic(element: KtElement, factory: DiagnosticFactory<*>) : AbstractDiagnosticForTests(element, factory)
class SyntaxErrorDiagnostic(errorElement: PsiErrorElement) : AbstractDiagnosticForTests(
    errorElement,
    SyntaxErrorDiagnosticFactory.INSTANCE
)

open class AbstractDiagnosticForTests(override val psiElement: PsiElement, override val factory: DiagnosticFactory<*>) : Diagnostic {
    override val severity: Severity
        get() = Severity.ERROR

    override val textRanges: List<TextRange>
        get() = listOf(psiElement.textRange)

    override val psiFile: PsiFile
        get() = psiElement.containingFile

    override val isValid: Boolean
        get() = true
}