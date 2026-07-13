/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.InternalDiagnosticFactoryMethod
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

object SyntaxErrorReporter {
    class SyntaxErrorReport(val isHasErrors: Boolean, val isAllErrorsAtEof: Boolean)

    fun reportSyntaxErrors(file: PsiElement, diagnosticCollector: BaseDiagnosticsCollector): SyntaxErrorReport {
        return reportSyntaxErrors(file) { element, message ->
            @OptIn(InternalDiagnosticFactoryMethod::class)
            val diagnostic = FirSyntaxErrors.SYNTAX.on(
                KtRealPsiSourceElement(element),
                message,
                positioningStrategy = null,
                DiagnosticContext.Default, // syntax errors couldn't be suppressed anyway
            )
            val context = object : DiagnosticContext {
                override val containingFile: KtSourceFile
                    get() = KtPsiSourceFile(file.containingFile)

                override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean {
                    return false
                }

                override val languageVersionSettings: LanguageVersionSettings
                    get() = LanguageVersionSettingsImpl.DEFAULT
            }
            diagnosticCollector.report(diagnostic, context)
        }
    }

    internal fun reportSyntaxErrors(
        file: PsiElement,
        createAndReportSyntaxError: (PsiErrorElement, message: String) -> Unit,
    ): SyntaxErrorReport {
        class ErrorReportingVisitor : KtTreeVisitorVoid() {
            var hasErrors = false
            var allErrorsAtEof = true

            private fun PsiElement.isAtEof(): Boolean {
                var element = this
                while (true) {
                    element = element.nextSibling ?: return true
                    if (element !is PsiWhiteSpace || element !is PsiComment) return false
                }
            }

            override fun visitErrorElement(element: PsiErrorElement) {
                val description = element.errorDescription
                if (allErrorsAtEof && !element.isAtEof()) {
                    allErrorsAtEof = false
                }
                hasErrors = true
                createAndReportSyntaxError(
                    element,
                    if (StringUtil.isEmpty(description)) "Syntax error" else description
                )
            }
        }

        val visitor = ErrorReportingVisitor()

        file.accept(visitor)

        return SyntaxErrorReport(visitor.hasErrors, visitor.allErrorsAtEof)
    }

    fun reportSpecialErrors(
        hasIncompatibleClasses: Boolean,
        hasPrereleaseClasses: Boolean,
        hasUnstableClasses: Boolean,
        messageCollector: MessageCollector,
    ) {
        if (hasIncompatibleClasses) {
            messageCollector.report(
                ERROR,
                "Incompatible classes were found in dependencies. " +
                        "Remove them from the classpath or use '-Xskip-metadata-version-check' to suppress errors"
            )
        }

        if (hasPrereleaseClasses) {
            messageCollector.report(
                ERROR,
                "Pre-release declarations were found in dependencies. Please exclude the dependencies with such declarations " +
                        "and recompile with a release compiler, or use '-Xskip-prerelease-check' to suppress errors. " +
                        "Note that in the latter case the compiled declarations will also be marked as pre-release."
            )
        }

        if (hasUnstableClasses) {
            messageCollector.report(
                ERROR,
                "Classes compiled by an unstable version of the Kotlin compiler were found in dependencies. " +
                        "Remove them from the classpath or use '-Xallow-unstable-dependencies' to suppress errors"
            )
        }
    }
}
