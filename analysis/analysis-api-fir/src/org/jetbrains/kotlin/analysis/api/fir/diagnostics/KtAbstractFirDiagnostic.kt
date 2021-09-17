/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDefaultErrorMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.KtPsiDiagnostic

internal interface KtAbstractFirDiagnostic<PSI : PsiElement> : KtDiagnosticWithPsi<PSI>, ValidityTokenOwner {
    val firDiagnostic: KtPsiDiagnostic

    override val factoryName: String
        get() = withValidityAssertion { firDiagnostic.factory.name }

    override val defaultMessage: String
        get() = withValidityAssertion {
            val diagnostic = firDiagnostic as KtDiagnostic

            val firDiagnosticRenderer = FirDefaultErrorMessages.getRendererForDiagnostic(diagnostic)
            return firDiagnosticRenderer.render(diagnostic)
        }

    override val textRanges: Collection<TextRange>
        get() = withValidityAssertion { firDiagnostic.textRanges }

    @Suppress("UNCHECKED_CAST")
    override val psi: PSI
        get() = withValidityAssertion { firDiagnostic.psiElement as PSI }

    override val severity: Severity
        get() = withValidityAssertion { firDiagnostic.severity }
}
