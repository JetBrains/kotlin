/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.coroutines

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.collections.AbstractCallChainChecker
import org.jetbrains.kotlin.idea.inspections.collections.SimplifyCallChainFix
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.qualifiedExpressionVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RedundantAsyncInspection : AbstractCallChainChecker() {

    fun generateConversion(expression: KtQualifiedExpression): Conversion? {
        var defaultContext: Boolean? = null
        var defaultStart: Boolean? = null

        var conversion = findQualifiedConversion(expression, conversionGroups) check@{ _, firstResolvedCall, _, _ ->
            for ((parameterDescriptor, valueArgument) in firstResolvedCall.valueArguments) {
                val default = valueArgument is DefaultValueArgument
                when (parameterDescriptor.name.asString()) {
                    CONTEXT_ARGUMENT_NAME -> defaultContext = default
                    START_ARGUMENT_NAME -> defaultStart = default
                }
            }
            true
        } ?: return null
        defaultContext ?: return null
        defaultStart ?: return null
        if (!defaultStart!!) return null

        val receiverExpression = expression.receiverExpression
        val scopeExpression = (receiverExpression as? KtQualifiedExpression)?.receiverExpression
        if (scopeExpression != null) {
            val context = scopeExpression.analyze(BodyResolveMode.PARTIAL)
            val scopeDescriptor = (scopeExpression as? KtNameReferenceExpression)?.let { context[BindingContext.REFERENCE_TARGET, it] }
            if (scopeDescriptor?.fqNameSafe?.toString() != GLOBAL_SCOPE) {
                conversion = conversion.withArgument("${scopeExpression.text}.coroutineContext")
            }
        }

        if (conversion.additionalArgument == null && defaultContext!! && defaultStart!!) {
            conversion = conversion.withArgument(
                if (conversion === conversions[0]) {
                    DEFAULT_ASYNC_ARGUMENT
                } else {
                    DEFAULT_ASYNC_ARGUMENT_EXPERIMENTAL
                }
            )
        }
        return conversion
    }

    fun generateFix(conversion: Conversion): SimplifyCallChainFix {
        val contextArgument = conversion.additionalArgument
        return SimplifyCallChainFix(conversion, removeReceiverOfFirstCall = true, runOptimizeImports = true) { callExpression ->
            if (contextArgument != null) {
                val call = callExpression.resolveToCall()
                if (call != null) {
                    for (argument in callExpression.valueArguments) {
                        val mapping = call.getArgumentMapping(argument) as? ArgumentMatch ?: continue
                        if (mapping.valueParameter.name.asString() == CONTEXT_ARGUMENT_NAME) {
                            val name = argument.getArgumentName()?.asName
                            val expressionText = contextArgument + " + " + argument.getArgumentExpression()!!.text
                            argument.replace(
                                if (name == null) {
                                    createArgument(expressionText)
                                } else {
                                    createArgument("$name = $expressionText")
                                }
                            )
                            break
                        }
                    }
                }
            }
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        qualifiedExpressionVisitor(fun(expression) {
            val conversion = generateConversion(expression) ?: return
            val descriptor = holder.manager.createProblemDescriptor(
                expression,
                expression.firstCalleeExpression()!!.textRange.shiftRight(-expression.startOffset),
                "Redundant 'async' call may be reduced to '${conversion.replacement}'",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                isOnTheFly,
                generateFix(conversion)
            )
            holder.registerProblem(descriptor)
        })

    private val conversionGroups = conversions.group()

    companion object {
        private val conversions = listOf(
            Conversion(
                "$COROUTINE_PACKAGE.async",
                "$COROUTINE_PACKAGE.Deferred.await",
                "$COROUTINE_PACKAGE.withContext"
            ),
            Conversion(
                "$COROUTINE_EXPERIMENTAL_PACKAGE.async",
                "$COROUTINE_EXPERIMENTAL_PACKAGE.Deferred.await",
                "$COROUTINE_EXPERIMENTAL_PACKAGE.withContext"
            )
        )

        private const val GLOBAL_SCOPE = "kotlinx.coroutines.GlobalScope"

        private const val CONTEXT_ARGUMENT_NAME = "context"

        private const val START_ARGUMENT_NAME = "start"

        private const val DEFAULT_ASYNC_ARGUMENT = "$COROUTINE_PACKAGE.Dispatchers.Default"

        private const val DEFAULT_ASYNC_ARGUMENT_EXPERIMENTAL = "$COROUTINE_EXPERIMENTAL_PACKAGE.DefaultDispatcher"
    }
}

internal const val COROUTINE_PACKAGE = "kotlinx.coroutines"

internal const val COROUTINE_EXPERIMENTAL_PACKAGE = "kotlinx.coroutines.experimental"
