/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

private val SIGNIFICANT_FILTER = { e: PsiElement -> e !is PsiWhiteSpace && e !is PsiComment && e.getTextLength() > 0 }

public interface KotlinPsiRange {
    public object Empty : KotlinPsiRange {
        override val elements: List<PsiElement> get() = Collections.emptyList<PsiElement>()

        override fun getTextRange(): TextRange = TextRange.EMPTY_RANGE
    }

    public class ListRange(override val elements: List<PsiElement>): KotlinPsiRange {
        val startElement: PsiElement = elements.first()
        val endElement: PsiElement = elements.last()

        override fun getTextRange(): TextRange {
            val startRange = startElement.getTextRange()
            val endRange = endElement.getTextRange()
            if (startRange == null || endRange == null) return TextRange.EMPTY_RANGE

            return TextRange(startRange.getStartOffset(), endRange.getEndOffset())
        }
    }

    public class Match(val range: KotlinPsiRange, val result: UnificationResult.Matched)

    val elements: List<PsiElement>

    fun getTextRange(): TextRange

    fun isValid(): Boolean = elements.all { it.isValid() }

    val empty: Boolean get() = this is Empty

    fun contains(element: PsiElement): Boolean = getTextRange().contains(element.getTextRange() ?: TextRange.EMPTY_RANGE)

    fun match(scope: PsiElement, unifier: KotlinPsiUnifier): List<Match> {
        val elements = elements.filter(SIGNIFICANT_FILTER)
        if (elements.isEmpty()) return Collections.emptyList()

        val matches = ArrayList<Match>()
        scope.accept(
                object: KtTreeVisitorVoid() {
                    override fun visitJetElement(element: KtElement) {
                        val range = element
                                .siblings()
                                .filter(SIGNIFICANT_FILTER)
                                .take(elements.size())
                                .toList()
                                .toRange()

                        val result = unifier.unify(range, this@KotlinPsiRange)

                        if (result is UnificationResult.StronglyMatched) {
                            matches.add(Match(range, result))
                        }
                        else {
                            val matchCountSoFar = matches.size()
                            super.visitJetElement(element)
                            if (result is UnificationResult.WeaklyMatched && matches.size() == matchCountSoFar) {
                                matches.add(Match(range, result))
                            }
                        }
                    }
                }
        )
        return matches
    }
}

public fun List<PsiElement>.toRange(significantOnly: Boolean = true): KotlinPsiRange {
    return if (isEmpty()) KotlinPsiRange.Empty else KotlinPsiRange.ListRange(if (significantOnly) filter(SIGNIFICANT_FILTER) else this)
}

public fun PsiElement?.toRange(): KotlinPsiRange = this?.let { KotlinPsiRange.ListRange(Collections.singletonList(it)) } ?: KotlinPsiRange.Empty
