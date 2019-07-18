/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirWhenSubject
import org.jetbrains.kotlin.fir.builder.generateContainsOperation
import org.jetbrains.kotlin.fir.builder.generateLazyLogicalOperation
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
    fun toFirWhenCondition(session: FirSession, subject: FirWhenSubject): FirExpression {
        var firCondition: FirExpression? = null
        for (condition in conditions) {
            val firConditionElement = condition.toFirWhenCondition(session, subject)
            firCondition = when (firCondition) {
                null -> firConditionElement
                else -> firCondition.generateLazyLogicalOperation(session, firConditionElement, false, null)
            }
        }
        return firCondition!!
    }

    private fun FirExpression.toFirWhenCondition(session: FirSession, subject: FirWhenSubject): FirExpression {
        val firSubjectExpression = FirWhenSubjectExpressionImpl(session, null, subject)
        return when (this) {
            is FirOperatorCallImpl -> {
                this.apply {
                    arguments.add(0, firSubjectExpression)
                }
            }
            is FirFunctionCall -> {
                val firExpression = this.explicitReceiver!!
                val isNegate = this.calleeReference.name == OperatorNameConventions.NOT
                firExpression.generateContainsOperation(session, firSubjectExpression, isNegate, null, null)
            }
            is FirTypeOperatorCallImpl -> {
                this.apply {
                    arguments += firSubjectExpression
                }
            }
            else -> {
                FirErrorExpressionImpl(session, null, "Unsupported when condition: ${this.javaClass}")
            }
        }
    }

    fun toFirWhenConditionWithoutSubject(session: FirSession): FirExpression {
        val condition = conditions.first()
        return when (condition) {
            is FirOperatorCallImpl -> condition.arguments.first()
            is FirFunctionCall -> condition.explicitReceiver!!
            is FirTypeOperatorCallImpl -> condition
            else -> FirErrorExpressionImpl(session, null, "Unsupported when condition: ${condition.javaClass}")
        }
    }
}