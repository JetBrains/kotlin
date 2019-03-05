/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.js.resolve.JsPlatformCompilerServices
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.qualifiedExpressionVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class SimplifiableCallChainInspection : AbstractCallChainChecker() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        qualifiedExpressionVisitor(fun(expression) {
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
                            it.isSubtypeOf(JsPlatformCompilerServices.builtIns.charSequence.defaultType)
                        }
                    ) return@check false
                }
                true
            } ?: return

            val replacement = conversion.replacement
            val descriptor = holder.manager.createProblemDescriptor(
                expression,
                expression.firstCalleeExpression()!!.textRange.shiftRight(-expression.startOffset),
                "Call chain on collection type may be simplified",
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
    }
}