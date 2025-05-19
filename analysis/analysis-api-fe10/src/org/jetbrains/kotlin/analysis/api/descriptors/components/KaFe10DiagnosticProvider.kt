/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.impl.base.util.toAnalysisApiSeverity
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.UnboundDiagnostic
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import kotlin.reflect.KClass

internal class KaFe10DiagnosticProvider(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseSessionComponent<KaFe10Session>(), KaDiagnosticProvider, KaFe10SessionComponent {
    override fun KtElement.diagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> = withPsiValidityAssertion {
        val bindingContext = analysisContext.analyze(this, AnalysisMode.PARTIAL_WITH_DIAGNOSTICS)
        val diagnostics = bindingContext.diagnostics.forElement(this)
        return diagnostics.map { KaFe10Diagnostic(it, token) }
    }

    override fun KtFile.collectDiagnostics(
        filter: KaDiagnosticCheckerFilter,
    ): Collection<KaDiagnosticWithPsi<*>> = withPsiValidityAssertion {
        val bindingContext = analysisContext.analyze(this)
        val result = mutableListOf<KaDiagnosticWithPsi<*>>()
        for (diagnostic in bindingContext.diagnostics) {
            if (this == diagnostic.psiFile) {
                result += KaFe10Diagnostic(diagnostic, token)
            }
        }
        return result
    }
}

internal class KaFe10Diagnostic(private val diagnostic: Diagnostic, override val token: KaLifetimeToken) : KaDiagnosticWithPsi<PsiElement> {
    override val severity: KaSeverity
        get() = withValidityAssertion { diagnostic.severity.toAnalysisApiSeverity() }

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