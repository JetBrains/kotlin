/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.approximateFlexibleTypes
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStartOffsetIn
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit

class ConvertLambdaToReferenceInspection() : IntentionBasedInspection<KtLambdaExpression>(
        ConvertLambdaToReferenceIntention::class,
        { it -> ConvertLambdaToReferenceIntention.shouldSuggestToConvert(it) }
) {
    override fun inspectionTarget(element: KtLambdaExpression) = element.bodyExpression?.statements?.singleOrNull()
}

class ConvertLambdaToReferenceIntention : SelfTargetingOffsetIndependentIntention<KtLambdaExpression>(
        KtLambdaExpression::class.java, "Convert lambda to reference"
) {
    private fun KtLambdaArgument.outerCalleeDescriptor(): FunctionDescriptor? {
        val outerCallExpression = parent as? KtCallExpression ?: return null
        val context = outerCallExpression.analyze()
        val outerCallee = outerCallExpression.calleeExpression as? KtReferenceExpression ?: return null
        return context[REFERENCE_TARGET, outerCallee] as? FunctionDescriptor
    }

    override fun isApplicableTo(element: KtLambdaExpression): Boolean {
        val body = element.bodyExpression ?: return false
        val statement = body.statements.singleOrNull() ?: return false
        val lambdaParent = element.parent
        var lambdaMustReturnUnit = false
        if (lambdaParent is KtLambdaArgument) {
            val outerCalleeDescriptor = lambdaParent.outerCalleeDescriptor() ?: return false
            val lambdaParameterType = outerCalleeDescriptor.valueParameters.lastOrNull()?.type
            if (lambdaParameterType != null && lambdaParameterType.isFunctionType) {
                // For lambda parameter with receiver, conversion is not allowed
                if (lambdaParameterType.isExtensionFunctionType) return false
                // Special Unit case (non-Unit returning lambda is accepted here, but non-Unit returning reference is not)
                lambdaMustReturnUnit = lambdaParameterType.getReturnTypeFromFunctionType().isUnit()
            }
        }
        val context = statement.analyze()

        fun isConvertableCallInLambda(
                callableExpression: KtExpression,
                explicitReceiver: KtExpression? = null,
                lambdaExpression: KtLambdaExpression
        ): Boolean {
            val calleeReferenceExpression = when (callableExpression) {
                is KtCallExpression -> callableExpression.calleeExpression as? KtNameReferenceExpression ?: return false
                is KtNameReferenceExpression -> callableExpression
                else -> return false
            }
            val calleeDescriptor = context[REFERENCE_TARGET, calleeReferenceExpression] as? CallableMemberDescriptor ?: return false
            // No references with type parameters
            if (calleeDescriptor.typeParameters.isNotEmpty()) return false
            // No references to Java synthetic properties
            if (calleeDescriptor is SyntheticJavaPropertyDescriptor) return false
            val descriptorHasReceiver = with(calleeDescriptor) {
                // No references to both member / extension
                if (dispatchReceiverParameter != null && extensionReceiverParameter != null) return false
                dispatchReceiverParameter != null || extensionReceiverParameter != null
            }
            val callHasReceiver = explicitReceiver != null
            if (descriptorHasReceiver != callHasReceiver) return false
            val callableArgumentsCount = if (callableExpression is KtCallExpression) callableExpression.valueArguments.size else 0
            if (calleeDescriptor.valueParameters.size != callableArgumentsCount) return false
            if (lambdaMustReturnUnit) {
                calleeDescriptor.returnType.let {
                    // If Unit required, no references to non-Unit callables
                    if (it == null || !it.isUnit()) return false
                }
            }

            val hasSpecification = lambdaExpression.functionLiteral.hasParameterSpecification()
            val receiverShift = if (callHasReceiver) 1 else 0
            val parametersCount = if (hasSpecification) lambdaExpression.valueParameters.size else 1
            if (parametersCount != callableArgumentsCount + receiverShift) return false
            if (explicitReceiver != null) {
                if (explicitReceiver !is KtNameReferenceExpression) return false
                val callReceiverDescriptor = context[REFERENCE_TARGET, explicitReceiver] as? ParameterDescriptor ?: return false
                val receiverType = callReceiverDescriptor.type
                // No exotic receiver types
                if (receiverType.isTypeParameter() || receiverType.isError || receiverType.isDynamic() ||
                    !receiverType.constructor.isDenotable || receiverType.isFunctionType) return false
                val receiverDeclarationDescriptor = receiverType.constructor.declarationDescriptor
                if (receiverDeclarationDescriptor is ClassDescriptor) {
                    // No references to object members
                    if (receiverDeclarationDescriptor.kind == ClassKind.OBJECT) return false
                    // No invisible receiver types
                    if (!receiverDeclarationDescriptor.isVisible(
                            explicitReceiver, null, context, explicitReceiver.getResolutionFacade()
                    )) return false
                }

                val parameterName = if (hasSpecification) lambdaExpression.valueParameters[0].name else "it"
                if (explicitReceiver.getReferencedName() != parameterName) return false
            }
            // Same lambda / references function parameter order
            if (callableExpression is KtCallExpression) {
                callableExpression.valueArguments.forEachIndexed { i, argument ->
                    val argumentExpression = argument.getArgumentExpression() as? KtNameReferenceExpression ?: return false
                    val parameterName = if (hasSpecification) lambdaExpression.valueParameters[i + receiverShift].name else "it"
                    if (argumentExpression.getReferencedName() != parameterName) return false
                }
            }
            return true
        }

        return when (statement) {
            is KtCallExpression -> {
                isConvertableCallInLambda(callableExpression = statement, lambdaExpression = element)
            }
            is KtNameReferenceExpression -> false // Global property reference is not possible (?!)
            is KtDotQualifiedExpression -> {
                val selector = statement.selectorExpression ?: return false
                isConvertableCallInLambda(callableExpression = selector, explicitReceiver = statement.receiverExpression,
                                          lambdaExpression = element)
            }
            else -> false
        }
    }

    override fun applyTo(element: KtLambdaExpression, editor: Editor?) {
        val body = element.bodyExpression ?: return
        val referenceName = buildReferenceText(body.statements.singleOrNull() ?: return, shortTypes = false) ?: return
        val factory = KtPsiFactory(element)
        val lambdaArgument = element.parent as? KtLambdaArgument
        if (lambdaArgument == null) {
            // Without lambda argument syntax, just replace lambda with reference
            val callableReferenceExpr = factory.createCallableReferenceExpression(referenceName) ?: return
            (element.replace(callableReferenceExpr) as? KtElement)?.let { ShortenReferences.DEFAULT.process(it) }
        }
        else {
            // Otherwise, replace the whole argument list for lambda argument-using call
            val outerCallExpression = lambdaArgument.parent as? KtCallExpression ?: return
            val outerCalleeDescriptor = lambdaArgument.outerCalleeDescriptor() ?: return
            // Parameters with default value
            val valueParameters = outerCalleeDescriptor.valueParameters
            val arguments = outerCallExpression.valueArguments.filter { it !is KtLambdaArgument }
            val useNamedArguments = valueParameters.any { it.hasDefaultValue() } || arguments.any { it.getArgumentName() != null }

            if (useNamedArguments && arguments.size > valueParameters.size) return
            val newArgumentList = factory.buildValueArgumentList {
                appendFixedText("(")
                arguments.forEachIndexed { i, argument ->
                    if (useNamedArguments) {
                        val argumentName = argument.getArgumentName()?.asName
                        val name = argumentName ?: valueParameters[i].name
                        appendName(name)
                        appendFixedText(" = ")
                    }
                    appendExpression(argument.getArgumentExpression())
                    appendFixedText(", ")
                }
                if (useNamedArguments) {
                    appendName(valueParameters.last().name)
                    appendFixedText(" = ")
                }
                appendFixedText(referenceName)
                appendFixedText(")")
            }
            val argumentList = outerCallExpression.valueArgumentList
            if (argumentList == null) {
                (lambdaArgument.replace(newArgumentList) as? KtElement)?.let { ShortenReferences.DEFAULT.process(it) }
            }
            else {
                (argumentList.replace(newArgumentList) as? KtValueArgumentList)?.let {
                    ShortenReferences.DEFAULT.process(it.arguments.last())
                }
                lambdaArgument.delete()
            }
        }
    }

    companion object {
        internal fun shouldSuggestToConvert(element: KtLambdaExpression): Boolean {
            val body = element.bodyExpression ?: return false
            val referenceName = buildReferenceText(body.statements.singleOrNull() ?: return false, shortTypes = true) ?: return false
            return referenceName.length < element.text.length
        }

        private fun buildReferenceText(expression: KtExpression, shortTypes: Boolean): String? {
            return when (expression) {
                is KtCallExpression -> "::${expression.getCallReferencedName()}"
                is KtDotQualifiedExpression -> {
                    val selector = expression.selectorExpression
                    val selectorReferenceName = when (selector) {
                        is KtCallExpression -> selector.getCallReferencedName() ?: return null
                        is KtNameReferenceExpression -> selector.getReferencedName()
                        else -> return null
                    }
                    val receiver = expression.receiverExpression as? KtNameReferenceExpression ?: return null
                    val context = receiver.analyze()
                    val receiverDescriptor = context[REFERENCE_TARGET, receiver] as? ParameterDescriptor ?: return null
                    val originalReceiverType = receiverDescriptor.type
                    val receiverType = originalReceiverType.approximateFlexibleTypes()
                    if (shortTypes) {
                        "${IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(receiverType)}::$selectorReferenceName"
                    }
                    else {
                        "${IdeDescriptorRenderers.SOURCE_CODE.renderType(receiverType)}::$selectorReferenceName"
                    }
                }
                else -> null
            }
        }

        private fun KtCallExpression.getCallReferencedName() = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
    }
}