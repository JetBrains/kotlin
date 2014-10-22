/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.util.psi.patternMatching

import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid
import org.jetbrains.jet.lang.psi.JetElement
import java.util.ArrayList
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiComment
import java.util.Collections
import org.jetbrains.jet.lang.psi.psiUtil.siblings
import com.intellij.openapi.util.TextRange
import java.util.HashSet
import org.jetbrains.jet.plugin.util.psi.patternMatching.JetPsiRange.Match

private val SIGNIFICANT_FILTER = { (e: PsiElement) -> e !is PsiWhiteSpace && e !is PsiComment && e.getTextLength() > 0 }

public trait JetPsiRange {
    public object Empty : JetPsiRange {
        override val elements: List<PsiElement> get() = Collections.emptyList<PsiElement>()

        override fun getTextRange(): TextRange = TextRange.EMPTY_RANGE
    }

    public class ListRange(override val elements: List<PsiElement>): JetPsiRange {
        val startElement: PsiElement = elements.first()
        val endElement: PsiElement = elements.last()

        override fun getTextRange(): TextRange {
            val startRange = startElement.getTextRange()
            val endRange = endElement.getTextRange()
            if (startRange == null || endRange == null) return TextRange.EMPTY_RANGE

            return TextRange(startRange.getStartOffset(), endRange.getEndOffset())
        }
    }

    public class Match(val range: JetPsiRange, val result: UnificationResult.Matched)

    val elements: List<PsiElement>

    fun getTextRange(): TextRange

    fun isValid(): Boolean = elements.all { it.isValid() }

    val empty: Boolean get() = this is Empty

    fun contains(element: PsiElement): Boolean = getTextRange().contains(element.getTextRange() ?: TextRange.EMPTY_RANGE)

    fun match(scope: PsiElement, unifier: JetPsiUnifier): List<Match> {
        val elements = elements.filter(SIGNIFICANT_FILTER)
        if (elements.empty) return Collections.emptyList()

        val matches = ArrayList<Match>()
        scope.accept(
                object: JetTreeVisitorVoid() {
                    override fun visitJetElement(element: JetElement) {
                        val range = element
                                .siblings()
                                .filter(SIGNIFICANT_FILTER)
                                .take(elements.size)
                                .toList()
                                .toRange()

                        val result = unifier.unify(range, this@JetPsiRange)

                        if (result is UnificationResult.StronglyMatched) {
                            matches.add(Match(range, result))
                        }
                        else {
                            val matchCountSoFar = matches.size
                            super.visitJetElement(element)
                            if (result is UnificationResult.WeaklyMatched && matches.size == matchCountSoFar) {
                                matches.add(Match(range, result))
                            }
                        }
                    }
                }
        )
        return matches
    }
}

public fun List<PsiElement>.toRange(significantOnly: Boolean = true): JetPsiRange {
    return if (empty) JetPsiRange.Empty else JetPsiRange.ListRange(if (significantOnly) filter(SIGNIFICANT_FILTER) else this)
}

public fun PsiElement?.toRange(): JetPsiRange = this?.let { JetPsiRange.ListRange(Collections.singletonList(it)) } ?: JetPsiRange.Empty