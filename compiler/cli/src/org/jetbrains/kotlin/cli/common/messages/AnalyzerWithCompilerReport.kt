/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common.messages

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.common.messages.SyntaxErrorReporter.SyntaxErrorReport
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.sortedDiagnostics
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages

@K1Deprecation
object AnalyzerWithCompilerReport {
    private class MyDiagnostic<E : PsiElement>(
        psiElement: E, factory: DiagnosticFactory0<E>, val message: String,
    ) : SimpleDiagnostic<E>(psiElement, factory, Severity.ERROR) {

        override val isValid: Boolean = true
    }

    private val SYNTAX_ERROR_FACTORY = DiagnosticFactory0.create<PsiErrorElement>(Severity.ERROR)

    private fun reportDiagnostic(diagnostic: Diagnostic, reporter: DiagnosticMessageReporter, renderDiagnosticName: Boolean): Boolean {
        if (!diagnostic.isValid) return false

        val message = (diagnostic as? MyDiagnostic<*>)?.message ?: DefaultErrorMessages.render(diagnostic)
        val diagnosticFactoryName = diagnostic.factoryNameOrNull()
        val textToRender = when (renderDiagnosticName) {
            true -> diagnosticFactoryName?.let { "[$it] $message" } ?: message
            false -> message
        }

        reporter.report(
            diagnostic,
            diagnostic.psiFile,
            textToRender
        )

        return diagnostic.severity == Severity.ERROR
    }

    fun reportDiagnostics(
        unsortedDiagnostics: GenericDiagnostics<*>,
        reporter: DiagnosticMessageReporter,
        renderDiagnosticName: Boolean,
    ): Boolean {
        var hasErrors = false
        val diagnostics = sortedDiagnostics(unsortedDiagnostics.all().filterIsInstance<Diagnostic>())
        for (diagnostic in diagnostics) {
            hasErrors = hasErrors or reportDiagnostic(diagnostic, reporter, renderDiagnosticName)
        }
        return hasErrors
    }

    // Reports K1 diagnostics ([org.jetbrains.kotlin.diagnostics.SimpleDiagnostic])
    fun reportSyntaxErrors(file: PsiElement, reporter: DiagnosticMessageReporter): SyntaxErrorReport {
        return SyntaxErrorReporter.reportSyntaxErrors(file) { element, message ->
            val diagnostic = MyDiagnostic(element, SYNTAX_ERROR_FACTORY, message)
            reportDiagnostic(diagnostic, reporter, renderDiagnosticName = false)
        }
    }

    fun reportSyntaxErrors(file: PsiElement, messageCollector: MessageCollector): SyntaxErrorReport {
        return reportSyntaxErrors(file, DefaultDiagnosticReporter(messageCollector))
    }
}
