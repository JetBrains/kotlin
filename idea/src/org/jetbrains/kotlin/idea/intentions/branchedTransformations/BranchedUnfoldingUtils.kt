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

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.intentions.declarations.DeclarationUtils
import org.jetbrains.kotlin.psi.*

public object BranchedUnfoldingUtils {

    private fun getOutermostLastBlockElement(expression: JetExpression?): JetExpression {
        return JetPsiUtil.getOutermostLastBlockElement(expression, JetPsiUtil.ANY_JET_ELEMENT) as JetExpression
    }

    public fun getUnfoldableExpressionKind(root: JetExpression?): UnfoldableKind? {
        if (root == null) return null

        if (JetPsiUtil.isAssignment(root)) {
            val assignment = root as JetBinaryExpression

            if (assignment.getLeft() == null) return null

            val rhs = assignment.getRight()
            if (rhs is JetIfExpression) return UnfoldableKind.ASSIGNMENT_TO_IF
            if (rhs is JetWhenExpression && JetPsiUtil.checkWhenExpressionHasSingleElse(rhs)) {
                return UnfoldableKind.ASSIGNMENT_TO_WHEN
            }
        }
        else if (root is JetReturnExpression) {
            val resultExpr = root.getReturnedExpression()

            if (resultExpr is JetIfExpression) return UnfoldableKind.RETURN_TO_IF
            if (resultExpr is JetWhenExpression && JetPsiUtil.checkWhenExpressionHasSingleElse(resultExpr)) {
                return UnfoldableKind.RETURN_TO_WHEN
            }
        }
        else if (root is JetProperty) {
            if (!root.isLocal()) return null

            val initializer = root.getInitializer()

            if (initializer is JetIfExpression) return UnfoldableKind.PROPERTY_TO_IF
            if (initializer is JetWhenExpression && JetPsiUtil.checkWhenExpressionHasSingleElse(initializer)) {
                return UnfoldableKind.PROPERTY_TO_WHEN
            }
        }

        return null
    }

    public val UNFOLD_WITHOUT_CHECK: String = "Expression must be checked before unfolding"

    private fun assertNotNull(value: Any?) {
        assert(value != null) { UNFOLD_WITHOUT_CHECK }
    }

    public fun unfoldAssignmentToIf(assignment: JetBinaryExpression, editor: Editor) {
        val op = assignment.getOperationReference().getText()
        val lhs = assignment.getLeft()
        val ifExpression = assignment.getRight() as JetIfExpression

        assertNotNull(ifExpression)

        //noinspection ConstantConditions
        val newIfExpression = ifExpression.copy() as JetIfExpression

        val thenExpr = getOutermostLastBlockElement(newIfExpression.getThen())
        val elseExpr = getOutermostLastBlockElement(newIfExpression.getElse())

        assertNotNull(thenExpr)
        assertNotNull(elseExpr)

        //noinspection ConstantConditions
        val psiFactory = JetPsiFactory(assignment)
        thenExpr.replace(psiFactory.createExpressionByPattern("$0 $1 $2", lhs, op, thenExpr))
        elseExpr.replace(psiFactory.createExpressionByPattern("$0 $1 $2", lhs, op, elseExpr))

        val resultElement = assignment.replace(newIfExpression)

        editor.getCaretModel().moveToOffset(resultElement.getTextOffset())
    }

    public fun unfoldAssignmentToWhen(assignment: JetBinaryExpression, editor: Editor) {
        val op = assignment.getOperationReference().getText()
        val lhs = assignment.getLeft()
        val whenExpression = assignment.getRight() as JetWhenExpression

        assertNotNull(whenExpression)

        //noinspection ConstantConditions
        val newWhenExpression = whenExpression.copy() as JetWhenExpression

        for (entry in newWhenExpression.getEntries()) {
            val currExpr = getOutermostLastBlockElement(entry.getExpression())

            assertNotNull(currExpr)

            //noinspection ConstantConditions
            currExpr.replace(JetPsiFactory(assignment).createExpressionByPattern("$0 $1 $2", lhs, op, currExpr))
        }

        val resultElement = assignment.replace(newWhenExpression)

        editor.getCaretModel().moveToOffset(resultElement.getTextOffset())
    }

    public fun unfoldPropertyToIf(property: JetProperty, editor: Editor) {
        val assignment = DeclarationUtils.splitPropertyDeclaration(property)
        unfoldAssignmentToIf(assignment, editor)
    }

    public fun unfoldPropertyToWhen(property: JetProperty, editor: Editor) {
        val assignment = DeclarationUtils.splitPropertyDeclaration(property)
        unfoldAssignmentToWhen(assignment, editor)
    }

    public fun unfoldReturnToIf(returnExpression: JetReturnExpression) {
        val ifExpression = returnExpression.getReturnedExpression() as JetIfExpression

        assertNotNull(ifExpression)

        //noinspection ConstantConditions
        val newIfExpression = ifExpression.copy() as JetIfExpression

        val thenExpr = getOutermostLastBlockElement(newIfExpression.getThen())
        val elseExpr = getOutermostLastBlockElement(newIfExpression.getElse())

        assertNotNull(thenExpr)
        assertNotNull(elseExpr)

        val psiFactory = JetPsiFactory(returnExpression)
        thenExpr.replace(psiFactory.createExpressionByPattern("return $0", thenExpr))
        elseExpr.replace(psiFactory.createExpressionByPattern("return $0", elseExpr))

        returnExpression.replace(newIfExpression)
    }

    public fun unfoldReturnToWhen(returnExpression: JetReturnExpression) {
        val whenExpression = returnExpression.getReturnedExpression() as JetWhenExpression

        assertNotNull(whenExpression)

        //noinspection ConstantConditions
        val newWhenExpression = whenExpression.copy() as JetWhenExpression

        for (entry in newWhenExpression.getEntries()) {
            val currExpr = getOutermostLastBlockElement(entry.getExpression())

            assertNotNull(currExpr)

            currExpr.replace(JetPsiFactory(returnExpression).createExpressionByPattern("return $0", currExpr))
        }

        returnExpression.replace(newWhenExpression)
    }
}
