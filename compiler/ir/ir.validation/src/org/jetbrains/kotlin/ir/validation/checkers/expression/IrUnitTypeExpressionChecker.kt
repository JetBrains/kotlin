/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.expression

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.validation.checkers.ensureTypeIs

object IrUnitTypeExpressionChecker : IrElementChecker<IrExpression>(IrExpression::class) {
    override fun check(element: IrExpression, context: CheckerContext) {
        when (element) {
            is IrSetValue, is IrSetField,
            is IrLoop,
            is IrDelegatingConstructorCall, is IrInstanceInitializerCall,
                -> element.ensureTypeIs(context.irBuiltIns.unitType, context)
        }
    }
}