/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.quickfix.ReplaceSelectorOfQualifiedExpressionFix
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class UselessCallOnCollectionInspection : AbstractUselessCallInspection() {
    override val uselessFqNames = mapOf(
        "kotlin.collections.filterNotNull" to deleteConversion,
        "kotlin.collections.filterIsInstance" to deleteConversion,
        "kotlin.collections.mapNotNull" to Conversion("map"),
        "kotlin.collections.mapNotNullTo" to Conversion("mapTo"),
        "kotlin.collections.mapIndexedNotNull" to Conversion("mapIndexed"),
        "kotlin.collections.mapIndexedNotNullTo" to Conversion("mapIndexedTo")
    )

    override val uselessNames = uselessFqNames.keys.toShortNames()

    override fun QualifiedExpressionVisitor.suggestConversionIfNeeded(
        expression: KtQualifiedExpression,
        calleeExpression: KtExpression,
        context: BindingContext,
        conversion: Conversion
    ) {
        val receiverType = expression.receiverExpression.getType(context) ?: return
        val receiverTypeArgument = receiverType.arguments.singleOrNull()?.type ?: return
        val resolvedCall = expression.getResolvedCall(context) ?: return
        if (calleeExpression.text == "filterIsInstance") {
            val typeParameterDescriptor = resolvedCall.candidateDescriptor.typeParameters.singleOrNull() ?: return
            val argumentType = resolvedCall.typeArguments[typeParameterDescriptor] ?: return
            if (receiverTypeArgument.isFlexible() || !receiverTypeArgument.isSubtypeOf(argumentType)) return
        } else {
            // xxxNotNull
            if (TypeUtils.isNullableType(receiverTypeArgument)) return
            if (calleeExpression.text != "filterNotNull") {
                // Also check last argument functional type to have not-null result
                val lastParameterMatches = resolvedCall.hasLastFunctionalParameterWithResult(context) {
                    !TypeUtils.isNullableType(it) && it.constructor !is TypeVariableTypeConstructor
                }
                if (!lastParameterMatches) return
            }
        }

        val newName = conversion.replacementName
        if (newName != null) {
            val descriptor = holder.manager.createProblemDescriptor(
                expression,
                TextRange(
                    expression.operationTokenNode.startOffset - expression.startOffset,
                    calleeExpression.endOffset - expression.startOffset
                ),
                KotlinBundle.message("call.on.collection.type.may.be.reduced"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                isOnTheFly,
                RenameUselessCallFix(newName)
            )
            holder.registerProblem(descriptor)
        } else {
            val fix = if (resolvedCall.resultingDescriptor.returnType.isList() && !receiverType.isList()) {
                ReplaceSelectorOfQualifiedExpressionFix("toList()")
            } else {
                RemoveUselessCallFix()
            }
            val descriptor = holder.manager.createProblemDescriptor(
                expression,
                TextRange(
                    expression.operationTokenNode.startOffset - expression.startOffset,
                    calleeExpression.endOffset - expression.startOffset
                ),
                KotlinBundle.message("useless.call.on.collection.type"),
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                isOnTheFly,
                fix
            )
            holder.registerProblem(descriptor)
        }
    }

    private fun KotlinType?.isList() = this?.constructor?.declarationDescriptor?.fqNameSafe == KotlinBuiltIns.FQ_NAMES.list
}