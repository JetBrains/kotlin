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

import org.jetbrains.jet.plugin.JetBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.PsiFile
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.psi.PsiElement
import com.intellij.refactoring.changeSignature.PsiCallReference
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression
import com.intellij.psi.PsiArrayAccessExpression
import com.intellij.psi.impl.source.tree.java.PsiArrayAccessExpressionImpl
import org.jetbrains.jet.lang.psi.JetVisitorVoid
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.plugin.quickfix.JetIntentionAction

public class GetCallReplacementIntention : JetSelfTargetingIntention<JetDotQualifiedExpression>("get.call.replacement", javaClass()) {
    var project : Project? = null

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        this.project = project
        super<JetSelfTargetingIntention>.invoke(project, editor, file)
    }

    override fun isApplicableTo(element: JetDotQualifiedExpression): Boolean {
        val selector : JetCallExpression? = element.getSelectorExpression() as? JetCallExpression
        val callee : JetExpression? = selector?.getCalleeExpression()

        return callee != null && callee.textMatches("get")
    }

    override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {
        val selector : JetCallExpression? = element.getSelectorExpression() as? JetCallExpression
        val callee : JetExpression? = selector?.getCalleeExpression()
        val arguments = selector?.getValueArgumentList()
        assert(arguments != null)
        val argumentsText = arguments!!.getText()
        assert(argumentsText != null)
        val receiver : JetExpression = element.getReceiverExpression()
        var arrayArgumentsText = ""

        for (i in 0..argumentsText!!.length() - 1) {
            when (i) {
                0 -> arrayArgumentsText += "["
                argumentsText.length() - 1 -> arrayArgumentsText += "]"
                else -> arrayArgumentsText += argumentsText[i]
            }
        }

        if (project == null) {
            throw UnsupportedOperationException("The project is unspecified.")
        } else {
            val replacement = JetPsiFactory.createExpression(project, receiver.getText() + arrayArgumentsText)
            element.replace(replacement)
        }
    }
}