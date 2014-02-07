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
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetFile

public class GetToArrayAccessIntention : JetSelfTargetingIntention<JetCallExpression> ("get.to.array.access", javaClass()) {
    override fun isApplicableTo(element: JetCallExpression): Boolean {
        val parent = element.getParent()
        val args = element.getValueArguments()
        val context = AnalyzerFacadeWithCache.analyzeFileWithCache(element.getContainingFile() as JetFile).getBindingContext()
        val resolvedCall = context[BindingContext.RESOLVED_CALL, element.getCalleeExpression()]
        if (resolvedCall == null) return false
        if (args.any({arg -> arg?.getArgumentName() != null})) {
            return false;
        }
        if (parent is JetDotQualifiedExpression && !element.getValueArguments().isEmpty()) {
           val expression = element.getCalleeExpression();
           if (expression != null) {
               return expression.getText() == "get"
           }
        }
        return false;
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        // if isApplicable returned true so we know the parent is a JetDotQualifiedExpression
        val parent = element.getParent() as JetDotQualifiedExpression
        val literals = element.getFunctionLiteralArguments()
        val arguments = element.getValueArgumentList()?.getText()
        if (arguments != null) {
            var insideBrakcets = if (literals.size > 0) {
                "${arguments.substring(1, arguments.length - 1)}, ${literals[0].getText()}"
            } else {
                "${arguments.substring(1, arguments.length - 1)}"
            }
            val arrayName = parent.getReceiverExpression().getText()
            val arrayAccess = JetPsiFactory.createExpression(element.getProject(), "${arrayName}[${insideBrakcets}]");
            val startOffset = parent.getTextRange()?.getStartOffset();;
            val bracketIndex = arrayAccess.getText()?.indexOf("]");
            if(startOffset != null && bracketIndex != null) {
                editor.getCaretModel().moveToOffset(startOffset + bracketIndex + 1);
            }
            parent.replace(arrayAccess);
        }
    }
}

// alternative way to build the literal arguments, the problem with this one is that it doesn't
// keep the spacing the way it was in the original text.
//
//  for (arg in element.getFunctionLiteralArguments()) {
//       literals = "${literals} ${arg.getText()}"
//  }