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


import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetPsiUtil
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression
import org.jetbrains.jet.lang.psi.ValueArgument
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression

public class GetToSquareBracketsIntention : JetSelfTargetingIntention<JetCallExpression>("get.to.square.brackets", javaClass()) {

    override fun isApplicableTo(element: JetCallExpression): Boolean {
        val expression = element.getCalleeExpression() as? JetSimpleNameExpression
        val arguments = element.getValueArguments()

        if (expression?.getReferencedName() != "get")
            return false

        if (arguments.isEmpty())
            return false

        for (argument in arguments) {
            if (argument?.getArgumentExpression() == null || argument!!.isNamed())
                return false
        }

        return element.getParent() is JetDotQualifiedExpression && element.getTypeArguments().isEmpty() && element.getFunctionLiteralArguments().isEmpty()

    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val parentElement = element.getParent() as JetDotQualifiedExpression
        val parentExpression = parentElement.getReceiverExpression().getText()
        val arguments = element.getValueArguments()
        var expressionString = StringBuilder()

        expressionString.append(parentExpression)
        expressionString.append("[")

        for ((index, argument) in arguments.withIndices()) {
            expressionString.append(argument!!.getArgumentExpression()!!.getText())
            if (index != arguments.lastIndex) {
                expressionString.append(", ")
            }
        }
        expressionString.append("]")

        val newExpression = JetPsiFactory.createExpression(element.getProject(), expressionString.toString())

        parentElement.replace(newExpression)
    }
}
