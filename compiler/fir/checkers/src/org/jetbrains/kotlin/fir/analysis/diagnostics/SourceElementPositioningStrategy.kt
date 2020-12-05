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

open class SourceElementPositioningStrategy<in E : PsiElement>(
    val lightTreeStrategy: LightTreePositioningStrategy,
    val psiStrategy: PositioningStrategy<E>
) {
    fun markDiagnostic(diagnostic: FirDiagnostic<*>): List<TextRange> {
        val element = diagnostic.element
        if (element is FirPsiSourceElement<*>) {
            return psiStrategy.mark(element.psi as E)
        }
        return lightTreeStrategy.mark(element.lighterASTNode, element.treeStructure)
    }

    fun isValid(element: FirSourceElement): Boolean {
        if (element is FirPsiSourceElement<*>) {
            return psiStrategy.isValid(element.psi as E)
        }
        return lightTreeStrategy.isValid(element.lighterASTNode, element.treeStructure)
    }

    companion object {
        val DEFAULT: SourceElementPositioningStrategy<PsiElement> = SourceElementPositioningStrategies.DEFAULT
    }
}