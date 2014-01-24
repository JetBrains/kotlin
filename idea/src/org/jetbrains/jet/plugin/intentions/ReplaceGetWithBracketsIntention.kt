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

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.JetNodeTypes
import org.jetbrains.jet.lang.psi.*

public class ReplaceGetWithBracketsIntention() : PsiElementBaseIntentionAction() {

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val callExpr = PsiTreeUtil.getParentOfType(element, javaClass<JetCallExpression>())!!
        val dotExpr = callExpr.getParent() as JetDotQualifiedExpression

        val arrayExpr = (JetPsiFactory.createExpression(project, "x[1]") as JetArrayAccessExpression)
        arrayExpr.getArrayExpression()!!.replace(dotExpr.getReceiverExpression())

        val indices = arrayExpr.getIndicesNode()
        indices.getNode().removeChild(indices.getNode().findChildByType(JetNodeTypes.INTEGER_CONSTANT)!!)

        var iter = callExpr.getValueArgumentList()!!.getFirstChild()!!.getNextSibling()!!

        while (iter != callExpr.getValueArgumentList()!!.getLastChild()) {
            if (iter is JetValueArgument) {
                indices.addBefore((iter as JetValueArgument).getArgumentExpression()!!, indices.getLastChild())
            } else {
                indices.addBefore(iter, indices.getLastChild())
            }
            iter = iter.getNextSibling()!!
        }

        dotExpr.replace(arrayExpr)
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        // Match method call with explicit receiver...
        val callExpr = PsiTreeUtil.getParentOfType(element, javaClass<JetCallExpression>())
        if (callExpr == null || callExpr.getParent() !is JetDotQualifiedExpression) {
            return false
        }

        // ... where the method is 'get'...
        if ((callExpr.getCalleeExpression() as? JetSimpleNameExpression)?.getIdentifier()?.getText() != "get") {
            return false
        }

        // ... and there is at least one non-named argument and no named arguments.
        if (callExpr.getValueArguments().isEmpty() || callExpr.getValueArguments().any { it!!.isNamed() }) {
            return false
        }

        setText("Replace list.get(...) with list[...]")
        return true
    }

    override fun getFamilyName(): String {
        return "Replace list.get(...) with list[...]"
    }
}
