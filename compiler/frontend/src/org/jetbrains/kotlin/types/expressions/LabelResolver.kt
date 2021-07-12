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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors.LABEL_NAME_CLASH
import org.jetbrains.kotlin.diagnostics.Errors.UNRESOLVED_REFERENCE
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.checkReservedYield
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.scopes.utils.getDeclarationsByLabel
import org.jetbrains.kotlin.utils.addIfNotNull

object LabelResolver {
    private fun getElementsByLabelName(
        labelName: Name,
        labelExpression: KtSimpleNameExpression,
        isThisExpression: Boolean
    ): Set<KtElement> {
        val elements = linkedSetOf<KtElement>()
        var parent: PsiElement? = labelExpression.parent
        while (parent != null) {
            val names = getLabelNamesIfAny(parent, isThisExpression)
            if (names.contains(labelName)) {
                elements.add(getExpressionUnderLabel(parent as KtExpression))
            }
            parent = if (parent is KtCodeFragment) parent.context else parent.parent
        }
        return elements
    }

    fun getLabelNamesIfAny(element: PsiElement, addContextReceiverNames: Boolean): List<Name> {
        val result = mutableListOf<Name>()
        when (element) {
            is KtLabeledExpression -> result.addIfNotNull(element.getLabelNameAsName())
            // TODO: Support context receivers in function literals
            is KtFunctionLiteral -> return getLabelNamesIfAny(element.parent, false)
            is KtLambdaExpression -> result.addIfNotNull(getLabelForFunctionalExpression(element))
        }
        val functionOrProperty = when (element) {
            is KtNamedFunction -> element
            is KtPropertyAccessor -> element.property
            else -> return result
        }
        if (addContextReceiverNames) {
            functionOrProperty.contextReceivers
                .mapNotNullTo(result) { it.name()?.let { s -> Name.identifier(s) } }
            functionOrProperty.receiverTypeReference?.nameForReceiverLabel()?.let { result.add(Name.identifier(it)) }
        }
        val name = functionOrProperty.nameAsName ?: getLabelForFunctionalExpression(functionOrProperty)
        result.addIfNotNull(name)
        return result
    }

    private fun getLabelForFunctionalExpression(element: KtExpression): Name? {
        val parent = element.parent
        return when (parent) {
            is KtLabeledExpression -> getLabelNamesIfAny(parent, false).singleOrNull()
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

        return resolveNamedLabel(labelName, labelElement, context.trace, false) ?: run {
            context.trace.report(UNRESOLVED_REFERENCE.on(labelElement, labelElement))
            null
        }
    }

    private fun resolveNamedLabel(
        labelName: Name,
        labelExpression: KtSimpleNameExpression,
        trace: BindingTrace,
        isThisExpression: Boolean
    ): KtElement? {
        val list = getElementsByLabelName(labelName, labelExpression, isThisExpression)
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
                val element = resolveNamedLabel(labelName, targetLabel, context.trace, expression is KtThisExpression)
                val declarationDescriptor = context.trace.bindingContext[DECLARATION_TO_DESCRIPTOR, element]
                if (declarationDescriptor is FunctionDescriptor) {
                    val receiverToLabelMap =
                        context.trace.bindingContext[DESCRIPTOR_TO_NAMED_RECEIVERS, if (declarationDescriptor is PropertyAccessorDescriptor) declarationDescriptor.correspondingProperty else declarationDescriptor]
                    val thisReceiver = receiverToLabelMap?.entries?.find {
                        it.value == labelName.identifier
                    }?.key ?: declarationDescriptor.extensionReceiverParameter
                    if (thisReceiver != null) {
                        context.trace.record(LABEL_TARGET, targetLabel, element)
                        context.trace.record(REFERENCE_TARGET, referenceExpression, declarationDescriptor)
                    }
                    return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver)
                } else {
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