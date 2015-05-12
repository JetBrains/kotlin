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

package org.jetbrains.kotlin.idea.intentions.attributeCallReplacements

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.functionName
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.expressions.OperatorConventions

public class ReplaceContainsIntention : JetSelfTargetingOffsetIndependentIntention<JetDotQualifiedExpression>(javaClass(), "Replace 'contains' call with 'in' operator") {
    override fun isApplicableTo(element: JetDotQualifiedExpression): Boolean {
        if (element.functionName != OperatorConventions.CONTAINS.asString()) return false
        val resolvedCall = element.toResolvedCall() ?: return false
        if (!resolvedCall.getStatus().isSuccess()) return false
        val argument = resolvedCall.getCall().getValueArguments().singleOrNull() ?: return false
        if ((resolvedCall.getArgumentMapping(argument) as ArgumentMatch).valueParameter.getIndex() != 0) return false

        val target = resolvedCall.getResultingDescriptor()
        val returnType = target.getReturnType() ?: return false
        return target.builtIns.isBooleanOrSubtype(returnType)
    }

    override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {
        val argument = element.callExpression!!.getValueArguments().single().getArgumentExpression()!!
        val receiver = element.getReceiverExpression()

        // Append semicolon to previous statement if needed
        val psiFactory = JetPsiFactory(element)
        if (argument is JetFunctionLiteralExpression) {
            val previousElement = JetPsiUtil.skipSiblingsBackwardByPredicate(element) {
                // I checked, it can't be null.
                it!!.getNode()?.getElementType() in JetTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
            }
            if (previousElement != null && previousElement is JetExpression) {
                // If the parent is null, something is very wrong.
                previousElement.getParent()!!.addAfter(psiFactory.createSemicolon(), previousElement)
            }
        }

        element.replace(psiFactory.createExpressionByPattern("$0 in $1", argument, receiver))
    }
}
