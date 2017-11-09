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
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStable
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

class MergeWhenIntention : SelfTargetingRangeIntention<KtWhenExpression>(KtWhenExpression::class.java, "Merge with next 'when'", "Merge 'when' expressions") {
    override fun applicabilityRange(element: KtWhenExpression): TextRange? {
        val next = PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace::class.java) as? KtWhenExpression ?: return null

        val subject1 = element.subjectExpression
        val subject2 = next.subjectExpression
        if (!subject1.matches(subject2)) return null
        if (subject1 != null && !subject1.isStable()) return null

        val entries1 = element.entries
        val entries2 = next.entries
        if (entries1.size != entries2.size) return null
        if (!entries1.zip(entries2).all { pair ->
            conditionsMatch(pair.first, pair.second) && checkBodies(pair.first, pair.second)
        }) return null

        return element.whenKeyword.textRange
    }

    private fun conditionsMatch(e1: KtWhenEntry, e2: KtWhenEntry): Boolean =
            e1.conditions.toList().toRange().matches(e2.conditions.toList().toRange())

    private fun checkBodies(e1: KtWhenEntry, e2: KtWhenEntry): Boolean {
        val names1 = e1.declarationNames()
        val names2 = e2.declarationNames()
        if (names1.any { it in names2 }) return false

        return when (e1.expression?.lastBlockStatementOrThis()) {
            is KtReturnExpression, is KtThrowExpression, is KtBreakExpression, is KtContinueExpression -> false
            else -> true
        }
    }

    private fun KtWhenEntry.declarationNames(): Set<String> =
            expression?.blockExpressionsOrSingle()
                    ?.filter { it is KtNamedDeclaration }
                    ?.mapNotNull { it.name }
                    ?.toSet() ?: emptySet()

    override fun applyTo(element: KtWhenExpression, editor: Editor?) {
        val nextWhen = PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace::class.java) as KtWhenExpression
        for ((entry1, entry2) in element.entries.zip(nextWhen.entries)) {
            entry1.expression.mergeWith(entry2.expression)
        }

        element.parent.deleteChildRange(element.nextSibling, nextWhen)
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
