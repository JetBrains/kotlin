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

import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetForExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.analyzer.analyzeInContext
import org.jetbrains.jet.lang.psi.JetCallExpression

public class AddForLoopIndicesIntention : JetSelfTargetingIntention<JetForExpression>(
        "add.for.loop.indices", javaClass()) {
    override fun applyTo(element: JetForExpression, editor: Editor) {
        val loopRange = element.getLoopRange()!!
        val newRangeText = "${loopRange.getText()}.withIndices()"
        val newRange = JetPsiFactory.createExpression(editor.getProject(), newRangeText)

        //Roundabout way to create new multiparameter element so as not to incorrectly trigger syntax error highlighting
        val loopParameter = element.getLoopParameter()!!
        val parenthesizedParam = JetPsiFactory.createExpression(editor.getProject(), "(index)") as JetParenthesizedExpression
        val indexElement = parenthesizedParam.getExpression()!!
        val comma = JetPsiFactory.createComma(editor.getProject())
        val newParamElement = JetPsiFactory.createExpression(editor.getProject(), " ${loopParameter.getText()}")
        parenthesizedParam.addAfter(newParamElement, indexElement)
        parenthesizedParam.addAfter(comma, indexElement)

        loopParameter.replace(parenthesizedParam)
        loopRange.replace(newRange)

        val multiParameter = PsiTreeUtil.findChildOfType(element, indexElement.javaClass)!!

        editor.getCaretModel().moveToOffset(multiParameter.getTextOffset())
        val templateBuilder = TemplateBuilderImpl(multiParameter)
        templateBuilder.replaceElement(multiParameter, "index")
        val manager = TemplateManagerImpl(editor.getProject())
        PsiDocumentManager.getInstance(editor.getProject()!!).doPostponedOperationsAndUnblockDocument(editor.getDocument())
        manager.startTemplate(editor, templateBuilder.buildInlineTemplate()!!)
    }

    override fun isApplicableTo(element: JetForExpression): Boolean {
        if (element.getLoopParameter() == null) return false
        val range = element.getLoopRange() ?: return false
        if (range is JetDotQualifiedExpression) {
            val selector = range.getSelectorExpression() ?: return true
            if (selector.getText() == "withIndices()") return false
        }

        val potentialExpression = JetPsiFactory.createExpression(element.getProject(),
                                                                 "${range.getText()}.withIndices()") as JetDotQualifiedExpression

        val bindingContext = AnalyzerFacadeWithCache.getContextForElement(element)
        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, element] ?: return false
        val functionSelector = potentialExpression.getSelectorExpression() as JetCallExpression
        val updatedContext = potentialExpression.analyzeInContext(scope)
        val callScope = updatedContext[BindingContext.RESOLVED_CALL, functionSelector.getCalleeExpression()!!] ?: return false
        val callFqName = DescriptorUtils.getFqNameSafe(callScope.getCandidateDescriptor())
        return callFqName.toString() == "kotlin.withIndices"
    }

}