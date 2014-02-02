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
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetValueArgumentList

public class ReplaceGetWithSquareBracketsIntention : JetSelfTargetingIntention<JetCallExpression>(
        "replace.get.with.square.brackets.intention", javaClass()
) {
    override fun isApplicableTo(element: JetCallExpression): Boolean {
        val functionName = element.getCalleeExpression()
        val argList = element.getValueArgumentList()
        val funcLitList = element.getFunctionLiteralArguments()

        if (!(element.getParent() is JetDotQualifiedExpression)) {
            return false
        }
        if (functionName == null) {
            return false
        }
        if (functionName.getText() != "get") {
            return false
        }
        if (element.getTypeArgumentList() != null) {
            return false
        }
        if (argList == null) {
            return funcLitList.isNotEmpty()
        }
        if (argList.getArguments().isNotEmpty() && argList.getRightParenthesis() == null) {
            return false
        }
        if (argList.getArguments().any {
            elt ->
            elt.isNamed()
        }) {
            return false
        }
        if (argList.getArguments().isEmpty() && funcLitList.isEmpty()) {
            return false
        }
        return true
    }

    fun parseArgumentString (argList: JetValueArgumentList?): String {
        if (argList == null) return ""
        val argListText = argList.getText()
        if (argList.getArguments().isNotEmpty()) {
            assert (argListText != null, "get function call parameters cannot be converted to text.")
            assert (argListText!!.length() >= 2 &&
                    argListText[0] == '(' &&
                    argListText[argListText.length() - 1] == ')', "Parameters passed to get function call are not syntactically correct.")
            return argListText.substring(1, argListText.length() - 1)
        } else {
            return ""
        }
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val parent = element.getParent() as JetDotQualifiedExpression
        val receiverText = parent.getReceiverExpression().getText()
        val funcLiteralArgs = element.getFunctionLiteralArguments()
        val funcLiteralArg = if (funcLiteralArgs.isEmpty()) "" else funcLiteralArgs.first().getText()
        val argListText = parseArgumentString(element.getValueArgumentList())
        val arrayAccessExpText = when {
            argListText.isEmpty() && funcLiteralArg.isNotEmpty() -> funcLiteralArg
            argListText.isNotEmpty() && funcLiteralArg.isNotEmpty() -> "$argListText, $funcLiteralArg"
            argListText.isNotEmpty() && !funcLiteralArg.isNotEmpty() -> argListText
            else -> ""
        }
        assert(arrayAccessExpText.isNotEmpty(), "No valid parameters to get function call found")

        val modifiedExpr = JetPsiFactory.createExpression(element.getProject(), "$receiverText[$arrayAccessExpText]")
        parent.replace(modifiedExpr)
    }
}