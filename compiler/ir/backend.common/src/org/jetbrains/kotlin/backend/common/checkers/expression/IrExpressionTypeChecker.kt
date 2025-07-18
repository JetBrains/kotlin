/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.render

object IrExpressionTypeChecker : IrElementChecker<IrExpression>(IrExpression::class) {
    override fun check(
        element: IrExpression,
        context: CheckerContext,
    ) {
        val type = element.type
        if (type is IrSimpleType) {
            if (!type.classifier.isBound) {
                context.error(element, "Type: ${type.render()} has unbound classifier")
            }
        }
    }
}