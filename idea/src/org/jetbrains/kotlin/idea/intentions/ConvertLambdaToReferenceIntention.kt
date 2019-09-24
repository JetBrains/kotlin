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
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.approximateFlexibleTypes
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext.FUNCTION
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit

@Suppress("DEPRECATION")
class ConvertLambdaToReferenceInspection : IntentionBasedInspection<KtLambdaExpression>(ConvertLambdaToReferenceIntention::class)

open class ConvertLambdaToReferenceIntention(text: String) :
    SelfTargetingOffsetIndependentIntention<KtLambdaExpression>(KtLambdaExpression::class.java, text) {

    @Suppress("unused")
    constructor() : this("Convert lambda to reference")

    open fun buildReferenceText(element: KtLambdaExpression) = buildReferenceText(lambdaExpression = element, shortTypes = false)

    private fun KtLambdaArgument.outerCalleeDescriptor(): FunctionDescriptor? {
        val outerCallExpression = parent as? KtCallExpression ?: return null
        return outerCallExpression.resolveToCall()?.resultingDescriptor as? FunctionDescriptor
    }

    private fun isConvertibleCallInLambda(
        callableExpression: KtExpression,
        explicitReceiver: KtExpression? = null,
        lambdaExpression: KtLambdaExpression,
        lambdaMustReturnUnit: Boolean
    ): Boolean {
        val context = callableExpression.analyze()
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
        // No suspend functions
        if ((calleeDescriptor as? FunctionDescriptor)?.isSuspend == true) return false

        val descriptorHasReceiver = with(calleeDescriptor) {
            // No references to both member / extension
            if (dispatchReceiverParameter != null && extensionReceiverParameter != null) return false
            dispatchReceiverParameter != null || extensionReceiverParameter != null
        }

        if (!descriptorHasReceiver && explicitReceiver != null) return false
        val noBoundReferences = !callableExpression.languageVersionSettings.supportsFeature(LanguageFeature.BoundCallableReferences)
        if (noBoundReferences && descriptorHasReceiver && explicitReceiver == null) return false

        val callableArgumentsCount = (callableExpression as? KtCallExpression)?.valueArguments?.size ?: 0
        if (calleeDescriptor.valueParameters.size != callableArgumentsCount) return false
        if (lambdaMustReturnUnit) {
            calleeDescriptor.returnType.let {
                // If Unit required, no references to non-Unit callables
                if (it == null || !it.isUnit()) return false
            }
        }

        val lambdaValueParameterDescriptors = context[FUNCTION, lambdaExpression.functionLiteral]?.valueParameters ?: return false
        if (explicitReceiver is KtClassLiteralExpression
            && explicitReceiver.receiverExpression?.getCallableDescriptor() in lambdaValueParameterDescriptors
        ) return false
        val explicitReceiverDescriptor = (explicitReceiver as? KtNameReferenceExpression)?.let {
            context[REFERENCE_TARGET, it]
        } as? ValueDescriptor
        val lambdaParameterAsExplicitReceiver = when (noBoundReferences) {
            true -> explicitReceiver != null
            false -> explicitReceiverDescriptor != null && explicitReceiverDescriptor == lambdaValueParameterDescriptors.firstOrNull()
        }
        val explicitReceiverShift = if (lambdaParameterAsExplicitReceiver) 1 else 0

        val lambdaParametersCount = lambdaValueParameterDescriptors.size
        if (lambdaParametersCount != callableArgumentsCount + explicitReceiverShift) return false

        if (explicitReceiver != null && explicitReceiverDescriptor != null && lambdaParameterAsExplicitReceiver) {
            val receiverType = explicitReceiverDescriptor.type
            // No exotic receiver types
            if (receiverType.isTypeParameter() || receiverType.isError || receiverType.isDynamic() ||
                !receiverType.constructor.isDenotable || receiverType.isFunctionType
            ) return false
        }

        // Same lambda / references function parameter order
        if (callableExpression is KtCallExpression) {
            if (lambdaValueParameterDescriptors.size < explicitReceiverShift + callableExpression.valueArguments.size) return false
            val resolvedCall = callableExpression.getResolvedCall(context) ?: return false
            resolvedCall.valueArguments.entries.forEach { (valueParameter, resolvedArgument) ->
                val argument = resolvedArgument.arguments.singleOrNull() ?: return false
                if (resolvedArgument is VarargValueArgument && argument.getSpreadElement() == null) return false
                val argumentExpression = argument.getArgumentExpression() as? KtNameReferenceExpression ?: return false
                val argumentTarget = context[REFERENCE_TARGET, argumentExpression] as? ValueParameterDescriptor ?: return false
                if (argumentTarget != lambdaValueParameterDescriptors[valueParameter.index + explicitReceiverShift]) return false
            }
        }
        return true
    }

    override fun isApplicableTo(element: KtLambdaExpression): Boolean {
        val singleStatement = element.singleStatementOrNull() ?: return false
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

        return when (singleStatement) {
            is KtCallExpression -> {
                isConvertibleCallInLambda(
                    callableExpression = singleStatement,
                    lambdaExpression = element,
                    lambdaMustReturnUnit = lambdaMustReturnUnit
                )
            }
            is KtNameReferenceExpression -> false // Global property reference is not possible (?!)
            is KtDotQualifiedExpression -> {
                val selector = singleStatement.selectorExpression ?: return false
                isConvertibleCallInLambda(
                    callableExpression = selector,
                    explicitReceiver = singleStatement.receiverExpression,
                    lambdaExpression = element,
                    lambdaMustReturnUnit = lambdaMustReturnUnit
                )
            }
            else -> false
        }
    }

    override fun applyTo(element: KtLambdaExpression, editor: Editor?) {
        val referenceName = buildReferenceText(element) ?: return
        val factory = KtPsiFactory(element)
        val lambdaArgument = element.parent as? KtLambdaArgument
        if (lambdaArgument == null) {
            // Without lambda argument syntax, just replace lambda with reference
            val callableReferenceExpr = factory.createCallableReferenceExpression(referenceName) ?: return
            (element.replace(callableReferenceExpr) as? KtElement)?.let { ShortenReferences.RETAIN_COMPANION.process(it) }
        } else {
            // Otherwise, replace the whole argument list for lambda argument-using call
            val outerCallExpression = lambdaArgument.parent as? KtCallExpression ?: return
            val outerCalleeDescriptor = lambdaArgument.outerCalleeDescriptor() ?: return
            // Parameters with default value
            val valueParameters = outerCalleeDescriptor.valueParameters
            val arguments = outerCallExpression.valueArguments.filter { it !is KtLambdaArgument }
            val hadDefaultValues = valueParameters.size - 1 > arguments.size
            val useNamedArguments = valueParameters.any { it.hasDefaultValue() } && hadDefaultValues
                    || arguments.any { it.isNamed() }

            val newArgumentList = factory.buildValueArgumentList {
                appendFixedText("(")
                arguments.forEach { argument ->
                    val argumentName = argument.getArgumentName()
                    if (useNamedArguments && argumentName != null) {
                        appendName(argumentName.asName)
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
                (lambdaArgument.replace(newArgumentList) as? KtElement)?.let { ShortenReferences.RETAIN_COMPANION.process(it) }
            } else {
                (argumentList.replace(newArgumentList) as? KtValueArgumentList)?.let {
                    ShortenReferences.RETAIN_COMPANION.process(it.arguments.last())
                }
                lambdaArgument.delete()
            }
        }
    }

    companion object {

        private fun buildReferenceText(lambdaExpression: KtLambdaExpression, shortTypes: Boolean): String? {
            return when (val singleStatement = lambdaExpression.singleStatementOrNull()) {
                is KtCallExpression -> {
                    val calleeReferenceExpression = singleStatement.calleeExpression as? KtNameReferenceExpression ?: return null
                    val resolvedCall = calleeReferenceExpression.resolveToCall() ?: return null
                    val receiver = resolvedCall.dispatchReceiver ?: resolvedCall.extensionReceiver
                    val descriptor by lazy { receiver?.type?.constructor?.declarationDescriptor }
                    val receiverText = when {
                        receiver == null || descriptor?.isCompanionObject() == true -> ""
                        receiver is ExtensionReceiver || lambdaExpression.getResolutionScope().getImplicitReceiversHierarchy().size == 1 -> "this"
                        else -> descriptor?.name?.let { "this@$it" } ?: return null
                    }
                    "$receiverText::${singleStatement.getCallReferencedName()}"
                }
                is KtDotQualifiedExpression -> {
                    val selectorReferenceName = when (val selector = singleStatement.selectorExpression) {
                        is KtCallExpression -> selector.getCallReferencedName() ?: return null
                        is KtNameReferenceExpression -> selector.getSafeReferencedName()
                        else -> return null
                    }
                    val receiver = singleStatement.receiverExpression
                    val context = receiver.analyze()
                    when (receiver) {
                        is KtNameReferenceExpression -> {
                            val receiverDescriptor = context[REFERENCE_TARGET, receiver] ?: return null
                            val lambdaValueParameters = context[FUNCTION, lambdaExpression.functionLiteral]?.valueParameters ?: return null
                            if (receiverDescriptor is ParameterDescriptor && receiverDescriptor == lambdaValueParameters.firstOrNull()) {
                                val originalReceiverType = receiverDescriptor.type
                                val receiverType = originalReceiverType.approximateFlexibleTypes(preferNotNull = true)
                                if (shortTypes) {
                                    "${IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(receiverType)}::$selectorReferenceName"
                                } else {
                                    "${IdeDescriptorRenderers.SOURCE_CODE.renderType(receiverType)}::$selectorReferenceName"
                                }
                            } else {
                                val receiverName = receiverDescriptor.importableFqName ?: receiverDescriptor.name
                                "$receiverName::$selectorReferenceName"
                            }
                        }
                        else -> {
                            "${receiver.text}::$selectorReferenceName"
                        }
                    }
                }
                else -> null
            }
        }

        private fun KtCallExpression.getCallReferencedName() = (calleeExpression as? KtNameReferenceExpression)?.getSafeReferencedName()

        private fun KtNameReferenceExpression.getSafeReferencedName() = getReferencedNameAsName().render()

        private fun KtLambdaExpression.singleStatementOrNull() = bodyExpression?.statements?.singleOrNull()
    }
}