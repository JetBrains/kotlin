/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.expression

import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.validation.checkers.ensureTypeIs

object IrNothingTypeExpressionChecker : IrElementChecker<IrExpression>(IrExpression::class) {
    override fun check(element: IrExpression, context: CheckerContext) {
        when (element) {
            is IrBreakContinue, is IrReturn, is IrThrow
                -> element.ensureTypeIs(context.irBuiltIns.nothingType, context)
        }
    }
}