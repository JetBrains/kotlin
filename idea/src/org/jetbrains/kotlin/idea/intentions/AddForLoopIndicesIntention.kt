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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils

public class AddForLoopIndicesIntention : JetSelfTargetingIntention<JetForExpression>(
        javaClass(), "Add indices to 'for' loop") {
    override fun applyTo(element: JetForExpression, editor: Editor) {
        val loopRange = element.getLoopRange()!!
        val newRangeText = "${loopRange.getText()}.withIndex()"
        val project = editor.getProject()!!
        val psiFactory = JetPsiFactory(project)
        val newRange = psiFactory.createExpression(newRangeText)

        //Roundabout way to create new multiparameter element so as not to incorrectly trigger syntax error highlighting
        val loopParameter = element.getLoopParameter()!!
        val parenthesizedParam = psiFactory.createExpression("(index)") as JetParenthesizedExpression
        val indexElement = parenthesizedParam.getExpression()!!
        val comma = psiFactory.createComma()
        val newParamElement = psiFactory.createExpression(loopParameter.getText())
        parenthesizedParam.addAfter(newParamElement, indexElement)
        parenthesizedParam.addAfter(comma, indexElement)

        loopParameter.replace(parenthesizedParam)
        loopRange.replace(newRange)

        val multiParameter = PsiTreeUtil.findChildOfType(element, indexElement.javaClass)!!

        editor.getCaretModel().moveToOffset(multiParameter.getTextOffset())
        val templateBuilder = TemplateBuilderImpl(multiParameter)
        templateBuilder.replaceElement(multiParameter, "index")
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument())
        TemplateManager.getInstance(project).startTemplate(editor, templateBuilder.buildInlineTemplate()!!)
    }

    override fun isApplicableTo(element: JetForExpression, caretOffset: Int): Boolean {
        if (element.getLoopParameter() == null) return false
        val body = element.getBody()
        if (body != null && caretOffset >= body.getTextRange().getStartOffset()) return false

        val range = element.getLoopRange() ?: return false
        if (range is JetDotQualifiedExpression) {
            val selector = range.getSelectorExpression() ?: return true
            if (selector.getText() == "withIndex()") return false
        }

        val psiFactory = JetPsiFactory(element.getProject())
        val potentialExpression = psiFactory.createExpression("${range.getText()}.withIndex()") as JetDotQualifiedExpression

        val bindingContext = element.analyze()
        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, element] ?: return false
        val functionSelector = potentialExpression.getSelectorExpression() as JetCallExpression
        val updatedContext = potentialExpression.analyzeInContext(scope)
        val call = updatedContext[BindingContext.CALL, functionSelector.getCalleeExpression()] ?: return false
        val callScope = updatedContext[BindingContext.RESOLVED_CALL, call] ?: return false
        val callFqName = DescriptorUtils.getFqNameSafe(callScope.getCandidateDescriptor())
        return callFqName.toString() == "kotlin.withIndex"
    }
}
