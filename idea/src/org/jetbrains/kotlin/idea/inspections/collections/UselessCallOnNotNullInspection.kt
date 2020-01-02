/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
    override val uselessFqNames = mapOf(
        "kotlin.collections.orEmpty" to deleteConversion,
        "kotlin.sequences.orEmpty" to deleteConversion,
        "kotlin.text.orEmpty" to deleteConversion,
        "kotlin.text.isNullOrEmpty" to Conversion("isEmpty"),
        "kotlin.text.isNullOrBlank" to Conversion("isBlank")
    )

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
        val defaultRange =
            TextRange(expression.operationTokenNode.startOffset, calleeExpression.endOffset).shiftRight(-expression.startOffset)
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
        } else if (notNullType) {
            val descriptor = holder.manager.createProblemDescriptor(
                expression,
                defaultRange,
                "Useless call on not-null type",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                isOnTheFly,
                RemoveUselessCallFix()
            )
            holder.registerProblem(descriptor)
        } else if (safeExpression != null) {
            holder.registerProblem(
                safeExpression.operationTokenNode.psi,
                "This call is useless with ?.",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                IntentionWrapper(ReplaceWithDotCallFix(safeExpression), safeExpression.containingKtFile)
            )
        }
    }
}

