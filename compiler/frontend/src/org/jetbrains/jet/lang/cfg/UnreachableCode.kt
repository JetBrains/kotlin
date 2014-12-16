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

package org.jetbrains.jet.lang.cfg

import org.jetbrains.jet.lang.psi.JetElement
import com.intellij.openapi.util.TextRange
import java.util.HashSet
import org.jetbrains.jet.lang.psi.JetPsiUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiWhiteSpace
import java.util.ArrayList
import org.jetbrains.jet.lexer.JetTokens
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiComment

trait UnreachableCode {
    val elements: Set<JetElement>
    fun getUnreachableTextRanges(element: JetElement): List<TextRange>
}

class UnreachableCodeImpl(
        private val reachableElements: Set<JetElement>,
        private val unreachableElements: Set<JetElement>
) : UnreachableCode {

    // This is needed in order to highlight only '1 < 2' and not '1', '<' and '2' as well
    override val elements = JetPsiUtil.findRootExpressions(unreachableElements)

    override fun getUnreachableTextRanges(element: JetElement): List<TextRange> {
        return if (element.hasChildrenInSet(reachableElements)) {
            element.getLeavesOrReachableChildren().removeReachableElementsWithMeaninglessSiblings().mergeAdjacentTextRanges()
        }
        else {
            listOf(element.getTextRange()!!)
        }
    }

    private fun JetElement.hasChildrenInSet(set: Set<JetElement>): Boolean {
        return PsiTreeUtil.collectElements(this) { it != this }.any { it in set }
    }

    private fun JetElement.getLeavesOrReachableChildren(): List<PsiElement> {
        val children = ArrayList<PsiElement>()
        acceptChildren(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val isReachable = element is JetElement && reachableElements.contains(element) && !element.hasChildrenInSet(unreachableElements)
                if (isReachable || element.getChildren().size() == 0) {
                    children.add(element)
                }
                else {
                    element.acceptChildren(this)
                }
            }
        })
        return children
    }

    fun List<PsiElement>.removeReachableElementsWithMeaninglessSiblings(): List<PsiElement> {
        fun PsiElement.isMeaningless() = this is PsiWhiteSpace
                || this.getNode()?.getElementType() == JetTokens.COMMA
                || this is PsiComment

        val childrenToRemove = HashSet<PsiElement>()
        fun collectSiblingsIfMeaningless(elementIndex: Int, direction: Int) {
            val index = elementIndex + direction
            if (index !in 0..(size() - 1)) return

            val element = this[index]
            if (element.isMeaningless()) {
                childrenToRemove.add(element)
                collectSiblingsIfMeaningless(index, direction)
            }
        }
        for ((index, element) in this.withIndices()) {
            if (reachableElements.contains(element)) {
                childrenToRemove.add(element)
                collectSiblingsIfMeaningless(index, -1)
                collectSiblingsIfMeaningless(index, 1)
            }
        }
        return this.filter { it !in childrenToRemove }
    }


    private fun List<PsiElement>.mergeAdjacentTextRanges(): List<TextRange> {
        val result = ArrayList<TextRange>()
        val lastRange = fold(null: TextRange?) {
            currentTextRange, element ->

            val elementRange = element.getTextRange()!!
            if (currentTextRange == null) {
                elementRange
            }
            else if (currentTextRange.getEndOffset() == elementRange.getStartOffset()) {
                currentTextRange.union(elementRange)
            }
            else {
                result.add(currentTextRange)
                elementRange
            }
        }
        if (lastRange != null) {
            result.add(lastRange)
        }
        return result
    }
}
