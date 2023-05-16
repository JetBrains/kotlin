/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

internal abstract class KtAbstractFirDiagnostic<PSI : PsiElement>(
    private val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtDiagnosticWithPsi<PSI>, KtLifetimeOwner {

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

    override val severity: Severity
        get() = withValidityAssertion { firDiagnostic.severity }
}
