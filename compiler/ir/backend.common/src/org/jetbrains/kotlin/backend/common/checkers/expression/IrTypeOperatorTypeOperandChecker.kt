/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.render

internal object IrTypeOperatorTypeOperandChecker : IrElementChecker<IrTypeOperatorCall>(IrTypeOperatorCall::class) {
    override fun check(element: IrTypeOperatorCall, context: CheckerContext) {
        val operator = element.operator
        val typeOperand = element.typeOperand
        if (operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT && !typeOperand.isUnit()) {
            context.error(element, "typeOperand is ${typeOperand.render()}")
        }
    }
}