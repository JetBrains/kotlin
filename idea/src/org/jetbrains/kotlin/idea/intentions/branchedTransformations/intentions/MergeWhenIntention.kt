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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.core.appendElement
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStableVariable
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

public class MergeWhenIntention : SelfTargetingRangeIntention<KtWhenExpression>(javaClass(), "Merge with next 'when'", "Merge 'when' expressions") {
    override fun applicabilityRange(element: KtWhenExpression): TextRange? {
        val next = PsiTreeUtil.skipSiblingsForward(element, javaClass<PsiWhiteSpace>()) as? KtWhenExpression ?: return null

        val subject1 = element.getSubjectExpression()
        val subject2 = next.getSubjectExpression()
        if (!subject1.matches(subject2)) return null
        if (subject1 != null && !subject1.isStableVariable()) return null

        val entries1 = element.getEntries()
        val entries2 = next.getEntries()
        if (entries1.size() != entries2.size()) return null
        if (!entries1.zip(entries2).all { pair ->
            conditionsMatch(pair.first, pair.second) && checkBodies(pair.first, pair.second)
        }) return null

        return element.getWhenKeyword().getTextRange()
    }

    private fun conditionsMatch(e1: KtWhenEntry, e2: KtWhenEntry): Boolean =
            e1.getConditions().toList().toRange().matches(e2.getConditions().toList().toRange())

    private fun checkBodies(e1: KtWhenEntry, e2: KtWhenEntry): Boolean {
        val names1 = e1.declarationNames()
        val names2 = e2.declarationNames()
        if (names1.any { it in names2 }) return false

        return when (e1.getExpression()?.lastBlockStatementOrThis()) {
            is KtReturnExpression, is KtThrowExpression, is KtBreakExpression, is KtContinueExpression -> false
            else -> true
        }
    }

    private fun KtWhenEntry.declarationNames(): Set<String> =
            getExpression()?.blockExpressionsOrSingle()
                    ?.filter { it is KtNamedDeclaration }
                    ?.map { it.getName() }
                    ?.filterNotNull()?.toSet() ?: emptySet()

    override fun applyTo(element: KtWhenExpression, editor: Editor) {
        val nextWhen = PsiTreeUtil.skipSiblingsForward(element, javaClass<PsiWhiteSpace>()) as KtWhenExpression
        for ((entry1, entry2) in element.getEntries().zip(nextWhen.getEntries())) {
            entry1.getExpression().mergeWith(entry2.getExpression())
        }

        element.getParent().deleteChildRange(element.getNextSibling(), nextWhen)
    }

    private fun KtExpression?.mergeWith(that: KtExpression?): KtExpression? = when {
        this == null -> that

        that == null -> this

        else -> {
            val psiFactory = KtPsiFactory(this)
            val block = if (this is KtBlockExpression)
                this
            else
                replaced(psiFactory.createSingleStatementBlock(this))

            for (element in that.blockExpressionsOrSingle()) {
                val expression = block.appendElement(element)
                block.addBefore(psiFactory.createNewLine(), expression)
            }
            block
        }
    }
}
