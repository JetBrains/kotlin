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

import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.replaced

fun JetWhenCondition.toExpression(subject: JetExpression?): JetExpression {
    val factory = JetPsiFactory(this)
    when (this) {
        is JetWhenConditionIsPattern -> {
            val op = if (isNegated()) "!is" else "is"
            return factory.createExpressionByPattern("$0 $op $1", subject ?: "_", getTypeReference() ?: "")
        }

        is JetWhenConditionInRange -> {
            val op = getOperationReference().getText()
            return factory.createExpressionByPattern("$0 $op $1", subject ?: "_", getRangeExpression() ?: "")
        }

        is JetWhenConditionWithExpression -> {
            return if (subject != null) {
                factory.createExpressionByPattern("$0 == $1", subject, getExpression() ?: "")
            }
            else {
                getExpression()
            }
        }

        else -> throw IllegalArgumentException("Unknown JetWhenCondition type: $this")
    }
}

public fun JetWhenExpression.getSubjectToIntroduce(): JetExpression?  {
    if (getSubjectExpression() != null) return null

    var lastCandidate: JetExpression? = null
    for (entry in getEntries()) {
        val conditions = entry.getConditions()
        if (!entry.isElse() && conditions.isEmpty()) return null

        for (condition in conditions) {
            if (condition !is JetWhenConditionWithExpression) return null

            val candidate = condition.getExpression()?.getWhenConditionSubjectCandidate() as? JetSimpleNameExpression ?: return null

            if (lastCandidate == null) {
                lastCandidate = candidate
            }
            else if (!lastCandidate.matches(candidate)) {
                return null
            }

        }
    }

    return lastCandidate
}

private fun JetExpression?.getWhenConditionSubjectCandidate(): JetExpression? {
    return when(this) {
        is JetIsExpression -> getLeftHandSide()

        is JetBinaryExpression -> {
            val lhs = getLeft()
            val op = getOperationToken()
            when (op) {
                JetTokens.IN_KEYWORD, JetTokens.NOT_IN -> lhs
                JetTokens.EQEQ -> lhs as? JetSimpleNameExpression ?: getRight()
                else -> null
            }

        }

        else -> null
    }
}

public fun JetWhenExpression.introduceSubject(): JetWhenExpression {
    val subject = getSubjectToIntroduce()!!

    val whenExpression = JetPsiFactory(this).buildExpression {
        appendFixedText("when(").appendExpression(subject).appendFixedText("){\n")

        for (entry in getEntries()) {
            val branchExpression = entry.getExpression()

            if (entry.isElse()) {
                appendFixedText("else")
            }
            else {
                for ((i, condition) in entry.getConditions().withIndex()) {
                    if (i > 0) appendFixedText(",")

                    val conditionExpression = (condition as JetWhenConditionWithExpression).getExpression()
                    when (conditionExpression)  {
                        is JetIsExpression -> {
                            if (conditionExpression.isNegated()) {
                                appendFixedText("!")
                            }
                            appendFixedText("is ")
                            appendNonFormattedText(conditionExpression.getTypeReference()?.getText() ?: "")
                        }

                        is JetBinaryExpression -> {
                            val lhs = conditionExpression.getLeft()
                            val rhs = conditionExpression.getRight()
                            val op = conditionExpression.getOperationToken()
                            when (op) {
                                JetTokens.IN_KEYWORD -> appendFixedText("in ").appendExpression(rhs)
                                JetTokens.NOT_IN -> appendFixedText("!in ").appendExpression(rhs)
                                JetTokens.EQEQ -> appendExpression(if (subject.matches(lhs)) rhs else lhs)
                                else -> throw IllegalStateException()
                            }
                        }

                        else -> throw IllegalStateException()
                    }
                }
            }
            appendFixedText("->")

            appendExpression(branchExpression)
            appendFixedText("\n")
        }

        appendFixedText("}")
    } as JetWhenExpression

    return replaced(whenExpression)
}
