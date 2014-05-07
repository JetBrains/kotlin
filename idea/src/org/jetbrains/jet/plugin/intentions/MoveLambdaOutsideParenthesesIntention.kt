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
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory

public class MoveLambdaOutsideParenthesesIntention : JetSelfTargetingIntention<JetCallExpression>(
        "move.lambda.outside.parentheses", javaClass()) {

    override fun isApplicableTo(element: JetCallExpression): Boolean {
        val args = element.getValueArguments()
        return args.size > 0 && args.last?.getArgumentExpression() is JetFunctionLiteralExpression
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val args = element.getValueArguments()
        val literal = args.last!!.getArgumentExpression()?.getText()
        val calleeText = element.getCalleeExpression()?.getText()
        if (calleeText == null || literal == null) return

        val params =
            args.subList(0, args.size - 1).map {
                val name = it?.getArgumentName()?.getText()
                val arg = it?.getArgumentExpression()?.getText()
                if (name != null) {
                    "$name = $arg"
                } else {
                    "$arg"
                }
            }.makeString(", ", "(", ")")

        val newCall =
            if (params == "()") {
                "$calleeText $literal"
            } else {
                "$calleeText$params $literal"
            }
        element.replace(JetPsiFactory.createExpression(element.getProject(), newCall))
    }
}