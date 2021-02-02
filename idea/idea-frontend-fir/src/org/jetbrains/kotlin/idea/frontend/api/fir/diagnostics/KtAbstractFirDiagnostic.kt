/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDefaultErrorMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderer
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi

internal interface KtAbstractFirDiagnostic<PSI : PsiElement> : KtDiagnosticWithPsi<PSI> {
    val firDiagnostic: FirPsiDiagnostic<*>

    override val factoryName: String
        get() = firDiagnostic.factory.name

    override val defaultMessage: String
        get() {
            val diagnostic = firDiagnostic as FirDiagnostic<*>

            @Suppress("UNCHECKED_CAST")
            val firDiagnosticRenderer =
                FirDefaultErrorMessages.getRendererForDiagnostic(diagnostic) as FirDiagnosticRenderer<FirDiagnostic<*>>
            return firDiagnosticRenderer.render(diagnostic)
        }

    override val textRanges: Collection<TextRange>
        get() = firDiagnostic.textRanges

    @Suppress("UNCHECKED_CAST")
    override val psi: PSI
        get() = firDiagnostic.psiElement as PSI

    override val severity: Severity
        get() = firDiagnostic.severity
}
