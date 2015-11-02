/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.conversion.copy.end
import org.jetbrains.kotlin.idea.conversion.copy.start
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatchStatus
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class AddNameToArgumentIntention
  : SelfTargetingIntention<KtValueArgument>(javaClass(), "Add name to argument"), LowPriorityAction {

    override fun isApplicableTo(element: KtValueArgument, caretOffset: Int): Boolean {
        val expression = element.getArgumentExpression() ?: return false
        val name = detectNameToAdd(element) ?: return false

        setText("Add '$name =' to argument")

        if (expression is KtFunctionLiteralExpression) {
            val range = expression.getTextRange()
            return caretOffset == range.start || caretOffset == range.end
        }

        return true
    }

    override fun allowCaretInsideElement(element: PsiElement)
            = element !is KtValueArgumentList && element !is KtContainerNode

    override fun applyTo(element: KtValueArgument, editor: Editor) {
        val name = detectNameToAdd(element)!!
        val newArgument = KtPsiFactory(element).createArgument(element.getArgumentExpression()!!, name, element.getSpreadElement() != null)
        element.replace(newArgument)
    }

    private fun detectNameToAdd(argument: KtValueArgument): Name? {
        if (argument.isNamed()) return null
        if (argument is KtFunctionLiteralArgument) return null

        val argumentList = argument.getParent() as? KtValueArgumentList ?: return null
        if (argument != argumentList.arguments.last { !it.isNamed() }) return null

        val callExpr = argumentList.getParent() as? KtExpression ?: return null
        val resolvedCall = callExpr.getResolvedCall(callExpr.analyze(BodyResolveMode.PARTIAL)) ?: return null
        val argumentMatch = resolvedCall.getArgumentMapping(argument) as? ArgumentMatch ?: return null
        if (argumentMatch.status != ArgumentMatchStatus.SUCCESS) return null

        if (!resolvedCall.getResultingDescriptor().hasStableParameterNames()) return null

        if (argumentMatch.valueParameter.varargElementType != null) {
            val varargArgument = resolvedCall.getValueArguments()[argumentMatch.valueParameter] as? VarargValueArgument ?: return null
            if (varargArgument.getArguments().size() != 1) return null
        }

        return argumentMatch.valueParameter.getName()
    }
}