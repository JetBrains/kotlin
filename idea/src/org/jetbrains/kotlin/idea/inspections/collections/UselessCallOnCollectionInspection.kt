/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class UselessCallOnCollectionInspection : AbstractUselessCallInspection() {
    override val uselessFqNames = mapOf("kotlin.collections.filterNotNull" to deleteConversion,
                                        "kotlin.collections.filterIsInstance" to deleteConversion,
                                        "kotlin.collections.mapNotNull" to Conversion("map"),
                                        "kotlin.collections.mapNotNullTo" to Conversion("mapTo"),
                                        "kotlin.collections.mapIndexedNotNull" to Conversion("mapIndexed"),
                                        "kotlin.collections.mapIndexedNotNullTo" to Conversion("mapIndexedTo"))

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
        }
        else {
            // xxxNotNull
            if (TypeUtils.isNullableType(receiverTypeArgument)) return
            if (calleeExpression.text != "filterNotNull") {
                // Also check last argument functional type to have not-null result
                if (!resolvedCall.hasLastFunctionalParameterWithResult(context) { !TypeUtils.isNullableType(it) }) return
            }
        }

        val newName = conversion.replacementName
        if (newName != null) {
            val descriptor = holder.manager.createProblemDescriptor(
                    expression,
                    TextRange(expression.operationTokenNode.startOffset - expression.startOffset,
                              calleeExpression.endOffset - expression.startOffset),
                    "Call on collection type may be reduced",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly,
                    RenameUselessCallFix(newName)
            )
            holder.registerProblem(descriptor)
        }
        else {
            val descriptor = holder.manager.createProblemDescriptor(
                    expression,
                    TextRange(expression.operationTokenNode.startOffset - expression.startOffset,
                              calleeExpression.endOffset - expression.startOffset),
                    "Useless call on collection type",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    isOnTheFly,
                    RemoveUselessCallFix()
            )
            holder.registerProblem(descriptor)
        }
    }
}