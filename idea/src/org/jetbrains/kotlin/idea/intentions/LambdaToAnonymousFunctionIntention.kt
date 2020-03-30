/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.moveInsideParentheses
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class LambdaToAnonymousFunctionIntention : SelfTargetingIntention<KtLambdaExpression>(
    KtLambdaExpression::class.java,
    KotlinBundle.lazyMessage("convert.to.anonymous.function"),
    KotlinBundle.lazyMessage("convert.lambda.expression.to.anonymous.function")
), LowPriorityAction {
    override fun isApplicableTo(element: KtLambdaExpression, caretOffset: Int): Boolean {
        val argument = element.getStrictParentOfType<KtValueArgument>() ?: return false
        val call = argument.getStrictParentOfType<KtCallElement>() ?: return false
        if (call.getStrictParentOfType<KtFunction>()?.hasModifier(KtTokens.INLINE_KEYWORD) == true) return false

        val context = call.analyze(BodyResolveMode.PARTIAL)
        if (call.getResolvedCall(context)?.getParameterForArgument(argument)?.type?.isSuspendFunctionType == true) return false
        val descriptor =
            context[BindingContext.DECLARATION_TO_DESCRIPTOR, element.functionLiteral] as? AnonymousFunctionDescriptor ?: return false
        if (descriptor.valueParameters.any { it.name.isSpecial }) return false

        val lastElement = element.functionLiteral.arrow ?: element.functionLiteral.lBrace
        return caretOffset <= lastElement.endOffset
    }

    override fun applyTo(element: KtLambdaExpression, editor: Editor?) {
        val functionDescriptor = element.functionLiteral.descriptor as? AnonymousFunctionDescriptor ?: return
        val resultingFunction = convertLambdaToFunction(element, functionDescriptor) ?: return
        (resultingFunction.parent as? KtLambdaArgument)?.also { it.moveInsideParentheses(it.analyze(BodyResolveMode.PARTIAL)) }
    }

    companion object {
        fun convertLambdaToFunction(
            lambda: KtLambdaExpression,
            functionDescriptor: FunctionDescriptor,
            functionName: String = "",
            typeParameters: Map<String, KtTypeReference> = emptyMap(),
            replaceElement: (KtNamedFunction) -> KtExpression = { lambda.replaced(it) }
        ): KtExpression? {
            val typeSourceCode = IdeDescriptorRenderers.SOURCE_CODE_TYPES
            val functionLiteral = lambda.functionLiteral
            val bodyExpression = functionLiteral.bodyExpression ?: return null

            val context = bodyExpression.analyze(BodyResolveMode.PARTIAL)
            val functionLiteralDescriptor by lazy { functionLiteral.descriptor }
            bodyExpression.collectDescendantsOfType<KtReturnExpression>().forEach {
                val targetDescriptor = it.getTargetFunctionDescriptor(context)
                if (targetDescriptor == functionDescriptor || targetDescriptor == functionLiteralDescriptor) it.labeledExpression?.delete()
            }

            val psiFactory = KtPsiFactory(lambda)
            val function = psiFactory.createFunction(
                KtPsiFactory.CallableBuilder(KtPsiFactory.CallableBuilder.Target.FUNCTION).apply {
                    typeParams()
                    functionDescriptor.extensionReceiverParameter?.type?.let {
                        receiver(typeSourceCode.renderType(it))
                    }

                    name(functionName)
                    for (parameter in functionDescriptor.valueParameters) {
                        val type = parameter.type.let { if (it.isFlexible()) it.makeNotNullable() else it }
                        val renderType = typeSourceCode.renderType(type)
                        if (type.isTypeParameter()) {
                            param(parameter.name.asString(), typeParameters[renderType]?.text ?: renderType)
                        } else {
                            param(parameter.name.asString(), renderType)
                        }
                    }

                    functionDescriptor.returnType?.takeIf { !it.isUnit() }?.let {
                        val lastStatement = bodyExpression.statements.lastOrNull()
                        if (lastStatement != null && lastStatement !is KtReturnExpression) {
                            val foldableReturns = BranchedFoldingUtils.getFoldableReturns(lastStatement)
                            if (foldableReturns == null || foldableReturns.isEmpty()) {
                                lastStatement.replace(psiFactory.createExpressionByPattern("return $0", lastStatement))
                            }
                        }
                        val renderType = typeSourceCode.renderType(it)
                        if (it.isTypeParameter()) {
                            returnType(typeParameters[renderType]?.text ?: renderType)
                        } else {
                            returnType(renderType)
                        }
                    } ?: noReturnType()
                    blockBody(" " + bodyExpression.text)
                }.asString()
            )

            return replaceElement(function).also { ShortenReferences.DEFAULT.process(it) }
        }
    }
}
