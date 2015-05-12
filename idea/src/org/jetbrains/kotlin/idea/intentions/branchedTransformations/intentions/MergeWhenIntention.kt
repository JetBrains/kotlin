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
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStableVariable
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

public class MergeWhenIntention : JetSelfTargetingRangeIntention<JetWhenExpression>(javaClass(), "Merge with next 'when'", "Merge 'when' expressions") {
    override fun applicabilityRange(element: JetWhenExpression): TextRange? {
        val next = PsiTreeUtil.skipSiblingsForward(element, javaClass<PsiWhiteSpace>()) as? JetWhenExpression ?: return null

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

    private fun conditionsMatch(e1: JetWhenEntry, e2: JetWhenEntry): Boolean =
            e1.getConditions().toList().toRange().matches(e2.getConditions().toList().toRange())

    private fun checkBodies(e1: JetWhenEntry, e2: JetWhenEntry): Boolean {
        val names1 = e1.declarationNames()
        val names2 = e2.declarationNames()
        if (names1.any { it in names2 }) return false

        return when (e1.getExpression()?.lastBlockStatementOrThis()) {
            is JetReturnExpression, is JetThrowExpression, is JetBreakExpression, is JetContinueExpression -> false
            else -> true
        }
    }

    private fun JetWhenEntry.declarationNames(): Set<String> =
            getExpression()?.blockExpressionsOrSingle()
                    ?.filter { it is JetNamedDeclaration }
                    ?.map { it.getName() }
                    ?.filterNotNull()?.toSet() ?: emptySet()

    override fun applyTo(element: JetWhenExpression, editor: Editor) {
        val nextWhen = PsiTreeUtil.skipSiblingsForward(element, javaClass<PsiWhiteSpace>()) as JetWhenExpression
        for ((entry1, entry2) in element.getEntries().zip(nextWhen.getEntries())) {
            entry1.getExpression().mergeWith(entry2.getExpression())
        }

        element.getParent().deleteChildRange(element.getNextSibling(), nextWhen)
    }

    private fun JetExpression?.mergeWith(that: JetExpression?): JetExpression? = when {
        this == null -> that
        that == null -> this
        else -> {
            val block = if (this is JetBlockExpression) this else replaced(wrapInBlock())
            for (element in that.blockExpressionsOrSingle()) {
                val expression = block.appendElement(element)
                block.addBefore(JetPsiFactory(this).createNewLine(), expression)
            }
            block
        }
    }
}
