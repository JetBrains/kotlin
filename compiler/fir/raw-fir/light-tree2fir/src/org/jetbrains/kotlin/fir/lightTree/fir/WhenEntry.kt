/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.fir.builder.generateLazyLogicalOperation
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.buildErrorExpression

data class WhenEntry(
    val conditions: List<FirExpression>,
    val firBlock: FirBlock,
    val node: LighterASTNode,
    val isElse: Boolean = false
) {
    fun toFirWhenCondition(): FirExpression {
        var firCondition: FirExpression? = null
        for (condition in conditions) {
            val firConditionElement = condition.toFirWhenCondition()
            firCondition = when (firCondition) {
                null -> firConditionElement
                else -> firCondition.generateLazyLogicalOperation(firConditionElement, false, null)
            }
        }
        return firCondition!!
    }

    private fun FirExpression.toFirWhenCondition(): FirExpression {
        return this
    }

    fun toFirWhenConditionWithoutSubject(): FirExpression {
        return when (val condition = conditions.firstOrNull()) {
            null -> buildErrorExpression(null, ConeSimpleDiagnostic("No expression in condition with expression", DiagnosticKind.Syntax))
            else -> condition
        }
    }
}
