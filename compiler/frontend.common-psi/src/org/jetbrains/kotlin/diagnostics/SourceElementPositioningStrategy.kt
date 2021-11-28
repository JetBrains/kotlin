/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement

class SourceElementPositioningStrategy(
    private val lightTreeStrategy: LightTreePositioningStrategy,
    private val psiStrategy: PositioningStrategy<*>,
    private val offsetsOnlyPositioningStrategy: OffsetsOnlyPositioningStrategy = OffsetsOnlyPositioningStrategy(),
) : AbstractSourceElementPositioningStrategy() {
    override fun markDiagnostic(diagnostic: KtDiagnostic): List<TextRange> {
        return when (val element = diagnostic.element) {
            is KtPsiSourceElement -> psiStrategy.markDiagnostic(diagnostic)
            is KtLightSourceElement -> lightTreeStrategy.markKtDiagnostic(element, diagnostic)
            else -> offsetsOnlyPositioningStrategy.markKtDiagnostic(element, diagnostic)
        }
    }

    override fun isValid(element: AbstractKtSourceElement): Boolean {
        return when (element) {
            is KtPsiSourceElement -> psiStrategy.hackyIsValid(element.psi)
            is KtLightSourceElement -> lightTreeStrategy.isValid(element.lighterASTNode, element.treeStructure)
            else -> true
        }
    }

    private fun PositioningStrategy<*>.hackyIsValid(psi: PsiElement): Boolean {
        @Suppress("UNCHECKED_CAST")
        return (this as PositioningStrategy<PsiElement>).isValid(psi)
    }
}
