/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KtDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.components.KtDiagnosticProvider
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.UnboundDiagnostic
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import kotlin.reflect.KClass

internal class KtFe10DiagnosticProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtDiagnosticProvider(), Fe10KtAnalysisSessionComponent {
    override val token: ValidityToken
        get() = analysisSession.token

    override fun getDiagnosticsForElement(element: KtElement, filter: KtDiagnosticCheckerFilter): Collection<KtDiagnosticWithPsi<*>> {
        withValidityAssertion {
            val bindingContext = analysisContext.analyze(element, AnalysisMode.PARTIAL_WITH_DIAGNOSTICS)
            val diagnostics = bindingContext.diagnostics.forElement(element)
            return diagnostics.map { KtFe10Diagnostic(it, token) }
        }
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: KtDiagnosticCheckerFilter): Collection<KtDiagnosticWithPsi<*>> {
        withValidityAssertion {
            val bindingContext = analysisContext.analyze(ktFile)
            val result = mutableListOf<KtDiagnosticWithPsi<*>>()
            for (diagnostic in bindingContext.diagnostics) {
                if (diagnostic.psiFile == ktFile) {
                    result += KtFe10Diagnostic(diagnostic, token)
                }
            }
            return result
        }
    }
}

internal class KtFe10Diagnostic(private val diagnostic: Diagnostic, override val token: ValidityToken) : KtDiagnosticWithPsi<PsiElement> {
    override val severity: Severity
        get() = diagnostic.severity

    override val factoryName: String
        get() = diagnostic.factory.name

    override val defaultMessage: String
        get() {
            @Suppress("UNCHECKED_CAST")
            val factory = diagnostic.factory as DiagnosticFactory<UnboundDiagnostic>?
            return factory?.defaultRenderer?.render(diagnostic)
                ?: DefaultErrorMessages.getRendererForDiagnostic(diagnostic)?.render(diagnostic)
                ?: ""
        }

    override val psi: PsiElement
        get() = diagnostic.psiElement

    override val textRanges: Collection<TextRange>
        get() = diagnostic.textRanges

    override val diagnosticClass: KClass<out KtDiagnosticWithPsi<PsiElement>>
        get() = KtFe10Diagnostic::class
}