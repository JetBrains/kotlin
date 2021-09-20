/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.cfg.UnreachableCode
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration


object PsiPositioningStrategies {
    val UNREACHABLE_CODE = object : PositioningStrategy<PsiElement>() {
        override fun markDiagnostic(diagnostic: DiagnosticMarker): List<TextRange> {
            //todo it is better to implement arguments extraction in KtDiagnosticFactory, but kotlin struggle with checking types in it atm
            @Suppress("UNCHECKED_CAST")
            val typed = diagnostic as KtDiagnosticWithParameters2<Set<KtSourceElement>, Set<KtSourceElement>>
            val source = diagnostic.element as KtPsiSourceElement
            return UnreachableCode.getUnreachableTextRanges(
                source.psi as KtElement,
                typed.a.mapNotNull { it.psi as? KtElement }.toSet(),
                typed.b.mapNotNull { it.psi as? KtElement }.toSet()
            )
        }
    }

    val ACTUAL_DECLARATION_NAME = object : PositioningStrategy<PsiElement>() {
        override fun markDiagnostic(diagnostic: DiagnosticMarker): List<TextRange> {
            require(diagnostic is KtDiagnostic)
            val element = diagnostic.element.psi ?: return emptyList()
            (element as? KtNamedDeclaration)?.nameIdentifier?.let { nameIdentifier ->
                return mark(nameIdentifier)
            }
            return mark(element)
        }
    }
}
