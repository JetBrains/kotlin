/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.analysis.api.impl.base.util.toAnalysisApiSeverity
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

internal abstract class KaAbstractFirDiagnostic<PSI : PsiElement>(
    private val firDiagnostic: KtPsiDiagnostic,
    override val token: KaLifetimeToken,
) : KaDiagnosticWithPsi<PSI>, KaLifetimeOwner {

    override val factoryName: String
        get() = withValidityAssertion { firDiagnostic.factory.name }

    override val defaultMessage: String
        get() = withValidityAssertion {
            val diagnostic = firDiagnostic as KtDiagnostic

            val firDiagnosticRenderer = RootDiagnosticRendererFactory(diagnostic)
            return firDiagnosticRenderer.render(diagnostic)
        }

    override val textRanges: Collection<TextRange>
        get() = withValidityAssertion { firDiagnostic.textRanges }

    @Suppress("UNCHECKED_CAST")
    override val psi: PSI
        get() = withValidityAssertion { firDiagnostic.psiElement as PSI }

    override val severity: KaSeverity
        get() = withValidityAssertion { firDiagnostic.severity.toAnalysisApiSeverity() }
}
