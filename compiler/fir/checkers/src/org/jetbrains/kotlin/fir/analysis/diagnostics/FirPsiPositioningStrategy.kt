/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.cfg.UnreachableCode
import org.jetbrains.kotlin.diagnostics.PositioningStrategy
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtElement

abstract class FirPsiPositioningStrategy<in E : PsiElement> : PositioningStrategy<E>(),
    FirDiagnosticPositioningStrategy<FirPsiSourceElement>

object FirPsiPositioningStrategies {

    val UNREACHABLE_CODE = object : FirPsiPositioningStrategy<PsiElement>() {

        override fun markFirDiagnostic(element: FirPsiSourceElement, diagnostic: FirDiagnostic): List<TextRange> {
            //todo it is better to implement arguments extraction in FirDiagnosticFactory, but kotlin struggle with checking types in it atm
            @Suppress("UNCHECKED_CAST")
            val typed = diagnostic as FirDiagnosticWithParameters2<Set<FirSourceElement>, Set<FirSourceElement>>
            return UnreachableCode.getUnreachableTextRanges(
                element.psi as KtElement,
                typed.a.mapNotNull { it.psi as? KtElement }.toSet(),
                typed.b.mapNotNull { it.psi as? KtElement }.toSet()
            )
        }
    }

}