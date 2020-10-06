/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.qualifiedExpressionVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class SimplifiableCallChainInspection : AbstractCallChainChecker() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): KtVisitorVoid {
        return qualifiedExpressionVisitor(fun(expression) {
            val conversion = findQualifiedConversion(expression, conversionGroups) check@{ conversion, firstResolvedCall, _, context ->
                // Do not apply on maps due to lack of relevant stdlib functions
                val firstReceiverType = firstResolvedCall.extensionReceiver?.type
                if (firstReceiverType != null) {
                    if (conversion.replacement == "mapNotNull" && KotlinBuiltIns.isPrimitiveArray(firstReceiverType)) return@check false
                    val builtIns = context[BindingContext.EXPRESSION_TYPE_INFO, expression]?.type?.builtIns ?: return@check false
                    val firstReceiverRawType = firstReceiverType.constructor.declarationDescriptor?.defaultType
                    if (firstReceiverRawType.isMap(builtIns)) return@check false
                }
                if (conversion.replacement.startsWith("joinTo")) {
                    // Function parameter in map must have String result type
                    if (!firstResolvedCall.hasLastFunctionalParameterWithResult(context) {
                            it.isSubtypeOf(JsPlatformAnalyzerServices.builtIns.charSequence.defaultType)
                        }
                    ) return@check false
                }

                return@check conversion.enableSuspendFunctionCall || !containsSuspendFunctionCall(firstResolvedCall, context)
            } ?: return

            val replacement = conversion.replacement
            val descriptor = holder.manager.createProblemDescriptor(
                expression,
                expression.firstCalleeExpression()!!.textRange.shiftRight(-expression.startOffset),
                KotlinBundle.message("call.chain.on.collection.type.may.be.simplified"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                isOnTheFly,
                SimplifyCallChainFix(conversion) { callExpression ->
                    val lastArgumentName = if (replacement.startsWith("joinTo")) Name.identifier("transform") else null
                    if (lastArgumentName != null) {
                        val lastArgument = callExpression.valueArgumentList?.arguments?.singleOrNull()
                        val argumentExpression = lastArgument?.getArgumentExpression()
                        if (argumentExpression != null) {
                            lastArgument.replace(createArgument(argumentExpression, lastArgumentName))
                        }
                    }
                }
            )
            holder.registerProblem(descriptor)
        })
    }

    private fun containsSuspendFunctionCall(resolvedCall: ResolvedCall<*>, context: BindingContext): Boolean {
        return resolvedCall.call.callElement.anyDescendantOfType<KtCallExpression> {
            it.getResolvedCall(context)?.resultingDescriptor?.isSuspend == true
        }
    }

    private val conversionGroups = conversions.group()

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
            Conversion("kotlin.collections.sorted", "kotlin.collections.firstOrNull", "min"),
            Conversion("kotlin.collections.sorted", "kotlin.collections.lastOrNull", "max"),
            Conversion("kotlin.collections.sortedDescending", "kotlin.collections.firstOrNull", "max"),
            Conversion("kotlin.collections.sortedDescending", "kotlin.collections.lastOrNull", "min"),
            Conversion("kotlin.collections.sortedBy", "kotlin.collections.firstOrNull", "minBy"),
            Conversion("kotlin.collections.sortedBy", "kotlin.collections.lastOrNull", "maxBy"),
            Conversion("kotlin.collections.sortedByDescending", "kotlin.collections.firstOrNull", "maxBy"),
            Conversion("kotlin.collections.sortedByDescending", "kotlin.collections.lastOrNull", "minBy"),
            Conversion("kotlin.collections.sorted", "kotlin.collections.first", "min", withNotNullAssertion = true),
            Conversion("kotlin.collections.sorted", "kotlin.collections.last", "max", withNotNullAssertion = true),
            Conversion("kotlin.collections.sortedDescending", "kotlin.collections.first", "max", withNotNullAssertion = true),
            Conversion("kotlin.collections.sortedDescending", "kotlin.collections.last", "min", withNotNullAssertion = true),
            Conversion("kotlin.collections.sortedBy", "kotlin.collections.first", "minBy", withNotNullAssertion = true),
            Conversion("kotlin.collections.sortedBy", "kotlin.collections.last", "maxBy", withNotNullAssertion = true),
            Conversion("kotlin.collections.sortedByDescending", "kotlin.collections.first", "maxBy", withNotNullAssertion = true),
            Conversion("kotlin.collections.sortedByDescending", "kotlin.collections.last", "minBy", withNotNullAssertion = true),

            Conversion("kotlin.text.filter", "kotlin.text.first", "first"),
            Conversion("kotlin.text.filter", "kotlin.text.firstOrNull", "firstOrNull"),
            Conversion("kotlin.text.filter", "kotlin.text.last", "last"),
            Conversion("kotlin.text.filter", "kotlin.text.lastOrNull", "lastOrNull"),
            Conversion("kotlin.text.filter", "kotlin.text.single", "single"),
            Conversion("kotlin.text.filter", "kotlin.text.singleOrNull", "singleOrNull"),
            Conversion("kotlin.text.filter", "kotlin.text.isNotEmpty", "any"),
            Conversion("kotlin.text.filter", "kotlin.text.isEmpty", "none"),

            Conversion("kotlin.collections.map", "kotlin.collections.joinTo", "joinTo", enableSuspendFunctionCall = false),
            Conversion("kotlin.collections.map", "kotlin.collections.joinToString", "joinToString", enableSuspendFunctionCall = false),
            Conversion("kotlin.collections.map", "kotlin.collections.filterNotNull", "mapNotNull"),

            Conversion("kotlin.collections.listOf", "kotlin.collections.filterNotNull", "listOfNotNull")
        )
    }
}