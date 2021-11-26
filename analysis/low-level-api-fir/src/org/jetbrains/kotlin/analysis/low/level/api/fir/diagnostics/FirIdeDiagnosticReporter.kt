/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.addValueFor
import org.jetbrains.kotlin.diagnostics.*

internal class FirIdeDiagnosticReporter : DiagnosticReporter() {
    val diagnostics = mutableMapOf<PsiElement, MutableList<KtPsiDiagnostic>>()

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return
        if (context.isDiagnosticSuppressed(diagnostic)) return

        val psiDiagnostic = when (diagnostic) {
            is KtPsiDiagnostic -> diagnostic
            is KtLightDiagnostic -> diagnostic.toPsiDiagnostic()
            else -> error("Unknown diagnostic type ${diagnostic::class.simpleName}")
        }
        diagnostics.addValueFor(psiDiagnostic.psiElement, psiDiagnostic)
    }
}

private fun KtLightDiagnostic.toPsiDiagnostic(): KtPsiDiagnostic {
    val psiSourceElement = element.unwrapToKtPsiSourceElement()
        ?: error("Diagnostic should be created from PSI in IDE")
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is KtLightSimpleDiagnostic -> KtPsiSimpleDiagnostic(
            psiSourceElement,
            severity,
            factory,
            positioningStrategy
        )

        is KtLightDiagnosticWithParameters1<*> -> KtPsiDiagnosticWithParameters1(
            psiSourceElement,
            a,
            severity,
            factory as KtDiagnosticFactory1<Any?>,
            positioningStrategy
        )

        is KtLightDiagnosticWithParameters2<*, *> -> KtPsiDiagnosticWithParameters2(
            psiSourceElement,
            a, b,
            severity,
            factory as KtDiagnosticFactory2<Any?, Any?>,
            positioningStrategy
        )

        is KtLightDiagnosticWithParameters3<*, *, *> -> KtPsiDiagnosticWithParameters3(
            psiSourceElement,
            a, b, c,
            severity,
            factory as KtDiagnosticFactory3<Any?, Any?, Any?>,
            positioningStrategy
        )

        is KtLightDiagnosticWithParameters4<*, *, *, *> -> KtPsiDiagnosticWithParameters4(
            psiSourceElement,
            a, b, c, d,
            severity,
            factory as KtDiagnosticFactory4<Any?, Any?, Any?, Any?>,
            positioningStrategy
        )
        else -> error("Unknown diagnostic type ${this::class.simpleName}")
    }
}
