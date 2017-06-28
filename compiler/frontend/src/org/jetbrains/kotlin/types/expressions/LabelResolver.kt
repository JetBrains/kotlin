/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.expressions

import com.google.common.collect.Sets
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.LABEL_NAME_CLASH
import org.jetbrains.kotlin.diagnostics.Errors.UNRESOLVED_REFERENCE
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.checkReservedYield
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.LABEL_TARGET
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.scopes.utils.getDeclarationsByLabel

object LabelResolver {
    private fun getElementsByLabelName(labelName: Name, labelExpression: KtSimpleNameExpression): Set<KtElement> {
        val elements = Sets.newLinkedHashSet<KtElement>()
        var parent: PsiElement? = labelExpression.parent
        while (parent != null) {
            val name = getLabelNameIfAny(parent)
            if (name != null && name == labelName) {
                elements.add(getExpressionUnderLabel(parent as KtExpression))
            }
            parent = parent.parent
        }
        return elements
    }

    fun getLabelNameIfAny(element: PsiElement): Name? {
        return when (element) {
            is KtLabeledExpression -> element.getLabelNameAsName()
            is KtFunctionLiteral -> getLabelNameIfAny(element.parent)
            is KtLambdaExpression -> getLabelForFunctionalExpression(element)
            is KtNamedFunction -> element.nameAsName ?: getLabelForFunctionalExpression(element)
            else -> null
        }
    }

    private fun getLabelForFunctionalExpression(element: KtExpression): Name? {
        val parent = element.parent
        return when (parent) {
            is KtLabeledExpression -> getLabelNameIfAny(parent)
            is KtBinaryExpression -> parent.operationReference.getReferencedNameAsName()
            else -> getCallerName(element)
        }
    }

    private fun getExpressionUnderLabel(labeledExpression: KtExpression): KtExpression {
        val expression = KtPsiUtil.safeDeparenthesize(labeledExpression)
        return if (expression is KtLambdaExpression) expression.functionLiteral else expression
    }

    private fun getCallerName(expression: KtExpression): Name? {
        val callExpression = getContainingCallExpression(expression) ?: return null
        val calleeExpression = callExpression.calleeExpression as? KtSimpleNameExpression
        return calleeExpression?.getReferencedNameAsName()

    }

    private fun getContainingCallExpression(expression: KtExpression): KtCallExpression? {
        val parent = expression.parent
        if (parent is KtLambdaArgument) {
            // f {}
            val call = parent.parent
            if (call is KtCallExpression) {
                return call
            }
        }

        if (parent is KtValueArgument) {
            // f ({}) or f(p = {}) or f (fun () {})
            val argList = parent.parent ?: return null
            val call = argList.parent
            if (call is KtCallExpression) {
                return call
            }
        }
        return null
    }

    fun resolveControlLabel(expression: KtExpressionWithLabel, context: ResolutionContext<*>): KtElement? {
        val labelElement = expression.getTargetLabel()
        checkReservedYield(labelElement, context.trace)

        val labelName = expression.getLabelNameAsName()
        if (labelElement == null || labelName == null) return null

        return resolveNamedLabel(labelName, labelElement, context.trace) ?: run {
            context.trace.report(UNRESOLVED_REFERENCE.on(labelElement, labelElement))
            null
        }
    }

    private fun resolveNamedLabel(
            labelName: Name,
            labelExpression: KtSimpleNameExpression,
            trace: BindingTrace
    ): KtElement? {
        val list = getElementsByLabelName(labelName, labelExpression)
        if (list.isEmpty()) return null

        if (list.size > 1) {
            trace.report(LABEL_NAME_CLASH.on(labelExpression))
        }

        return list.first().also { trace.record(LABEL_TARGET, labelExpression, it) }
    }

    fun resolveThisOrSuperLabel(
            expression: KtInstanceExpressionWithLabel,
            context: ResolutionContext<*>,
            labelName: Name
    ): LabeledReceiverResolutionResult {
        val referenceExpression = expression.instanceReference
        val targetLabel = expression.getTargetLabel() ?: error(expression)

        val declarationsByLabel = context.scope.getDeclarationsByLabel(labelName)
        val size = declarationsByLabel.size
        when (size) {
            1 -> {
                val declarationDescriptor = declarationsByLabel.single()
                val thisReceiver = when (declarationDescriptor) {
                    is ClassDescriptor -> declarationDescriptor.thisAsReceiverParameter
                    is FunctionDescriptor -> declarationDescriptor.extensionReceiverParameter
                    is PropertyDescriptor -> declarationDescriptor.extensionReceiverParameter
                    else -> throw UnsupportedOperationException("Unsupported descriptor: " + declarationDescriptor) // TODO
                }

                val element = DescriptorToSourceUtils.descriptorToDeclaration(declarationDescriptor)
                              ?: error("No PSI element for descriptor: " + declarationDescriptor)
                context.trace.record(LABEL_TARGET, targetLabel, element)
                context.trace.record(REFERENCE_TARGET, referenceExpression, declarationDescriptor)

                if (declarationDescriptor is ClassDescriptor) {
                    if (!DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, targetLabel, declarationDescriptor)) {
                        return LabeledReceiverResolutionResult.labelResolutionFailed()
                    }
                }

                return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver)
            }
            0 -> {
                val element = resolveNamedLabel(labelName, targetLabel, context.trace)
                val declarationDescriptor = context.trace.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, element]
                if (declarationDescriptor is FunctionDescriptor) {
                    val thisReceiver = declarationDescriptor.extensionReceiverParameter
                    if (thisReceiver != null) {
                        context.trace.record(LABEL_TARGET, targetLabel, element)
                        context.trace.record(REFERENCE_TARGET, referenceExpression, declarationDescriptor)
                    }
                    return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver)
                }
                else {
                    context.trace.report(UNRESOLVED_REFERENCE.on(targetLabel, targetLabel))
                }
            }
            else -> BindingContextUtils.reportAmbiguousLabel(context.trace, targetLabel, declarationsByLabel)
        }
        return LabeledReceiverResolutionResult.labelResolutionFailed()
    }

    class LabeledReceiverResolutionResult private constructor(
            val code: LabeledReceiverResolutionResult.Code,
            private val receiverParameterDescriptor: ReceiverParameterDescriptor?
    ) {
        enum class Code {
            LABEL_RESOLUTION_ERROR,
            NO_THIS,
            SUCCESS
        }

        fun success(): Boolean {
            return code == Code.SUCCESS
        }

        fun getReceiverParameterDescriptor(): ReceiverParameterDescriptor? {
            assert(success()) { "Don't try to obtain the receiver when resolution failed with " + code }
            return receiverParameterDescriptor
        }

        companion object {
            fun labelResolutionSuccess(receiverParameterDescriptor: ReceiverParameterDescriptor?): LabeledReceiverResolutionResult {
                if (receiverParameterDescriptor == null) {
                    return LabeledReceiverResolutionResult(Code.NO_THIS, null)
                }
                return LabeledReceiverResolutionResult(Code.SUCCESS, receiverParameterDescriptor)
            }

            fun labelResolutionFailed(): LabeledReceiverResolutionResult {
                return LabeledReceiverResolutionResult(Code.LABEL_RESOLUTION_ERROR, null)
            }
        }
    }
}