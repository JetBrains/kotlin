/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions.branchedTransformations

import com.intellij.psi.tree.IElementType
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.util.JetPsiMatcher
import org.jetbrains.jet.lang.psi.JetPsiUnparsingUtils.*
import org.jetbrains.jet.lang.psi.psiUtil.*

public val TRANSFORM_WITHOUT_CHECK: String = "Expression must be checked before applying transformation"

fun JetWhenCondition.toExpressionText(subject: JetExpression?): String {
    return when (this) {
        is JetWhenConditionIsPattern -> {
            val op = if (isNegated()) "!is" else "is"
            toBinaryExpression(subject, op, getTypeRef())
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

    return JetPsiUtil.checkWhenExpressionHasSingleElse(elseBranch) &&
        JetPsiMatcher.checkElementMatch(subject, elseBranch.getSubjectExpression())
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
            else if (!JetPsiMatcher.checkElementMatch(lastCandidate, currCandidate)) return null

        }
    }

    return lastCandidate
}

public fun JetWhenExpression.canIntroduceSubject(): Boolean {
    return getSubjectCandidate() != null
}

public fun JetWhenExpression.canEliminateSubject(): Boolean {
    return getSubjectExpression() is JetSimpleNameExpression
}

public fun JetWhenExpression.flatten(): JetWhenExpression {
    val subjectExpression = getSubjectExpression()
    val elseBranch = getElseExpression()
    
    assert(elseBranch is JetWhenExpression, TRANSFORM_WITHOUT_CHECK)
    
    val nestedWhenExpression = (elseBranch as JetWhenExpression)
    
    val outerEntries = getEntries()
    val innerEntries = nestedWhenExpression.getEntries()
    val builder = JetPsiFactory.WhenBuilder(subjectExpression)
    for (entry in outerEntries) {
        if (entry.isElse())
            continue

        builder.entry(entry)
    }
    for (entry in innerEntries) {
        builder.entry(entry)
    }

    return replaced(builder.toExpression(getProject()))
}

public fun JetWhenExpression.introduceSubject(): JetWhenExpression {
    val subject = getSubjectCandidate()!!
    
    val builder = JetPsiFactory.WhenBuilder(subject)
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
                    builder.pattern(conditionExpression.getTypeRef(), conditionExpression.isNegated())
                }
                is JetBinaryExpression -> {
                    val lhs = conditionExpression.getLeft()
                    val rhs = conditionExpression.getRight()
                    val op = conditionExpression.getOperationToken()
                    when (op) {
                        JetTokens.IN_KEYWORD -> builder.range(rhs, false)
                        JetTokens.NOT_IN -> builder.range(rhs, true)
                        JetTokens.EQEQ -> {
                            if (JetPsiMatcher.checkElementMatch(subject, lhs)) {
                                builder.condition(rhs)
                            }
                            else {
                                builder.condition(lhs)
                            }
                        }
                        else -> assert(false, TRANSFORM_WITHOUT_CHECK)
                    }
                }
                else -> assert(false, TRANSFORM_WITHOUT_CHECK)
            }

        }
        builder.branchExpression(branchExpression)
    }

    return replaced(builder.toExpression(getProject()))
}

public fun JetWhenExpression.eliminateSubject(): JetWhenExpression {
    val subject = getSubjectExpression()!!

    val builder = JetPsiFactory.WhenBuilder()
    for (entry in getEntries()) {
        val branchExpression = entry.getExpression()

        if (entry.isElse()) {
            builder.elseEntry(branchExpression)
            continue
        }
        for (condition in entry.getConditions()) {
            builder.condition(condition.toExpressionText(subject))
        }

        builder.branchExpression(branchExpression)
    }

    return replaced(builder.toExpression(getProject()))
}
