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

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression

public class ReplaceGetWithBracketsIntention : JetSelfTargetingIntention<JetCallExpression>(
        "replace.get.with.brackets", javaClass()
) {
    override fun isApplicableTo(element: JetCallExpression): Boolean {
        if (element.getParent() as? JetDotQualifiedExpression == null)
            return false // ignore orphan get functions
        val exp : JetExpression = element.getCalleeExpression() ?: return false //If there is no callee expression, we can't use the plugin
        for (arg in element.getValueArguments()){
            val childCount = arg?.asElement()?.getChildren()?.size ?: return false //Make sure there are arguments
            if (childCount > 1) //Make sure only numbers/variables are used as indexes. Named arguments are not valid
                return false
        }
        return (exp.textMatches("get") && !element.getValueArguments().isEmpty()) //Check that function text matches 'get' and we have 1+ args


    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {

        val par = element.getParent() as JetDotQualifiedExpression   //get calling object and function syntax. Unsafe cast should be fine
        val prefix = par.getReceiverExpression().getText()         //get calling object name
        assert(prefix!=null,"Calling object name cannot be null/empty")

        val args = element.getValueArgumentList()?.getText()       //get arguments
        assert(args!=null,"Argument list cannot be null, should be checked in isApplicableTo")

        val trimmedArgs = args!!.substring(1,args.length-1)        //remove leading and trailing parentheses
        val exp = JetPsiFactory.createExpression(element.getProject(),"$prefix[$trimmedArgs]")

        par.replace(exp) //replace element
    }

}
