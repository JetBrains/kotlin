/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.ensureTypeIs
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue

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