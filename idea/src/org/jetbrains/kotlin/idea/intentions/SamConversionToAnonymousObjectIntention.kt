/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.sam.JavaSingleAbstractMethodUtils
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class SamConversionToAnonymousObjectIntention : SelfTargetingRangeIntention<KtCallExpression>(
    KtCallExpression::class.java, KotlinBundle.lazyMessage("convert.to.anonymous.object")
), LowPriorityAction {
    override fun applicabilityRange(element: KtCallExpression): TextRange? {
        val callee = element.calleeExpression ?: return null
        val lambda = getLambdaExpression(element) ?: return null
        val functionLiteral = lambda.functionLiteral
        val bindingContext = functionLiteral.analyze()
        val sam = element.getSingleAbstractMethod(bindingContext) ?: return null

        val samValueParameters = sam.valueParameters
        val samValueParameterSize = samValueParameters.size
        if (samValueParameterSize != functionLiteral.functionDescriptor(bindingContext)?.valueParameters?.size) return null

        val samName = sam.name.asString()
        if (functionLiteral.anyDescendantOfType<KtCallExpression> { call ->
                if (call.calleeExpression?.text != samName) return@anyDescendantOfType false
                val valueArguments = call.valueArguments
                if (valueArguments.size != samValueParameterSize) return@anyDescendantOfType false
                val context = call.analyze(BodyResolveMode.PARTIAL)
                valueArguments.zip(samValueParameters).all { (arg, param) ->
                    arg.getArgumentExpression()?.getType(context)?.isSubtypeOf(param.type) == true
                }
            }) return null

        if (bindingContext.diagnostics.forElement(functionLiteral).any { it.severity == Severity.ERROR }) return null

        return callee.textRange
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val lambda = getLambdaExpression(element) ?: return
        val context = element.analyze(BodyResolveMode.PARTIAL)
        val lambdaFunctionDescriptor = lambda.functionLiteral.functionDescriptor(context) ?: return
        val samDescriptor = element.getSingleAbstractMethod(context) ?: return
        convertToAnonymousObject(element, samDescriptor, lambda, lambdaFunctionDescriptor)
    }

    private fun KtCallExpression.getSingleAbstractMethod(context: BindingContext): FunctionDescriptor? {
        val type = getType(context) ?: return null
        if (!JavaSingleAbstractMethodUtils.isSamType(type)) return null
        val classDescriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
        return getSingleAbstractMethodOrNull(classDescriptor)
    }

    private fun KtFunctionLiteral.functionDescriptor(context: BindingContext): AnonymousFunctionDescriptor? =
        context[BindingContext.FUNCTION, this] as? AnonymousFunctionDescriptor

    companion object {
        fun convertToAnonymousObject(
            call: KtCallExpression,
            samDescriptor: FunctionDescriptor,
            lambda: KtLambdaExpression,
            lambdaFunctionDescriptor: AnonymousFunctionDescriptor? = null
        ) {
            val parentOfCall = call.getQualifiedExpressionForSelector()
            val interfaceName = if (parentOfCall != null) {
                parentOfCall.resolveToCall()?.resultingDescriptor?.fqNameSafe?.asString()
            } else {
                call.calleeExpression?.text
            } ?: return

            val typeArguments = call.typeArguments.mapNotNull { it.typeReference }
            val typeArgumentsText = if (typeArguments.isEmpty()) {
                ""
            } else {
                typeArguments.joinToString(prefix = "<", postfix = ">", separator = ", ") { it.text }
            }

            val functionDescriptor = lambdaFunctionDescriptor ?: samDescriptor
            val functionName = samDescriptor.name.asString()
            val samParameters = samDescriptor.valueParameters
            val nameValidator = CollectingNameValidator(lambdaFunctionDescriptor?.valueParameters?.map { it.name.asString() }.orEmpty())
            val functionParameterName: (ValueParameterDescriptor, Int) -> String = { parameter, index ->
                val name = parameter.name
                if (name.isSpecial) {
                    KotlinNameSuggester.suggestNameByName((samParameters.getOrNull(index)?.name ?: name).asString(), nameValidator)
                } else {
                    name.asString()
                }
            }
            val classDescriptor = functionDescriptor.containingDeclaration as? ClassDescriptor
            val typeParameters = classDescriptor?.declaredTypeParameters?.map { it.name.asString() }?.zip(typeArguments)?.toMap().orEmpty()

            LambdaToAnonymousFunctionIntention.convertLambdaToFunction(
                lambda,
                functionDescriptor,
                functionName,
                functionParameterName,
                typeParameters
            ) {
                it.addModifier(KtTokens.OVERRIDE_KEYWORD)
                (parentOfCall ?: call).replaced(
                    KtPsiFactory(it).createExpression("object : $interfaceName$typeArgumentsText { ${it.text} }")
                )
            }
        }

        fun getLambdaExpression(element: KtCallExpression): KtLambdaExpression? =
            element.lambdaArguments.firstOrNull()?.getLambdaExpression()
                ?: element.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression
    }
}