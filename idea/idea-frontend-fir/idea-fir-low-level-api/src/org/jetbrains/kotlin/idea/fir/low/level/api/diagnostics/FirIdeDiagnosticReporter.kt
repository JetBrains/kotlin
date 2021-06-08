/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.idea.fir.low.level.api.util.addValueFor

internal class FirIdeDiagnosticReporter : DiagnosticReporter() {
    val diagnostics = mutableMapOf<PsiElement, MutableList<FirPsiDiagnostic<*>>>()

    override fun report(diagnostic: FirDiagnostic<*>?, context: CheckerContext) {
        if (diagnostic == null) return
        if (context.isDiagnosticSuppressed(diagnostic)) return

        val psiDiagnostic = when (diagnostic) {
            is FirPsiDiagnostic<*> -> diagnostic
            is FirLightDiagnostic -> diagnostic.toPsiDiagnostic()
            else -> error("Unknown diagnostic type ${diagnostic::class.simpleName}")
        }
        diagnostics.addValueFor(psiDiagnostic.psiElement, psiDiagnostic)
    }
}

private fun FirLightDiagnostic.toPsiDiagnostic(): FirPsiDiagnostic<*> {
    val psiSourceElement = element.unwrapToFirPsiSourceElement()
        ?: error("Diagnostic should be created from PSI in IDE")
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is FirLightSimpleDiagnostic -> FirPsiSimpleDiagnostic(
            psiSourceElement,
            severity,
            factory as FirDiagnosticFactory0<PsiElement>,
            positioningStrategy
        )

        is FirLightDiagnosticWithParameters1<*> -> FirPsiDiagnosticWithParameters1(
            psiSourceElement,
            a,
            severity,
            factory as FirDiagnosticFactory1<PsiElement, Any?>,
            positioningStrategy
        )

        is FirLightDiagnosticWithParameters2<*, *> -> FirPsiDiagnosticWithParameters2(
            psiSourceElement,
            a, b,
            severity,
            factory as FirDiagnosticFactory2<PsiElement, Any?, Any?>,
            positioningStrategy
        )

        is FirLightDiagnosticWithParameters3<*, *, *> -> FirPsiDiagnosticWithParameters3(
            psiSourceElement,
            a, b, c,
            severity,
            factory as FirDiagnosticFactory3<PsiElement, Any?, Any?, Any?>,
            positioningStrategy
        )

        is FirLightDiagnosticWithParameters4<*, *, *, *> -> FirPsiDiagnosticWithParameters4(
            psiSourceElement,
            a, b, c, d,
            severity,
            factory as FirDiagnosticFactory4<PsiElement, Any?, Any?, Any?, Any?>,
            positioningStrategy
        )
        else -> error("Unknown diagnostic type ${this::class.simpleName}")
    }
}
