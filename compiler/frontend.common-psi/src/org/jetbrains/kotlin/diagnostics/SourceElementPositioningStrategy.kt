/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtMissingSourceElement
import org.jetbrains.kotlin.KtOffsetsOnlySourceElement
import org.jetbrains.kotlin.KtPsiSourceElement

@OptIn(DiagnosticLossRisk::class)
class SourceElementPositioningStrategy(
    private val lightTreeStrategy: LightTreePositioningStrategy,
    psiStrategy: PositioningStrategy<*>,
) : AbstractSourceElementPositioningStrategy() {
    // The strategy is only applied to the elements it's registered for, so the erased element type is safe
    @Suppress("UNCHECKED_CAST")
    private val psiStrategy = psiStrategy as PositioningStrategy<PsiElement>

    companion object {
        private val improperTextRanges = listOf(PossiblyImproperTextRange(-1, -1))
    }

    override fun markDiagnostic(diagnostic: KtDiagnosticWithSource): List<TextRange> {
        return when (val element = diagnostic.element) {
            is KtMissingSourceElement -> improperTextRanges
            is KtOffsetsOnlySourceElement -> listOf(PossiblyImproperTextRange(element.startOffset, element.endOffset))
            is KtPsiSourceElement -> psiStrategy.markDiagnostic(diagnostic)
            is KtLightSourceElement -> lightTreeStrategy.markKtDiagnostic(element, diagnostic)
        }
    }

    override fun isValid(element: AbstractKtSourceElement): Boolean {
        return when (element) {
            is KtMissingSourceElement -> true
            is KtOffsetsOnlySourceElement -> true
            is KtPsiSourceElement -> psiStrategy.isValid(element.psi)
            is KtLightSourceElement -> lightTreeStrategy.isValid(element.lighterASTNode, element.treeStructure)
        }
    }
}
