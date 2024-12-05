/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.render

internal object IrTypeOperatorTypeOperandChecker : IrTypeOperatorChecker {
    override fun check(
        expression: IrTypeOperatorCall,
        context: CheckerContext,
    ) {
        val operator = expression.operator
        val typeOperand = expression.typeOperand
        if (operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT && !typeOperand.isUnit()) {
            context.error(expression, "typeOperand is ${typeOperand.render()}")
        }
    }
}