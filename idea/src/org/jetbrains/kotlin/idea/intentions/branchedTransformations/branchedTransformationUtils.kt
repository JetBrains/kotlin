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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetPsiUnparsingUtils.*
import org.jetbrains.kotlin.psi.psiUtil.*
import java.util.ArrayList
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiWhiteSpace
import java.util.Collections
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches

public val TRANSFORM_WITHOUT_CHECK: String = "Expression must be checked before applying transformation"

fun JetWhenCondition.toExpressionText(subject: JetExpression?): String {
    return when (this) {
        is JetWhenConditionIsPattern -> {
            val op = if (isNegated()) "!is" else "is"
            toBinaryExpression(subject, op, getTypeReference())
        }
        is JetWhenConditionInRange -> {
            toBinaryExpression(subject, getOperationReference()!!.getText()!!, getRangeExpression())
        }
        is JetWhenConditionWithExpression -> {
            val conditionExpression = getExpression()
            if (subject != null) {
                toBinaryExpression(parenthesizeIfNeeded(subject), "==", parenthesizeIfNeeded(conditionExpression))
            }
            else {
                JetPsiUtil.getText(this)
            }
        }
        else -> {
            assert(this is JetWhenConditionWithExpression, TRANSFORM_WITHOUT_CHECK)

            val conditionExpression = (this as JetWhenConditionWithExpression).getExpression()
            if (subject != null) {
                toBinaryExpression(parenthesizeIfNeeded(subject), "==", parenthesizeIfNeeded(conditionExpression))
            }
            else {
                JetPsiUtil.getText(this)
            }
        }
    }
}

public fun JetWhenExpression.canFlatten(): Boolean {
    val subject = getSubjectExpression()
    if (subject != null && subject !is JetSimpleNameExpression) return false

    if (!JetPsiUtil.checkWhenExpressionHasSingleElse(this)) return false

    val elseBranch = getElseExpression()
    if (elseBranch !is JetWhenExpression) return false

    return JetPsiUtil.checkWhenExpressionHasSingleElse(elseBranch) && subject.matches(elseBranch.getSubjectExpression())
}

fun JetWhenExpression.getSubjectCandidate(): JetExpression?  {
    fun JetExpression?.getWhenConditionSubjectCandidate(): JetExpression? {
        return when(this) {
            is JetIsExpression -> getLeftHandSide()
            is JetBinaryExpression -> {
                val lhs = getLeft()
                val op = getOperationToken()
                when (op) {
                    JetTokens.IN_KEYWORD, JetTokens.NOT_IN -> lhs
                    JetTokens.EQEQ -> {
                        if (lhs is JetSimpleNameExpression)
                            lhs
                        else
                            getRight()
                    }
                    else -> null
                }

            }
            else -> null
        }
    }

    if (getSubjectExpression() != null) return null

    var lastCandidate: JetExpression? = null
    for (entry in getEntries()) {
        val conditions = entry.getConditions()
        if (!entry.isElse() && conditions.size == 0) return null

        for (condition in conditions) {
            if (condition !is JetWhenConditionWithExpression) return null

            val currCandidate = condition.getExpression().getWhenConditionSubjectCandidate()
            if (currCandidate !is JetSimpleNameExpression) return null

            if (lastCandidate == null) {
                lastCandidate = currCandidate
            }
            else if (!lastCandidate.matches(currCandidate)) return null

        }
    }

    return lastCandidate
}

public fun JetWhenExpression.canIntroduceSubject(): Boolean {
    return getSubjectCandidate() != null
}

public fun JetWhenExpression.flatten(): JetWhenExpression {
    val subjectExpression = getSubjectExpression()
    val elseBranch = getElseExpression()

    assert(elseBranch is JetWhenExpression, TRANSFORM_WITHOUT_CHECK)

    val nestedWhenExpression = (elseBranch as JetWhenExpression)

    val outerEntries = getEntries()
    val innerEntries = nestedWhenExpression.getEntries()
    val builder = JetPsiFactory(this).WhenBuilder(subjectExpression)
    for (entry in outerEntries) {
        if (entry.isElse())
            continue

        builder.entry(entry)
    }
    for (entry in innerEntries) {
        builder.entry(entry)
    }

    return replaced(builder.toExpression())
}

public fun JetWhenExpression.introduceSubject(): JetWhenExpression {
    val subject = getSubjectCandidate()!!

    val builder = JetPsiFactory(this).WhenBuilder(subject)
    for (entry in getEntries()) {
        val branchExpression = entry.getExpression()
        if (entry.isElse()) {
            builder.elseEntry(branchExpression)
            continue
        }

        for (condition in entry.getConditions()) {
            assert(condition is JetWhenConditionWithExpression, TRANSFORM_WITHOUT_CHECK)

            val conditionExpression = ((condition as JetWhenConditionWithExpression)).getExpression()
            when (conditionExpression)  {
                is JetIsExpression -> {
                    builder.pattern(conditionExpression.getTypeReference(), conditionExpression.isNegated())
                }
                is JetBinaryExpression -> {
                    val lhs = conditionExpression.getLeft()
                    val rhs = conditionExpression.getRight()
                    val op = conditionExpression.getOperationToken()
                    when (op) {
                        JetTokens.IN_KEYWORD -> builder.range(rhs, false)
                        JetTokens.NOT_IN -> builder.range(rhs, true)
                        JetTokens.EQEQ -> builder.condition(if (subject.matches(lhs)) rhs else lhs)
                        else -> assert(false, TRANSFORM_WITHOUT_CHECK)
                    }
                }
                else -> assert(false, TRANSFORM_WITHOUT_CHECK)
            }

        }
        builder.branchExpression(branchExpression)
    }

    return replaced(builder.toExpression())
}

public fun JetWhenExpression.canTransformToIf(): Boolean = !getEntries().isEmpty()

public fun JetWhenExpression.transformToIf() {
    fun combineWhenConditions(conditions: Array<JetWhenCondition>, subject: JetExpression?): String {
        return when (conditions.size) {
            0 -> ""
            1 -> conditions[0].toExpressionText(subject)
            else -> {
                conditions
                        .map { condition -> parenthesizeTextIfNeeded(condition.toExpressionText(subject)) }
                        .makeString(separator = " || ")
            }
        }
    }

    val builder = JetPsiFactory(this).IfChainBuilder()

    for (entry in getEntries()) {
        val branch = entry.getExpression()
        if (entry.isElse()) {
            builder.elseBranch(branch)
        }
        else {
            val branchConditionText = combineWhenConditions(entry.getConditions(), getSubjectExpression())
            builder.ifBranch(branchConditionText, JetPsiUtil.getText(branch))
        }
    }

    replace(builder.toExpression())
}

public fun JetWhenExpression.canMergeWithNext(): Boolean {
    fun checkConditions(e1: JetWhenEntry, e2: JetWhenEntry): Boolean =
            e1.getConditions().toList().toRange().matches(e2.getConditions().toList().toRange())

    fun JetWhenEntry.declarationNames(): Set<String> =
            getExpression()?.blockExpressionsOrSingle()
                    ?.filter { it is JetNamedDeclaration }
                    ?.map { decl -> decl.getName() }
                    ?.filterNotNull()?.toSet() ?: Collections.emptySet<String>()

    fun checkBodies(e1: JetWhenEntry, e2: JetWhenEntry): Boolean {
        if (ContainerUtil.intersects(e1.declarationNames(), e2.declarationNames())) return false

        return when (e1.getExpression()?.outermostLastBlockElement()) {
            is JetReturnExpression, is JetThrowExpression, is JetBreakExpression, is JetContinueExpression -> false
            else -> true
        }
    }

    val sibling = PsiTreeUtil.skipSiblingsForward(this, javaClass<PsiWhiteSpace>())

    if (sibling !is JetWhenExpression) return false
    if (!getSubjectExpression().matches(sibling.getSubjectExpression())) return false

    val entries1 = getEntries()
    val entries2 = sibling.getEntries()
    return entries1.size == entries2.size && (entries1 zip entries2).all { pair ->
        checkConditions(pair.first, pair.second) && checkBodies(pair.first, pair.second)
    }
}

public fun JetWhenExpression.mergeWithNext() {
    fun JetExpression?.mergeWith(that: JetExpression?): JetExpression? = when {
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

    val sibling = PsiTreeUtil.skipSiblingsForward(this, javaClass<PsiWhiteSpace>()) as JetWhenExpression
    for ((entry1, entry2) in getEntries() zip sibling.getEntries()) {
        entry1.getExpression() mergeWith entry2.getExpression()
    }

    getParent()?.deleteChildRange(getNextSibling(), sibling)
}
