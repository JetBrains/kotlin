/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.applicator

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.psi.KtElement

sealed class HLApplicabilityRange<in ELEMENT : PsiElement> {
    /**
     * Returns the list of ranges on which [HLApplicator] is available
     * The ranges are relative to [element]
     */
    abstract fun getApplicabilityRanges(element: ELEMENT): List<TextRange>
}

private class HLApplicabilityRangeImpl<ELEMENT : PsiElement>(
    private val getApplicabilityRanges: (ELEMENT) -> List<TextRange>,
) : HLApplicabilityRange<ELEMENT>() {
    override fun getApplicabilityRanges(element: ELEMENT): List<TextRange> =
        getApplicabilityRanges.invoke(element)
}


fun <ELEMENT : KtElement> applicabilityRanges(
    getRanges: (ELEMENT) -> List<TextRange>
): HLApplicabilityRange<ELEMENT> =
    HLApplicabilityRangeImpl(getRanges)

fun <ELEMENT : KtElement> applicabilityRange(
    getRange: (ELEMENT) -> TextRange?
): HLApplicabilityRange<ELEMENT> =
    HLApplicabilityRangeImpl { listOfNotNull(getRange(it)) }

fun <ELEMENT : PsiElement> applicabilityTarget(
    getTarget: (ELEMENT) -> PsiElement?
): HLApplicabilityRange<ELEMENT> =
    HLApplicabilityRangeImpl { element ->
        when (val target = getTarget(element)) {
            null -> emptyList()
            element -> listOf(TextRange(0, element.textLength))
            else -> listOf(target.textRangeIn(element))
        }
    }

