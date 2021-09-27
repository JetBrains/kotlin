/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement

open class OffsetsOnlyPositioningStrategy : AbstractSourceElementPositioningStrategy() {
    open fun markKtDiagnostic(element: AbstractKtSourceElement, diagnostic: KtDiagnostic): List<TextRange> {
        return mark(element.startOffset, element.endOffset)
    }

    open fun mark(
        startOffset: Int,
        endOffset: Int,
    ): List<TextRange> {
        return markElement(startOffset, endOffset)
    }

    override fun markDiagnostic(diagnostic: KtDiagnostic): List<TextRange> = markKtDiagnostic(diagnostic.element, diagnostic)

    override fun isValid(element: AbstractKtSourceElement): Boolean = true
}

fun markElement(
    startOffset: Int,
    endOffset: Int,
): List<TextRange> = markRange(startOffset, endOffset)

fun markRange(
    startOffset: Int,
    endOffset: Int,
): List<TextRange> {
    return listOf(markSingleElement(startOffset, endOffset))
}

fun markSingleElement(
    startOffset: Int,
    endOffset: Int,
): TextRange {
    return TextRange(startOffset, endOffset)
}
