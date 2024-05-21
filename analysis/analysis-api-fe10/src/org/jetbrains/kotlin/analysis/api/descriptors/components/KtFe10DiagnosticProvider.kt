/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticProvider
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.UnboundDiagnostic
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import kotlin.reflect.KClass

internal class KaFe10DiagnosticProvider(
    override val analysisSession: KaFe10Session
) : KaDiagnosticProvider(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun getDiagnosticsForElement(element: KtElement, filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> {
        val bindingContext = analysisContext.analyze(element, AnalysisMode.PARTIAL_WITH_DIAGNOSTICS)
        val diagnostics = bindingContext.diagnostics.forElement(element)
        return diagnostics.map { KaFe10Diagnostic(it, token) }
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> {
        val bindingContext = analysisContext.analyze(ktFile)
        val result = mutableListOf<KaDiagnosticWithPsi<*>>()
        for (diagnostic in bindingContext.diagnostics) {
            if (diagnostic.psiFile == ktFile) {
                result += KaFe10Diagnostic(diagnostic, token)
            }
        }
        return result
    }
}

internal class KaFe10Diagnostic(private val diagnostic: Diagnostic, override val token: KaLifetimeToken) : KaDiagnosticWithPsi<PsiElement> {
    override val severity: Severity
        get() = withValidityAssertion { diagnostic.severity }

    override val factoryName: String
        get() = withValidityAssertion { diagnostic.factory.name }

    override val defaultMessage: String
        get() = withValidityAssertion {
            @Suppress("UNCHECKED_CAST")
            val factory = diagnostic.factory as DiagnosticFactory<UnboundDiagnostic>?
            return factory?.defaultRenderer?.render(diagnostic)
                ?: DefaultErrorMessages.getRendererForDiagnostic(diagnostic)?.render(diagnostic)
                ?: ""
        }

    override val psi: PsiElement
        get() = withValidityAssertion { diagnostic.psiElement }

    override val textRanges: Collection<TextRange>
        get() = withValidityAssertion { diagnostic.textRanges }

    override val diagnosticClass: KClass<out KaDiagnosticWithPsi<PsiElement>>
        get() = withValidityAssertion { KaFe10Diagnostic::class }
}