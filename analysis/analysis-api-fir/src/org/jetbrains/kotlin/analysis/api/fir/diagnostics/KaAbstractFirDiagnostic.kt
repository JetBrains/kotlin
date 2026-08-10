/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.analysis.api.impl.base.util.toAnalysisApiSeverity
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic

internal abstract class KaAbstractFirDiagnostic<PSI : PsiElement>(
    private val firDiagnostic: KtPsiDiagnostic,
    override val token: KaLifetimeToken,
) : KaDiagnosticWithPsi<PSI>, KaLifetimeOwner {

    override val factoryName: String
        get() = withValidityAssertion { firDiagnostic.factory.name }

    override val defaultMessage: String
        get() = withValidityAssertion {
            val diagnostic = firDiagnostic as KtDiagnostic
            return diagnostic.renderMessage()
        }

    override val textRanges: Collection<TextRange>
        get() = withValidityAssertion { firDiagnostic.textRanges }

    @Suppress("UNCHECKED_CAST")
    override val psi: PSI
        get() = withValidityAssertion { firDiagnostic.psiElement as PSI }

    override val severity: KaSeverity
        get() = withValidityAssertion { firDiagnostic.severity.toAnalysisApiSeverity() }

    /**
     * The suppression status is only known during diagnostic collection, so it is assigned right after the conversion from
     * [LLDiagnostic][org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLDiagnostic]. As a fresh diagnostic instance is created per
     * conversion, clients cannot observe the assignment.
     *
     * @see org.jetbrains.kotlin.analysis.api.fir.asKaDiagnostic
     */
    @KaExperimentalApi
    override var isSuppressed: Boolean = false
        get() = withValidityAssertion { field }
        set(value) = withValidityAssertion { field = value }
}
