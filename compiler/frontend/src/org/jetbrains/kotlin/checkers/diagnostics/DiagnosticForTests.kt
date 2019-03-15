/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
class SyntaxErrorDiagnostic(errorElement: PsiErrorElement) : AbstractDiagnosticForTests(errorElement,
                                                                                                                                  SyntaxErrorDiagnosticFactory.INSTANCE
)

open class AbstractDiagnosticForTests(private val element: PsiElement, private val factory: DiagnosticFactory<*>) : Diagnostic {
    override fun getFactory(): DiagnosticFactory<*> {
        return factory
    }

    override fun getSeverity(): Severity {
        return Severity.ERROR
    }

    override fun getPsiElement(): PsiElement {
        return element
    }

    override fun getTextRanges(): List<TextRange> {
        return listOf(element.textRange)
    }

    override fun getPsiFile(): PsiFile {
        return element.containingFile
    }

    override fun isValid(): Boolean {
        return true
    }
}