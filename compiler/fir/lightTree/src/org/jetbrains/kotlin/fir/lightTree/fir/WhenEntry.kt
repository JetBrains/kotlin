/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.fir.FirWhenSubject
import org.jetbrains.kotlin.fir.builder.generateContainsOperation
import org.jetbrains.kotlin.fir.builder.generateLazyLogicalOperation
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirErrorExpressionImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirOperatorCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirTypeOperatorCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirWhenSubjectExpressionImpl
import org.jetbrains.kotlin.util.OperatorNameConventions

data class WhenEntry(
    val conditions: List<FirExpression>,
    val firBlock: FirBlock,
    val isElse: Boolean = false
) {
    fun toFirWhenCondition(subject: FirWhenSubject): FirExpression {
        var firCondition: FirExpression? = null
        for (condition in conditions) {
            val firConditionElement = condition.toFirWhenCondition(subject)
            firCondition = when (firCondition) {
                null -> firConditionElement
                else -> firCondition.generateLazyLogicalOperation(firConditionElement, false, null)
            }
        }
        return firCondition!!
    }

    private fun FirExpression.toFirWhenCondition(subject: FirWhenSubject): FirExpression {
        val firSubjectExpression = FirWhenSubjectExpressionImpl(null, subject)
        return when (this) {
            is FirOperatorCallImpl -> {
                this.apply {
                    arguments.add(0, firSubjectExpression)
                }
            }
            is FirFunctionCall -> {
                val firExpression = this.explicitReceiver!!
                val isNegate = this.calleeReference.name == OperatorNameConventions.NOT
                firExpression.generateContainsOperation(firSubjectExpression, isNegate, null, null)
            }
            is FirTypeOperatorCallImpl -> {
                this.apply {
                    arguments += firSubjectExpression
                }
            }
            else -> {
                FirErrorExpressionImpl(null, FirSimpleDiagnostic("Unsupported when condition: ${this.javaClass}", DiagnosticKind.Syntax))
            }
        }
    }

    fun toFirWhenConditionWithoutSubject(): FirExpression {
        return when (val condition = conditions.first()) {
            is FirOperatorCallImpl -> condition.arguments.first()
            else -> FirErrorExpressionImpl(null, FirSimpleDiagnostic("No expression in condition with expression", DiagnosticKind.Syntax))
        }
    }
}