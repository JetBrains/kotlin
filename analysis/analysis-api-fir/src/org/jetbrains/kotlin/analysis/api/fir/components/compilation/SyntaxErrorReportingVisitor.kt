/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.compilation

import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.diagnostics.ConeSyntaxDiagnostic
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import java.util.*

internal class SyntaxErrorReportingVisitor(
    private val useSiteSession: FirSession,
    private val diagnosticConverter: (KtDiagnosticWithSource) -> KaDiagnosticWithPsi<*>
) : KtTreeVisitorVoid() {
    private val collectedDiagnostics = mutableListOf<KaDiagnostic>()

    val diagnostics: List<KaDiagnostic>
        get() = Collections.unmodifiableList(collectedDiagnostics)

    override fun visitErrorElement(element: PsiErrorElement) {
        collectedDiagnostics += ConeSyntaxDiagnostic(element.errorDescription)
            .toFirDiagnostics(useSiteSession, KtRealPsiSourceElement(element), callOrAssignmentSource = null)
            .map { diagnosticConverter(it as KtDiagnosticWithSource) }

        super.visitErrorElement(element)
    }
}
