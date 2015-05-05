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
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.canIntroduceSubject
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceSubject
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import java.util.ArrayList

public class IfToWhenIntention : JetSelfTargetingIntention<JetIfExpression>(javaClass(), "Replace 'if' with 'when'") {
    override fun isApplicableTo(element: JetIfExpression, caretOffset: Int): Boolean {
        if (element.getThen() == null) return false
        return element.getIfKeyword().getTextRange().containsOffset(caretOffset)
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val builder = JetPsiFactory(element).WhenBuilder()

        var ifExpression = element
        while (true) {
            val condition = ifExpression.getCondition()
            val orBranches = ArrayList<JetExpression>()
            if (condition != null) {
                orBranches.addOrBranches(condition)
            }
            if (orBranches.isNotEmpty()) {
                orBranches.forEach { builder.condition(it) }
            }
            else {
                builder.condition(null)
            }

            builder.branchExpression(ifExpression.getThen())

            val elseBranch = ifExpression.getElse() ?: break
            if (elseBranch is JetIfExpression) {
                ifExpression = elseBranch
            }
            else {
                builder.elseEntry(elseBranch)
                break
            }
        }

        var whenExpression = builder.toExpression()
        if (whenExpression.canIntroduceSubject()) {
            whenExpression = whenExpression.introduceSubject()
        }

        element.replace(whenExpression)
    }

    private fun MutableList<JetExpression>.addOrBranches(expression: JetExpression): List<JetExpression> {
        if (expression is JetBinaryExpression && expression.getOperationToken() == JetTokens.OROR) {
            val left = expression.getLeft()
            val right = expression.getRight()
            if (left != null && right != null) {
                addOrBranches(left)
                addOrBranches(right)
                return this
            }
        }

        add(JetPsiUtil.safeDeparenthesize(expression))
        return this
    }
}
