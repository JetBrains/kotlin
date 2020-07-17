/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.source.getPsi

@Suppress("DataClassPrivateConstructor") // we have modifier data to get equals&hashCode only
data class ArgumentSliceProducer private constructor(
    private val parameterIndex: Int,
    private val isExtension: Boolean
) : SliceProducer
{
    constructor(parameterDescriptor: ValueParameterDescriptor) : this(
        parameterDescriptor.index,
        parameterDescriptor.containingDeclaration.isExtension
    )

    override fun produce(usage: UsageInfo, mode: KotlinSliceAnalysisMode, parent: SliceUsage): Collection<SliceUsage>? {
        val element = usage.element ?: return emptyList()
        val argumentExpression = extractArgumentExpression(element) ?: return emptyList()
        return listOf(KotlinSliceUsage(argumentExpression, parent, mode, forcedExpressionMode = true))
    }

    override val testPresentation = "ARGUMENT #$parameterIndex".let { if (isExtension) "$it EXTENSION" else it }

    private fun extractArgumentExpression(refElement: PsiElement): PsiElement? {
        val refParent = refElement.parent
        return when {
            refElement is KtExpression -> {
                val callElement = refElement as? KtCallElement
                    ?: refElement.getParentOfTypeAndBranch { calleeExpression }
                    ?: return null
                val resolvedCall = callElement.resolveToCall() ?: return null
                val resultingDescriptor = resolvedCall.resultingDescriptor
                val parameterIndexToUse = parameterIndex +
                        (if (isExtension && resultingDescriptor.extensionReceiverParameter == null) 1 else 0)
                val parameterDescriptor = resultingDescriptor.valueParameters[parameterIndexToUse]
                val resolvedArgument = resolvedCall.valueArguments[parameterDescriptor] ?: return null
                when (resolvedArgument) {
                    is DefaultValueArgument -> (parameterDescriptor.source.getPsi() as? KtParameter)?.defaultValue
                    is ExpressionValueArgument -> resolvedArgument.valueArgument?.getArgumentExpression()
                    else -> null
                }
            }

            refParent is PsiCall -> refParent.argumentList?.expressions?.getOrNull(parameterIndex + (if (isExtension) 1 else 0))

            refElement is PsiMethod -> refElement.parameterList.parameters.getOrNull(parameterIndex + (if (isExtension) 1 else 0))

            else -> null
        }
    }
}