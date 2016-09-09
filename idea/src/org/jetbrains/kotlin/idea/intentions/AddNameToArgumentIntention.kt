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
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatchStatus
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class AddNameToArgumentIntention
  : SelfTargetingIntention<KtValueArgument>(KtValueArgument::class.java, "Add name to argument"), LowPriorityAction {

    override fun isApplicableTo(element: KtValueArgument, caretOffset: Int): Boolean {
        val expression = element.getArgumentExpression() ?: return false
        val name = detectNameToAdd(element) ?: return false

        text = "Add '$name =' to argument"

        if (expression is KtLambdaExpression) {
            val range = expression.textRange
            return caretOffset == range.start || caretOffset == range.end
        }

        return true
    }

    override fun allowCaretInsideElement(element: PsiElement)
            = element !is KtValueArgumentList && element !is KtContainerNode && super.allowCaretInsideElement(element)

    override fun applyTo(element: KtValueArgument, editor: Editor?) {
        val name = detectNameToAdd(element)!!
        val newArgument = KtPsiFactory(element).createArgument(element.getArgumentExpression()!!, name, element.getSpreadElement() != null)
        element.replace(newArgument)
    }

    private fun detectNameToAdd(argument: KtValueArgument): Name? {
        if (argument.isNamed()) return null
        if (argument is KtLambdaArgument) return null

        val argumentList = argument.parent as? KtValueArgumentList ?: return null
        if (argument != argumentList.arguments.last { !it.isNamed() }) return null

        val callExpr = argumentList.parent as? KtCallElement ?: return null
        val resolvedCall = callExpr.getResolvedCall(callExpr.analyze(BodyResolveMode.PARTIAL)) ?: return null
        val argumentMatch = resolvedCall.getArgumentMapping(argument) as? ArgumentMatch ?: return null
        if (argumentMatch.status != ArgumentMatchStatus.SUCCESS) return null

        if (!resolvedCall.resultingDescriptor.hasStableParameterNames()) return null

        if (argumentMatch.valueParameter.varargElementType != null) {
            val varargArgument = resolvedCall.valueArguments[argumentMatch.valueParameter] as? VarargValueArgument ?: return null
            if (varargArgument.arguments.size != 1) return null
        }

        return argumentMatch.valueParameter.name
    }
}