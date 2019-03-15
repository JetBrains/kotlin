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

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.quickfix.ReplaceWithDotCallFix
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.TypeUtils

class UselessCallOnNotNullInspection : AbstractUselessCallInspection() {
    override val uselessFqNames = mapOf("kotlin.collections.orEmpty" to deleteConversion,
                                        "kotlin.sequences.orEmpty" to deleteConversion,
                                        "kotlin.text.orEmpty" to deleteConversion,
                                        "kotlin.text.isNullOrEmpty" to Conversion("isEmpty"),
                                        "kotlin.text.isNullOrBlank" to Conversion("isBlank"))

    override val uselessNames = uselessFqNames.keys.toShortNames()

    override fun QualifiedExpressionVisitor.suggestConversionIfNeeded(
            expression: KtQualifiedExpression,
            calleeExpression: KtExpression,
            context: BindingContext,
            conversion: Conversion
    ) {
        val newName = conversion.replacementName

        val safeExpression = expression as? KtSafeQualifiedExpression
        val notNullType = expression.receiverExpression.getType(context)?.let { TypeUtils.isNullableType(it) } == false
        val defaultRange = TextRange(expression.operationTokenNode.startOffset, calleeExpression.endOffset)
                .shiftRight(-expression.startOffset)
        if (newName != null && (notNullType || safeExpression != null)) {
            val fixes = listOf(RenameUselessCallFix(newName)) + listOfNotNull(safeExpression?.let {
                IntentionWrapper(ReplaceWithDotCallFix(safeExpression), safeExpression.containingKtFile)
            })
            val descriptor = holder.manager.createProblemDescriptor(
                    expression,
                    defaultRange,
                    "Call on not-null type may be reduced",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly,
                    *fixes.toTypedArray()
            )
            holder.registerProblem(descriptor)
        }
        else if (notNullType) {
            val descriptor = holder.manager.createProblemDescriptor(
                    expression,
                    defaultRange,
                    "Useless call on not-null type",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    isOnTheFly,
                    RemoveUselessCallFix()
            )
            holder.registerProblem(descriptor)
        }
        else if (safeExpression != null) {
            holder.registerProblem(
                    safeExpression.operationTokenNode.psi,
                    "This call is useless with ?.",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    IntentionWrapper(ReplaceWithDotCallFix(safeExpression), safeExpression.containingKtFile)
            )
        }
    }
}

