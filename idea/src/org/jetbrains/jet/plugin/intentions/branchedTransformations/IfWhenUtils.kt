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

import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lexer.JetTokens
import java.util.ArrayList
import java.util.Collections
import org.jetbrains.jet.lang.psi.JetPsiUnparsingUtils.*

public fun JetIfExpression.canTransformToWhen(): Boolean = getThen() != null

public fun JetWhenExpression.canTransformToIf(): Boolean = !getEntries().isEmpty()

public fun JetIfExpression.transformToWhen() {
    fun JetExpression.splitToOrBranches(): List<JetExpression> {
        val branches = ArrayList<JetExpression>()
        accept(
                object : JetVisitorVoid() {
                    public override fun visitBinaryExpression(expression: JetBinaryExpression) {
                        if (expression.getOperationToken() == JetTokens.OROR) {
                            expression.getLeft()?.accept(this)
                            expression.getRight()?.accept(this)
                        }
                        else {
                            visitExpression(expression)
                        }
                    }

                    public override fun visitParenthesizedExpression(expression: JetParenthesizedExpression) {
                        expression.getExpression()?.accept(this)
                    }

                    public override fun visitExpression(expression: JetExpression) {
                        branches.add(expression)
                    }
                }
        )
        return branches
    }

    fun branchIterator(ifExpression: JetIfExpression): Iterator<JetIfExpression> = object: Iterator<JetIfExpression> {
        private var expression: JetIfExpression? = ifExpression

        override fun next(): JetIfExpression {
            val current = expression!!
            expression = current.getElse()?.let { next -> if (next is JetIfExpression) next else null }
            return current
        }

        override fun hasNext(): Boolean = expression != null
    }

    val builder = JetPsiFactory.WhenBuilder()
    branchIterator(this).forEach { ifExpression ->
        ifExpression.getCondition()?.let { condition ->
            val orBranches = condition.splitToOrBranches()
            if (orBranches.isEmpty()) {
                builder.condition("")
            }
            else {
                orBranches.forEach { branch -> builder.condition(branch) }
            }
        }

        builder.branchExpression(ifExpression.getThen())

        ifExpression.getElse()?.let { elseBranch ->
            if (elseBranch !is JetIfExpression) {
                builder.elseEntry(elseBranch)
            }
        }
    }

    val whenExpression = builder.toExpression(getProject()).let { whenExpression ->
        if (whenExpression.canIntroduceSubject()) whenExpression.introduceSubject() else whenExpression
    }
    replace(whenExpression)
}

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

    val builder = JetPsiFactory.IfChainBuilder()

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

    replace(builder.toExpression(getProject()))
}
