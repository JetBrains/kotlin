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

package org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions

import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.intentions.branchedTransformations.extractExpressionIfSingle
import org.jetbrains.jet.plugin.intentions.branchedTransformations.comparesNonNullToNull
import org.jetbrains.jet.plugin.intentions.branchedTransformations.getNonNullExpression
import org.jetbrains.jet.plugin.intentions.branchedTransformations.isNullExpressionOrEmptyBlock
import org.jetbrains.jet.plugin.intentions.branchedTransformations.replace
import org.jetbrains.jet.lang.psi.JetSafeQualifiedExpression
import org.jetbrains.jet.plugin.intentions.branchedTransformations.isStableVariable
import org.jetbrains.jet.plugin.intentions.branchedTransformations.inlineReceiverIfApplicableWithPrompt

public class IfThenToSafeAccessIntention : JetSelfTargetingIntention<JetIfExpression>("if.then.to.safe.access", javaClass()) {

    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val condition = element.getCondition()
        val thenClause = element.getThen()
        val elseClause = element.getElse()
        if (condition !is JetBinaryExpression || !condition.comparesNonNullToNull()) return false

        val receiverExpression = condition.getNonNullExpression()
        if (receiverExpression == null || !receiverExpression.isStableVariable()) return false

        return when (condition.getOperationToken()) {
            JetTokens.EQEQ ->
                thenClause?.isNullExpressionOrEmptyBlock() ?: true &&
                elseClause != null && clauseContainsAppropriateDotQualifiedExpression(elseClause, receiverExpression)

            JetTokens.EXCLEQ ->
                elseClause?.isNullExpressionOrEmptyBlock() ?: true &&
                thenClause != null && clauseContainsAppropriateDotQualifiedExpression(thenClause, receiverExpression)

            else ->
                false
        }
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val condition = element.getCondition() as JetBinaryExpression
        val receiverExpression = checkNotNull(condition.getNonNullExpression(), "The receiver expression cannot be null")

        val selectorExpression =
                when(condition.getOperationToken()) {

                    JetTokens.EQEQ -> {
                        val elseClause = checkNotNull(element.getElse(), "The else clause cannot be null")
                        findSelectorExpressionInClause(elseClause, receiverExpression)
                    }

                    JetTokens.EXCLEQ -> {
                        val thenClause = checkNotNull(element.getThen(), "The then clause cannot be null")
                        findSelectorExpressionInClause(thenClause, receiverExpression)
                    }

                    else ->
                        throw IllegalStateException("Operation token must be either null or not null")
                }

        val resultingExprString = "${receiverExpression.getText()}?.${selectorExpression?.getText()}"
        val safeAccessExpr = element.replace(resultingExprString) as JetSafeQualifiedExpression
        safeAccessExpr.inlineReceiverIfApplicableWithPrompt(editor)
    }

    fun clauseContainsAppropriateDotQualifiedExpression(clause: JetExpression, receiverExpression: JetExpression): Boolean =
            findSelectorExpressionInClause(clause, receiverExpression) != null

    fun findSelectorExpressionInClause(clause: JetExpression, receiverExpression: JetExpression): JetExpression? {
        val expression = clause.extractExpressionIfSingle() as? JetDotQualifiedExpression

        if (expression?.getReceiverExpression()?.getText() != receiverExpression.getText()) return null

        return expression?.getSelectorExpression()
    }
}
