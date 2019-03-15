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
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.conversion.copy.end
import org.jetbrains.kotlin.idea.conversion.copy.start
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatchStatus.ARGUMENT_HAS_NO_TYPE
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatchStatus.SUCCESS
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument

class AddNameToArgumentIntention : SelfTargetingIntention<KtValueArgument>(
    KtValueArgument::class.java, "Add name to argument"
), LowPriorityAction {

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

    override fun allowCaretInsideElement(element: PsiElement) =
        element !is KtValueArgumentList && element !is KtContainerNode && super.allowCaretInsideElement(element)

    override fun applyTo(element: KtValueArgument, editor: Editor?) {
        apply(element)
    }

    companion object {
        fun apply(element: KtValueArgument): Boolean {
            val name = detectNameToAdd(element) ?: return false
            val argumentExpression = element.getArgumentExpression() ?: return false
            val newArgument = KtPsiFactory(element).createArgument(argumentExpression, name, element.getSpreadElement() != null)
            element.replace(newArgument)
            return true
        }

        fun detectNameToAdd(argument: KtValueArgument): Name? {
            if (argument.isNamed()) return null
            if (argument is KtLambdaArgument) return null

            val argumentList = argument.parent as? KtValueArgumentList ?: return null
            if (argument != argumentList.arguments.last { !it.isNamed() }) return null

            val callExpr = argumentList.parent as? KtCallElement ?: return null
            val resolvedCall = callExpr.resolveToCall() ?: return null
            if (!resolvedCall.resultingDescriptor.hasStableParameterNames()) return null

            if (!argumentMatchedAndCouldBeNamedInCall(argument, resolvedCall, callExpr.languageVersionSettings)) return null

            return (resolvedCall.getArgumentMapping(argument) as? ArgumentMatch)?.valueParameter?.name
        }

        fun argumentMatchedAndCouldBeNamedInCall(
            argument: ValueArgument,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            versionSettings: LanguageVersionSettings
        ): Boolean {
            val argumentMatch = resolvedCall.getArgumentMapping(argument) as? ArgumentMatch ?: return false
            if (argumentMatch.status != SUCCESS && argumentMatch.status != ARGUMENT_HAS_NO_TYPE) return false

            if (argumentMatch.valueParameter.varargElementType != null) {
                val varargArgument = resolvedCall.valueArguments[argumentMatch.valueParameter] as? VarargValueArgument ?: return false
                if (varargArgument.arguments.size != 1) return false
                if (versionSettings.supportsFeature(LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm)) {
                    if (argument.getSpreadElement() == null) return false
                }
            }
            return true
        }
    }
}