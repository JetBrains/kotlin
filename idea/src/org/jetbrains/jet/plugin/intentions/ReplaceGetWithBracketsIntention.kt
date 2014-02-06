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
import com.intellij.psi.util.PsiUtil
import org.jetbrains.jet.lang.psi.ValueArgument
import com.siyeh.ig.junit.BeforeClassOrAfterClassIsPublicStaticVoidNoArgInspection
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import com.intellij.psi.impl.source.tree.PsiErrorElementImpl


public class ReplaceGetWithBracketsIntention : JetSelfTargetingIntention<JetCallExpression>(
        "replace.get.with.brackets", javaClass()
) {
    override fun isApplicableTo(element: JetCallExpression): Boolean {
        if (element.getParent() !is JetDotQualifiedExpression) {
            return false // ignore orphan get functions
        }

        val calleeExpression = element.getCalleeExpression() ?: return false //If there is no callee expression, we can't use the plugin
        val arguments = element.getValueArguments()
        val lastNode = element.getNode().getLastChildNode()?.getLastChildNode()

        if (lastNode is PsiErrorElementImpl) {
            return false
        }


        for (arg in arguments) {
            if (arg?.isNamed() ?: false) { //Named arguments are inapplicable for brackets
                return false
            }
        }

        return calleeExpression.textMatches("get") && !arguments.isEmpty() //Check that function text matches 'get' and we have 1+ args
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val parent = element.getParent() as JetDotQualifiedExpression
        val prefix = parent.getReceiverExpression().getText()
        assert(prefix != null, "Calling object name cannot be null/empty")

        val args = element.getValueArgumentList()?.getText()
        assert(args != null, "Argument list cannot be null, should be checked in isApplicableTo")

        val trimmedArgs = args!!.substring(1, args.length - 1)        //remove leading and trailing parentheses
        val exp = JetPsiFactory.createExpression(element.getProject(), "$prefix[$trimmedArgs]")

        editor.getCaretModel().moveToOffset(element.getTextOffset())

        parent.replace(exp)
    }

}
