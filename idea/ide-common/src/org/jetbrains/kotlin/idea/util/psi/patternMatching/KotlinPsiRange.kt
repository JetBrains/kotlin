/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util.psi.patternMatching

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.siblings
import java.util.*

private val SIGNIFICANT_FILTER = { e: PsiElement -> e !is PsiWhiteSpace && e !is PsiComment && e.textLength > 0 }

interface KotlinPsiRange {
    object Empty : KotlinPsiRange {
        override val elements: List<PsiElement> get() = Collections.emptyList<PsiElement>()

        override fun getTextRange(): TextRange = TextRange.EMPTY_RANGE
    }

    class ListRange(override val elements: List<PsiElement>) : KotlinPsiRange {
        val startElement: PsiElement = elements.first()
        val endElement: PsiElement = elements.last()

        override fun getTextRange(): TextRange {
            val startRange = startElement.textRange
            val endRange = endElement.textRange
            if (startRange == null || endRange == null) return TextRange.EMPTY_RANGE

            return TextRange(startRange.startOffset, endRange.endOffset)
        }
    }

    val elements: List<PsiElement>

    fun getTextRange(): TextRange

    fun isValid(): Boolean = elements.all { it.isValid }

    val empty: Boolean get() = this is Empty

    operator fun contains(element: PsiElement): Boolean = getTextRange().contains(element.textRange ?: TextRange.EMPTY_RANGE)

    fun match(scope: PsiElement, unifier: KotlinPsiUnifier): List<UnificationResult.Matched> {
        val elements = elements.filter(SIGNIFICANT_FILTER)
        if (elements.isEmpty()) return Collections.emptyList()

        val matches = ArrayList<UnificationResult.Matched>()
        scope.accept(
            object : KtTreeVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    val range = element
                        .siblings()
                        .filter(SIGNIFICANT_FILTER)
                        .take(elements.size)
                        .toList()
                        .toRange()

                    val result = unifier.unify(range, this@KotlinPsiRange)

                    if (result is UnificationResult.StronglyMatched) {
                        matches.add(result)
                    } else {
                        val matchCountSoFar = matches.size
                        super.visitKtElement(element)
                        if (result is UnificationResult.WeaklyMatched && matches.size == matchCountSoFar) {
                            matches.add(result)
                        }
                    }
                }
            }
        )
        return matches
    }
}

fun List<PsiElement>.toRange(significantOnly: Boolean = true): KotlinPsiRange {
    val elements = if (significantOnly) filter(SIGNIFICANT_FILTER) else this
    return if (elements.isEmpty()) KotlinPsiRange.Empty else KotlinPsiRange.ListRange(elements)
}

fun PsiElement?.toRange(): KotlinPsiRange = this?.let { KotlinPsiRange.ListRange(Collections.singletonList(it)) } ?: KotlinPsiRange.Empty
