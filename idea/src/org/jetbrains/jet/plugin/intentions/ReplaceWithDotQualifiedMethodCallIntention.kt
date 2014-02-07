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

package org.jetbrains.jet.plugin.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import com.google.common.collect.ImmutableSet
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lexer.JetToken

public class ReplaceWithDotQualifiedMethodCallIntention : JetSelfTargetingIntention<JetSimpleNameExpression>("replace.with.dot.qualified.method.call.intention", javaClass()) {
    private val NON_APPLICABLE_BINARY_OPERATIONS: ImmutableSet<JetToken>? = ImmutableSet.builder<JetToken>()
            ?.addAll(OperatorConventions.BINARY_OPERATION_NAMES.keySet())
            ?.addAll(OperatorConventions.COMPARISON_OPERATIONS)
            ?.addAll(OperatorConventions.EQUALS_OPERATIONS)
            ?.addAll(OperatorConventions.BOOLEAN_OPERATIONS.keySet())
            ?.build()

    override fun isApplicableTo(element: JetSimpleNameExpression): Boolean {
        val parent = element.getParent()

        return parent is JetBinaryExpression &&
                parent.getLeft() != null &&
                parent.getRight() != null &&
                !(NON_APPLICABLE_BINARY_OPERATIONS?.contains(parent.getOperationToken()) ?: false)
    }

    override fun applyTo(element: JetSimpleNameExpression, editor: Editor) {
        val parent = element.getParent() as JetBinaryExpression
        val receiverText = parent.getLeft()!!.getText()
        val argumentText = parent.getRight()!!.getText()
        val functionName = element.getText()
        val replacementExpressionStringBuilder = StringBuilder("$receiverText.$functionName")

        replacementExpressionStringBuilder.append(
                when (parent.getRight()) {
                    is JetFunctionLiteralExpression -> " $argumentText"
                    is JetParenthesizedExpression -> "$argumentText"
                    else -> "($argumentText)"
                }
        )

        val replacement = JetPsiFactory.createExpression(element.getProject(), replacementExpressionStringBuilder.toString())
        parent.replace(replacement)
    }
}