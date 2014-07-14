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

package org.jetbrains.jet.plugin.intentions.attributeCallReplacements

import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lexer.JetTokens
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression

public open class ReplaceContainsIntention : AttributeCallReplacementIntention("replace.contains.with.in") {

    override fun isApplicableToCall(call: CallDescription): Boolean {
        return call.functionName == "contains" && call.argumentCount == 1 && !call.hasEmptyArguments
    }

    override fun replaceCall(call: CallDescription, editor: Editor) {
        val ret = call.resolved.getResultingDescriptor().getReturnType()
            ?: return intentionFailed(editor, "undefined.returntype")

        if (!JetTypeChecker.DEFAULT.isSubtypeOf(ret, KotlinBuiltIns.getInstance().getBooleanType())) {
            return intentionFailed(editor, "contains.returns.boolean")
        }

        val argument = (handleErrors(editor, call.getPositionalArguments()) ?: return)[0].getArgumentExpression()

        // Append semicolon to previous statement if needed
        val psiFactory = JetPsiFactory(call.element)
        if (argument is JetFunctionLiteralExpression) {
            val previousElement = JetPsiUtil.skipSiblingsBackwardByPredicate(call.element) {
                // I checked, it can't be null.
                it!!.getNode()?.getElementType() in JetTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
            }
            if (previousElement != null && previousElement is JetExpression) {
                // If the parent is null, something is very wrong.
                previousElement.getParent()!!.addAfter(psiFactory.createSemicolon(), previousElement)
            }
        }

        call.element.replace(psiFactory.createBinaryExpression(
                argument,
                "in",
                call.element.getReceiverExpression()
        ))
    }
}
