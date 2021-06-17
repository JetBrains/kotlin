/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.util.end
import org.jetbrains.kotlin.idea.core.util.start
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatchStatus.ARGUMENT_HAS_NO_TYPE
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatchStatus.SUCCESS
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument

class AddNameToArgumentIntention : SelfTargetingIntention<KtValueArgument>(
    KtValueArgument::class.java, KotlinBundle.lazyMessage("add.name.to.argument")
), LowPriorityAction {
    override fun isApplicableTo(element: KtValueArgument, caretOffset: Int): Boolean {
        val expression = element.getArgumentExpression() ?: return false
        val name = detectNameToAdd(
            element,
            shouldBeLastUnnamed = !element.languageVersionSettings.supportsFeature(LanguageFeature.MixedNamedArgumentsInTheirOwnPosition)
        ) ?: return false

        setTextGetter(KotlinBundle.lazyMessage("add.0.to.argument", name))

        if (expression is KtLambdaExpression) {
            val range = expression.textRange
            return caretOffset == range.start || caretOffset == range.end
        }

        return true
    }

    override fun skipProcessingFurtherElementsAfter(element: PsiElement) = element is KtValueArgumentList ||
            element is KtContainerNode ||
            super.skipProcessingFurtherElementsAfter(element)

    override fun applyTo(element: KtValueArgument, editor: Editor?) {
        apply(element)
    }

    companion object {
        fun apply(element: KtValueArgument, givenResolvedCall: ResolvedCall<*>? = null): Boolean {
            val name = detectNameToAdd(element, shouldBeLastUnnamed = false, givenResolvedCall = givenResolvedCall) ?: return false
            val argumentExpression = element.getArgumentExpression() ?: return false

            val prevSibling = element.getPrevSiblingIgnoringWhitespace()
            if (prevSibling is PsiComment && """/\*\s*$name\s*=\s*\*/""".toRegex().matches(prevSibling.text)) {
                prevSibling.delete()
            }

            val newArgument = KtPsiFactory(element).createArgument(argumentExpression, name, element.getSpreadElement() != null)
            element.replace(newArgument)
            return true
        }

        fun detectNameToAdd(argument: KtValueArgument, shouldBeLastUnnamed: Boolean, givenResolvedCall: ResolvedCall<*>? = null): Name? {
            if (argument.isNamed()) return null
            if (argument is KtLambdaArgument) return null

            val argumentList = argument.parent as? KtValueArgumentList ?: return null
            if (shouldBeLastUnnamed && argument != argumentList.arguments.last { !it.isNamed() }) return null

            val callExpr = argumentList.parent as? KtCallElement ?: return null
            val resolvedCall = givenResolvedCall ?: callExpr.resolveToCall() ?: return null
            if (!resolvedCall.candidateDescriptor.hasStableParameterNames()) return null

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
