/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.PositioningStrategy
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement

class SourceElementPositioningStrategy(
    private val lightTreeStrategy: LightTreePositioningStrategy,
    private val psiStrategy: PositioningStrategy<*>
) {
    fun markDiagnostic(diagnostic: FirDiagnostic): List<TextRange> {
        val element = diagnostic.element
        if (element is FirPsiSourceElement) {
            @Suppress("UNCHECKED_CAST")
            return psiStrategy.hackyMark(element.psi)
        }
        return lightTreeStrategy.mark(element.lighterASTNode, element.startOffset, element.endOffset, element.treeStructure)
    }

    fun isValid(element: FirSourceElement): Boolean {
        if (element is FirPsiSourceElement) {
            @Suppress("UNCHECKED_CAST")
            return psiStrategy.hackyIsValid(element.psi)
        }
        return lightTreeStrategy.isValid(element.lighterASTNode, element.treeStructure)
    }

    private fun PositioningStrategy<*>.hackyMark(psi: PsiElement): List<TextRange> {
        @Suppress("UNCHECKED_CAST")
        return (this as PositioningStrategy<PsiElement>).mark(psi)
    }

    private fun PositioningStrategy<*>.hackyIsValid(psi: PsiElement): Boolean {
        @Suppress("UNCHECKED_CAST")
        return (this as PositioningStrategy<PsiElement>).isValid(psi)
    }

    companion object {
        val DEFAULT: SourceElementPositioningStrategy = SourceElementPositioningStrategies.DEFAULT
    }
}
