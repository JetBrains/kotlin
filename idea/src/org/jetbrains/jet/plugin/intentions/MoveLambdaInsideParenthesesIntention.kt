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
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.plugin.caches.resolve.getBindingContext
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getResolvedCall

public class MoveLambdaInsideParenthesesIntention : JetSelfTargetingIntention<JetCallExpression>(
        "move.lambda.inside.parentheses", javaClass()) {

    override fun isApplicableTo(element: JetCallExpression): Boolean = !element.getFunctionLiteralArguments().isEmpty()

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val typeArgs = element.getTypeArgumentList()?.getText()
        val exprText = element.getCalleeExpression()?.getText()
        if (exprText == null) return
        val funName = if (!element.getTypeArguments().isEmpty() && typeArgs != null) "$exprText$typeArgs" else "$exprText"
        val sb = StringBuilder()
        sb.append("(")
        for (value in element.getValueArguments()) {
            if (value == null) continue
            if (value.getArgumentName() != null) {
                sb.append("${value.getArgumentName()?.getText()} = ${value.getArgumentExpression()?.getText()},")
            } else {
                sb.append("${value.getArgumentExpression()?.getText()},")
            }
        }
        if (element.getValueArguments().any { it?.getArgumentName() != null}) {
            val context = element.getBindingContext()
            val resolvedCall = element.getResolvedCall(context)
            val literalName = resolvedCall?.getResultingDescriptor()?.getValueParameters()?.last?.getName().toString()
            sb.append("$literalName = ")
        }
        val newExpression = "$funName${sb.toString()}${element.getFunctionLiteralArguments()[0].getText()})"
        element.replace(JetPsiFactory(element).createExpression(newExpression))
    }
}