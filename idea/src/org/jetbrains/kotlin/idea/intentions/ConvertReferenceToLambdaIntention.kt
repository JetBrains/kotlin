/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.DOUBLE_COLON_LHS
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS

@Suppress("DEPRECATION")
class ConvertReferenceToLambdaInspection : IntentionBasedInspection<KtCallableReferenceExpression>(ConvertReferenceToLambdaIntention::class)

class ConvertReferenceToLambdaIntention : SelfTargetingOffsetIndependentIntention<KtCallableReferenceExpression>(
    KtCallableReferenceExpression::class.java, KotlinBundle.lazyMessage("convert.reference.to.lambda")
) {
    override fun applyTo(element: KtCallableReferenceExpression, editor: Editor?) {
        val context = element.analyze(BodyResolveMode.PARTIAL)
        val reference = element.callableReference
        val targetDescriptor = context[REFERENCE_TARGET, reference] as? CallableMemberDescriptor ?: return
        val valueArgumentParent = element.parent as? KtValueArgument
        val callGrandParent = valueArgumentParent?.parent?.parent as? KtCallExpression
        val resolvedCall = callGrandParent?.getResolvedCall(context)
        val matchingParameterType = resolvedCall?.getParameterForArgument(valueArgumentParent)?.type
        val matchingParameterIsExtension = matchingParameterType?.isExtensionFunctionType ?: false
        val firstArgumentIsThis = matchingParameterIsExtension && resolvedCall?.resultingDescriptor?.isExtension == true

        val receiverExpression = element.receiverExpression
        val receiverType = receiverExpression?.let {
            (context[DOUBLE_COLON_LHS, it] as? DoubleColonLHS.Type)?.type
        }
        val acceptsReceiverAsParameter = receiverType != null && !matchingParameterIsExtension &&
                (targetDescriptor.dispatchReceiverParameter != null || targetDescriptor.extensionReceiverParameter != null)

        val parameterNamesAndTypes = targetDescriptor.valueParameters.map { it.name.asString() to it.type }.let {
            if (matchingParameterType != null) {
                val parameterSize = matchingParameterType.arguments.size - (if (acceptsReceiverAsParameter) 2 else 1)
                if (parameterSize >= 0) it.take(parameterSize) else it
            } else {
                it
            }
        }
        val receiverNameAndType = receiverType?.let {
            KotlinNameSuggester.suggestNamesByType(it, validator = { name ->
                name !in parameterNamesAndTypes.map { pair -> pair.first }
            }, defaultName = "receiver").first() to it
        }

        val factory = KtPsiFactory(element)
        val targetName = reference.text
        val lambdaParameterNamesAndTypes =
            if (acceptsReceiverAsParameter) listOf(receiverNameAndType!!) + parameterNamesAndTypes
            else parameterNamesAndTypes

        val receiverPrefix = when {
            acceptsReceiverAsParameter -> receiverNameAndType!!.first + "."
            matchingParameterIsExtension -> ""
            else -> receiverExpression?.let { it.text + "." } ?: ""
        }

        val lambdaExpression = if (valueArgumentParent != null &&
            lambdaParameterNamesAndTypes.size == 1 &&
            receiverExpression?.text != "it"
        ) {
            factory.createLambdaExpression(
                parameters = "",
                body = when {
                    acceptsReceiverAsParameter ->
                        if (targetDescriptor is PropertyDescriptor) "it.$targetName"
                        else "it.$targetName()"
                    else ->
                        "$receiverPrefix$targetName(${if (firstArgumentIsThis) "this" else "it"})"
                }
            )
        } else {
            val (lambdaParams, args) = if (firstArgumentIsThis) {
                val thisArgument = if (parameterNamesAndTypes.isNotEmpty()) listOf("this") else emptyList()
                lambdaParameterNamesAndTypes.drop(1) to (thisArgument + parameterNamesAndTypes.drop(1).map { it.first })
            } else {
                lambdaParameterNamesAndTypes to parameterNamesAndTypes.map { it.first }
            }
            factory.createLambdaExpression(
                parameters = lambdaParams.joinToString(separator = ", ") {
                    if (valueArgumentParent != null) it.first
                    else it.first + ": " + SOURCE_RENDERER.renderType(it.second)
                },
                body = if (targetDescriptor is PropertyDescriptor) {
                    "$receiverPrefix$targetName"
                } else {
                    args.joinToString(
                        prefix = "$receiverPrefix$targetName(",
                        separator = ", ",
                        postfix = ")"
                    )
                }
            )
        }

        val needParentheses = lambdaParameterNamesAndTypes.isEmpty() && when (element.parent.node.elementType) {
            KtNodeTypes.WHEN_ENTRY, KtNodeTypes.THEN, KtNodeTypes.ELSE -> true
            else -> false
        }
        val wrappedExpression =
            if (needParentheses) factory.createExpressionByPattern("($0)", lambdaExpression) else lambdaExpression
        ShortenReferences.DEFAULT.process(element.replaced(wrappedExpression))

        if (valueArgumentParent != null && callGrandParent != null) {
            callGrandParent.getLastLambdaExpression()?.moveFunctionLiteralOutsideParenthesesIfPossible()
        }
    }

    override fun isApplicableTo(element: KtCallableReferenceExpression): Boolean {
        val context = element.analyze(BodyResolveMode.PARTIAL)
        val expectedType = context[BindingContext.EXPECTED_EXPRESSION_TYPE, element] ?: return true
        val expectedTypeDescriptor = expectedType.constructor.declarationDescriptor as? ClassDescriptor ?: return true
        val expectedTypeFqName = expectedTypeDescriptor.fqNameSafe
        return expectedTypeFqName.isRoot || expectedTypeFqName.parent() != KOTLIN_REFLECT_FQ_NAME
    }

    companion object {
        private val SOURCE_RENDERER = IdeDescriptorRenderers.SOURCE_CODE
    }
}