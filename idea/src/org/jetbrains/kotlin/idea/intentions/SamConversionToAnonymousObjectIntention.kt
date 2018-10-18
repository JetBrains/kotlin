/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class SamConversionToAnonymousObjectIntention : SelfTargetingRangeIntention<KtCallExpression>(
    KtCallExpression::class.java, "Convert to anonymous object"
), LowPriorityAction {

    override fun applicabilityRange(element: KtCallExpression): TextRange? {
        val callee = element.calleeExpression ?: return null
        val lambda = getLambdaExpression(element) ?: return null
        val functionLiteral = lambda.functionLiteral
        val descriptor = (functionLiteral.descriptor as? FunctionDescriptor) ?: return null
        val bindingContext = functionLiteral.analyze()
        val sam = element.getSingleAbstractMethod(bindingContext) ?: return null

        val samValueParameters = sam.valueParameters
        val samValueParameterSize = samValueParameters.size
        if (descriptor.valueParameters.size != samValueParameterSize) return null

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
        val functionDescriptor = lambda.functionLiteral.descriptor as? FunctionDescriptor ?: return
        val functionName = element.getSingleAbstractMethod(element.analyze(BodyResolveMode.PARTIAL))?.name?.asString() ?: return
        convertToAnonymousObject(element, lambda, functionDescriptor, functionName)
    }

    private fun KtCallExpression.getSingleAbstractMethod(context: BindingContext): FunctionDescriptor? {
        val type = getType(context) ?: return null
        if (!SingleAbstractMethodUtils.isSamType(type)) return null
        val javaClass = type.constructor.declarationDescriptor as? JavaClassDescriptor ?: return null
        return SingleAbstractMethodUtils.getSingleAbstractMethodOrNull(javaClass)
    }

    companion object {
        fun convertToAnonymousObject(call: KtCallExpression, functionDescriptor: FunctionDescriptor, functionName: String) {
            val lambda = getLambdaExpression(call) ?: return
            convertToAnonymousObject(call, lambda, functionDescriptor, functionName)
        }

        private fun convertToAnonymousObject(
            call: KtCallExpression,
            lambda: KtLambdaExpression,
            functionDescriptor: FunctionDescriptor,
            functionName: String
        ) {
            val interfaceName = call.calleeExpression?.text ?: return
            LambdaToAnonymousFunctionIntention.convertLambdaToFunction(lambda, functionDescriptor, functionName = functionName) {
                it.addModifier(KtTokens.OVERRIDE_KEYWORD)
                call.replaced(KtPsiFactory(it).createExpression("object : $interfaceName { ${it.text} }"))
            }
        }

        private fun getLambdaExpression(element: KtCallExpression): KtLambdaExpression? {
            return element.lambdaArguments.firstOrNull()?.getLambdaExpression()
                ?: element.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression
        }
    }
}