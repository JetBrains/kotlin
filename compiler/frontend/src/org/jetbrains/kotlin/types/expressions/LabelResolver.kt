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
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageFeature.ContextReceivers
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.checkReservedYield
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.scopes.utils.getDeclarationsByLabel
import org.jetbrains.kotlin.utils.addIfNotNull

object LabelResolver {
    private fun getElementsByLabelName(
        labelName: Name,
        labelExpression: KtSimpleNameExpression,
        classNameLabelsEnabled: Boolean
    ): Pair<LinkedHashSet<KtElement>, KtCallableDeclaration?> {
        val elements = linkedSetOf<KtElement>()
        var typedElement: KtCallableDeclaration? = null
        var parent: PsiElement? = labelExpression.parent
        while (parent != null) {
            val names = getLabelNamesIfAny(parent, classNameLabelsEnabled)
            if (names.contains(labelName)) {
                elements.add(getExpressionUnderLabel(parent as KtExpression))
            } else if (parent is KtCallableDeclaration && typedElement == null) {
                val receiverTypeReference = parent.receiverTypeReference
                val nameForReceiverLabel = receiverTypeReference?.nameForReceiverLabel()
                if (nameForReceiverLabel == labelName.asString()) {
                    typedElement = parent
                }
            }
            parent = if (parent is KtCodeFragment) parent.context else parent.parent
        }
        return elements to typedElement
    }

    fun getLabelNamesIfAny(element: PsiElement, addClassNameLabels: Boolean): List<Name> {
        val result = mutableListOf<Name>()
        when (element) {
            is KtLabeledExpression -> result.addIfNotNull(element.getLabelNameAsName())
            // TODO: Support context receivers in function literals
            is KtFunctionLiteral -> return getLabelNamesIfAny(element.parent, false)
            is KtLambdaExpression -> result.addIfNotNull(getLabelForFunctionalExpression(element))
        }
        val functionOrProperty = when (element) {
            is KtNamedFunction -> {
                result.addIfNotNull(element.nameAsName ?: getLabelForFunctionalExpression(element))
                element
            }
            is KtPropertyAccessor -> element.property
            else -> return result
        }
        if (addClassNameLabels) {
            functionOrProperty.receiverTypeReference?.nameForReceiverLabel()?.let { result.add(Name.identifier(it)) }
            functionOrProperty.contextReceivers
                .mapNotNullTo(result) { it.name()?.let { s -> Name.identifier(s) } }
        }
        return result
    }

    private fun getLabelForFunctionalExpression(element: KtExpression): Name? {
        return when (val parent = element.parent) {
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
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
            checkReservedYield(labelElement, context.trace)
        }

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
        val list = getElementsByLabelName(labelName, labelExpression, classNameLabelsEnabled = false).first
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
        val targetLabelExpression = expression.getTargetLabel() ?: error(expression)

        val scope = context.scope
        val declarationsByLabel = scope.getDeclarationsByLabel(labelName)
        val (elementsByLabel, typedElement) = getElementsByLabelName(
            labelName, targetLabelExpression,
            classNameLabelsEnabled = expression is KtThisExpression && context.languageVersionSettings.supportsFeature(ContextReceivers)
        )
        val trace = context.trace
        when (declarationsByLabel.size) {
            1 -> {
                val declarationDescriptor = declarationsByLabel.single()
                val thisReceiver = when (declarationDescriptor) {
                    is ClassDescriptor -> declarationDescriptor.thisAsReceiverParameter
                    is FunctionDescriptor -> declarationDescriptor.extensionReceiverParameter
                    is PropertyDescriptor -> declarationDescriptor.extensionReceiverParameter
                    else -> throw UnsupportedOperationException("Unsupported descriptor: $declarationDescriptor") // TODO
                }

                val declarationElement = DescriptorToSourceUtils.descriptorToDeclaration(declarationDescriptor)
                    ?: error("No PSI element for descriptor: $declarationDescriptor")
                trace.record(LABEL_TARGET, targetLabelExpression, declarationElement)
                trace.record(REFERENCE_TARGET, referenceExpression, declarationDescriptor)
                val closestElement = elementsByLabel.firstOrNull()
                if (closestElement != null && declarationElement in closestElement.parents) {
                    reportLabelResolveWillChange(
                        trace, targetLabelExpression, declarationElement, closestElement, isForExtensionReceiver = false
                    )
                } else if (typedElement != null && declarationElement in typedElement.parents) {
                    reportLabelResolveWillChange(
                        trace, targetLabelExpression, declarationElement, typedElement, isForExtensionReceiver = true
                    )
                }

                if (declarationDescriptor is ClassDescriptor) {
                    if (!DescriptorResolver.checkHasOuterClassInstance(
                            scope, trace, targetLabelExpression, declarationDescriptor
                        )
                    ) {
                        return LabeledReceiverResolutionResult.labelResolutionFailed()
                    }
                }

                return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver)
            }
            0 -> {
                if (elementsByLabel.size > 1) {
                    trace.report(LABEL_NAME_CLASH.on(targetLabelExpression))
                }
                val element = elementsByLabel.firstOrNull()?.also {
                    trace.record(LABEL_TARGET, targetLabelExpression, it)
                }
                val declarationDescriptor = trace.bindingContext[DECLARATION_TO_DESCRIPTOR, element]
                if (declarationDescriptor is FunctionDescriptor) {
                    val labelNameToReceiverMap = trace.bindingContext[
                            DESCRIPTOR_TO_CONTEXT_RECEIVER_MAP,
                            if (declarationDescriptor is PropertyAccessorDescriptor) declarationDescriptor.correspondingProperty else declarationDescriptor
                    ]
                    val thisReceivers = labelNameToReceiverMap?.get(labelName.identifier)
                    val thisReceiver = when {
                        thisReceivers.isNullOrEmpty() -> declarationDescriptor.extensionReceiverParameter
                        thisReceivers.size == 1 -> thisReceivers.single()
                        else -> {
                            BindingContextUtils.reportAmbiguousLabel(trace, targetLabelExpression, declarationsByLabel)
                            return LabeledReceiverResolutionResult.labelResolutionFailed()
                        }
                    }?.also {
                        trace.record(LABEL_TARGET, targetLabelExpression, element)
                        trace.record(REFERENCE_TARGET, referenceExpression, declarationDescriptor)
                    }
                    return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver)
                } else {
                    trace.report(UNRESOLVED_REFERENCE.on(targetLabelExpression, targetLabelExpression))
                }
            }
            else -> BindingContextUtils.reportAmbiguousLabel(trace, targetLabelExpression, declarationsByLabel)
        }
        return LabeledReceiverResolutionResult.labelResolutionFailed()
    }

    private fun reportLabelResolveWillChange(
        trace: BindingTrace,
        target: KtSimpleNameExpression,
        declarationElement: PsiElement,
        closestElement: KtElement,
        isForExtensionReceiver: Boolean
    ) {
        fun suffix() = if (isForExtensionReceiver) "extension receiver" else "context receiver"

        val closestDescription = when (closestElement) {
            is KtFunctionLiteral -> "anonymous function"
            is KtNamedFunction -> "function ${closestElement.name} ${suffix()}"
            is KtPropertyAccessor -> "property ${closestElement.property.name} ${suffix()}"
            else -> "???"
        }
        val declarationDescription = when (declarationElement) {
            is KtClass -> "class ${declarationElement.name}"
            is KtNamedFunction -> "function ${declarationElement.name}"
            is KtProperty -> "property ${declarationElement.name}"
            is KtNamedDeclaration -> "declaration with name ${declarationElement.name}"
            else -> "unknown declaration"
        }
        trace.report(LABEL_RESOLVE_WILL_CHANGE.on(target, declarationDescription, closestDescription))
    }

    class LabeledReceiverResolutionResult private constructor(
        val code: Code,
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
            assert(success()) { "Don't try to obtain the receiver when resolution failed with $code" }
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