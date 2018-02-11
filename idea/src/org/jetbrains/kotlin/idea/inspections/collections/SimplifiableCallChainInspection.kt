/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class SimplifiableCallChainInspection : AbstractKotlinInspection() {


    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
            qualifiedExpressionVisitor(fun(expression) {
                val firstExpression = expression.receiverExpression
                val firstCallExpression = getCallExpression(firstExpression) ?: return

                val secondCallExpression = expression.selectorExpression as? KtCallExpression ?: return

                val firstCalleeExpression = firstCallExpression.calleeExpression ?: return
                val secondCalleeExpression = secondCallExpression.calleeExpression ?: return
                val actualConversions = conversionGroups[
                        firstCalleeExpression.text to secondCalleeExpression.text
                        ] ?: return

                val context = expression.analyze()
                val firstResolvedCall = firstExpression.getResolvedCall(context) ?: return
                val conversion = actualConversions.firstOrNull {
                    firstResolvedCall.resultingDescriptor.fqNameOrNull()?.asString() == it.firstFqName
                } ?: return

                val builtIns = context[BindingContext.EXPRESSION_TYPE_INFO, expression]?.type?.builtIns ?: return

                // Do not apply on maps due to lack of relevant stdlib functions
                val firstReceiverType = firstResolvedCall.extensionReceiver?.type
                val firstReceiverRawType = firstReceiverType?.constructor?.declarationDescriptor?.defaultType
                if (firstReceiverRawType != null) {
                    if (firstReceiverRawType.isSubtypeOf(builtIns.map.defaultType) ||
                        firstReceiverRawType.isSubtypeOf(builtIns.mutableMap.defaultType)) return
                }

                // Do not apply for lambdas with return inside
                val lambdaArgument = firstCallExpression.lambdaArguments.firstOrNull()
                if (lambdaArgument?.anyDescendantOfType<KtReturnExpression>() == true) return

                val secondResolvedCall = expression.getResolvedCall(context) ?: return
                val secondResultingDescriptor = secondResolvedCall.resultingDescriptor
                if (secondResultingDescriptor.fqNameOrNull()?.asString() != conversion.secondFqName) return
                if (secondResolvedCall.valueArguments.any { (parameter, resolvedArgument) ->
                    parameter.type.isFunctionOfAnyKind() &&
                    resolvedArgument !is DefaultValueArgument
                }) return

                if (conversion.replacement.startsWith("joinTo")) {
                    // Function parameter in map must have String result type
                    if (!firstResolvedCall.hasLastFunctionalParameterWithResult(context) {
                        it.isSubtypeOf(builtIns.charSequence.defaultType)
                    }) return
                }

                val descriptor = holder.manager.createProblemDescriptor(
                        expression,
                        firstCalleeExpression.textRange.shiftRight(-expression.startOffset),
                        "Call chain on collection type may be simplified",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        isOnTheFly,
                        SimplifyCallChainFix(conversion.replacement)
                )
                holder.registerProblem(descriptor)
            })

    companion object {

        private val conversions = listOf(
                Conversion("kotlin.collections.filter", "kotlin.collections.first", "first"),
                Conversion("kotlin.collections.filter", "kotlin.collections.firstOrNull", "firstOrNull"),
                Conversion("kotlin.collections.filter", "kotlin.collections.last", "last"),
                Conversion("kotlin.collections.filter", "kotlin.collections.lastOrNull", "lastOrNull"),
                Conversion("kotlin.collections.filter", "kotlin.collections.single", "single"),
                Conversion("kotlin.collections.filter", "kotlin.collections.singleOrNull", "singleOrNull"),
                Conversion("kotlin.collections.filter", "kotlin.collections.isNotEmpty", "any"),
                Conversion("kotlin.collections.filter", "kotlin.collections.List.isEmpty", "none"),

                Conversion("kotlin.text.filter", "kotlin.text.first", "first"),
                Conversion("kotlin.text.filter", "kotlin.text.firstOrNull", "firstOrNull"),
                Conversion("kotlin.text.filter", "kotlin.text.last", "last"),
                Conversion("kotlin.text.filter", "kotlin.text.lastOrNull", "lastOrNull"),
                Conversion("kotlin.text.filter", "kotlin.text.single", "single"),
                Conversion("kotlin.text.filter", "kotlin.text.singleOrNull", "singleOrNull"),
                Conversion("kotlin.text.filter", "kotlin.text.isNotEmpty", "any"),
                Conversion("kotlin.text.filter", "kotlin.text.isEmpty", "none"),

                Conversion("kotlin.collections.map", "kotlin.collections.joinTo", "joinTo"),
                Conversion("kotlin.collections.map", "kotlin.collections.joinToString", "joinToString"),
                Conversion("kotlin.collections.map", "kotlin.collections.filterNotNull", "mapNotNull"),

                Conversion("kotlin.collections.listOf", "kotlin.collections.filterNotNull", "listOfNotNull")
        )

        private val conversionGroups = conversions.groupBy { it.firstName to it.secondName }

        data class Conversion(val firstFqName: String, val secondFqName: String, val replacement: String) {
            private fun String.convertToShort() = takeLastWhile { it != '.' }

            val firstName = firstFqName.convertToShort()

            val secondName = secondFqName.convertToShort()
        }

        fun getCallExpression(firstExpression: KtExpression) =
                ((firstExpression as? KtQualifiedExpression)?.selectorExpression as? KtCallExpression
                 ?: firstExpression as? KtCallExpression)

    }
}