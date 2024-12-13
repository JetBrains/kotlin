/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.ensureTypeIs
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.makeNullable

internal object IrTypeOperatorTypeChecker : IrTypeOperatorChecker {
    override fun check(
        expression: IrTypeOperatorCall,
        context: CheckerContext,
    ) {
        // TODO: check IMPLICIT_NOTNULL's argument type.
        val operator = expression.operator
        val typeOperand = expression.typeOperand
        val naturalType = when (operator) {
            IrTypeOperator.CAST,
            IrTypeOperator.IMPLICIT_CAST,
            IrTypeOperator.IMPLICIT_NOTNULL,
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
            IrTypeOperator.IMPLICIT_INTEGER_COERCION,
            IrTypeOperator.SAM_CONVERSION,
            IrTypeOperator.IMPLICIT_DYNAMIC_CAST,
            IrTypeOperator.REINTERPRET_CAST ->
                typeOperand

            IrTypeOperator.SAFE_CAST ->
                typeOperand.makeNullable()

            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF ->
                context.irBuiltIns.booleanType
        }
        expression.ensureTypeIs(naturalType, context)
    }
}