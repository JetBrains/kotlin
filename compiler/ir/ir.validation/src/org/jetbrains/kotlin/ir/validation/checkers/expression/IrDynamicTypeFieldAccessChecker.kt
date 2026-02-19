/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.expression

import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

/**
 * Makes sure that [IrDynamicType] is not used as receiver of IrFieldAccessExpression.
 */
object IrDynamicTypeFieldAccessChecker : IrElementChecker<IrFieldAccessExpression>(IrFieldAccessExpression::class) {
    override fun check(element: IrFieldAccessExpression, context: CheckerContext) {
        if (element.receiver?.type is IrDynamicType) {
            context.error(
                element,
                "IrFieldAccessExpression may not access fields using dynamic receiver. " +
                        "IrDynamicMemberExpression must be used instead.",
            )
        }
    }
}