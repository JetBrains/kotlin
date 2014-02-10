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

public class MoveLambdaInsideParenthesisIntention : JetSelfTargetingIntention<JetCallExpression>(
        "move.lambda.inside.parenthesis", javaClass()) {

    override fun isApplicableTo(element: JetCallExpression): Boolean = !element.getFunctionLiteralArguments().isEmpty()

    // ADD NAMED ARGUMENT SUPPORT - IE IF OTHERS ARE NAMED THEN WHEN MOVING THE LAMBDA INSIDE NAME IT TOO
    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val funName = element.getCalleeExpression()?.getText()
        val args = element.getValueArgumentList()?.getText()
        val literals = element.getFunctionLiteralArguments()[0].getText() // we know the list isn't empty
        if(args == null || funName == null || literals == null) return
        val newExpressionString = if (args.size > 2) { // args.size == 2 when args = () - no parameters
            "$funName${args.substring(0,args.size-1)},$literals)"
        } else {
            "$funName($literals)"
        }
        element.replace(JetPsiFactory.createExpression(element.getProject(),newExpressionString))
    }
}
