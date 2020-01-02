/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.checker.NewCapturedType

class InsertExplicitTypeArgumentsIntention :
    SelfTargetingRangeIntention<KtCallExpression>(KtCallExpression::class.java, "Add explicit type arguments"), LowPriorityAction {
    override fun applicabilityRange(element: KtCallExpression): TextRange? {
        return if (isApplicableTo(element, element.analyze())) element.calleeExpression?.textRange else null
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) = applyTo(element)

    companion object {
        fun isApplicableTo(element: KtCallElement, bindingContext: BindingContext = element.analyze(BodyResolveMode.PARTIAL)): Boolean {
            if (element.typeArguments.isNotEmpty()) return false
            if (element.calleeExpression == null) return false

            val resolvedCall = element.getResolvedCall(bindingContext) ?: return false
            val typeArgs = resolvedCall.typeArguments
            if (resolvedCall is NewResolvedCallImpl<*>) {
                val valueParameterTypes = resolvedCall.resultingDescriptor.valueParameters.map { it.type }
                if (valueParameterTypes.any { ErrorUtils.containsErrorType(it) }) {
                    return false
                }
            }

            return typeArgs.isNotEmpty() && typeArgs.values
                .none { ErrorUtils.containsErrorType(it) || it is CapturedType || it is NewCapturedType }
        }

        fun applyTo(element: KtCallElement, shortenReferences: Boolean = true) {
            val argumentList = createTypeArguments(element, element.analyze()) ?: return

            val callee = element.calleeExpression ?: return
            val newArgumentList = element.addAfter(argumentList, callee) as KtTypeArgumentList

            if (shortenReferences) {
                ShortenReferences.DEFAULT.process(newArgumentList)
            }
        }

        fun createTypeArguments(element: KtCallElement, bindingContext: BindingContext): KtTypeArgumentList? {
            val resolvedCall = element.getResolvedCall(bindingContext) ?: return null

            val args = resolvedCall.typeArguments
            val types = resolvedCall.candidateDescriptor.typeParameters

            val text = types.joinToString(", ", "<", ">") {
                IdeDescriptorRenderers.SOURCE_CODE.renderType(args.getValue(it))
            }

            return KtPsiFactory(element).createTypeArguments(text)
        }
    }
}
