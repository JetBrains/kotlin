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

package org.jetbrains.kotlin.idea.intentions

import org.jetbrains.kotlin.psi.JetForExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetOperationExpression

public class ConvertToForEachFunctionCallIntention : JetSelfTargetingOffsetIndependentIntention<JetForExpression>("convert.to.for.each.function.call.intention", javaClass()) {
    override fun isApplicableTo(element: JetForExpression): Boolean {
        return element.getLoopRange() != null && element.getLoopParameter() != null && element.getBody() != null
    }

    override fun applyTo(element: JetForExpression, editor: Editor) {
        fun buildStatements(statements: List<JetElement>): String {
            return when {
                statements.isEmpty() -> ""
                statements.size() == 1 -> statements[0].getText() ?: throw AssertionError("Statements in ForExpression shouldn't be empty: expressionText = ${element.getText()}")
                else -> statements.fold(StringBuilder(), { acc, h -> acc.append("${h.getText()}\n") }).toString()
            }
        }

        fun buildReplacementBodyText(loopParameter: JetParameter, functionBodyText: String): String {
            return when {
                loopParameter.getTypeReference() != null -> " (${loopParameter.getText()}) -> $functionBodyText"
                else -> "${loopParameter.getText()} -> $functionBodyText"
            }
        }

        fun buildReceiverText(element: JetForExpression): String {
            val loopRange = element.getLoopRange()!!

            return when (loopRange) {
                is JetOperationExpression -> "(${loopRange.getText()})"
                else -> loopRange.getText() ?: throw AssertionError("LoopRange in ForExpression shouldn't be empty: expressionText = ${element.getText()}")
            }
        }

        val body = element.getBody()!!
        val loopParameter = element.getLoopParameter()!!

        val bodyText = buildReplacementBodyText(loopParameter, when (body) {
            is JetBlockExpression -> buildStatements(body.getStatements())
            else -> body.getText() ?: throw AssertionError("Body of ForExpression shouldn't be empty: expressionText = ${element.getText()}")
        })

        element.replace(JetPsiFactory(element).createExpression("${buildReceiverText(element)}.forEach { $bodyText }"))
    }
}
