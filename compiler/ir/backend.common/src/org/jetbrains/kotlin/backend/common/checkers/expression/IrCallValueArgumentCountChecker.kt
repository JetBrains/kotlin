/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.parentAsClass

object IrCallValueArgumentCountChecker : IrElementChecker<IrFunctionAccessExpression>(IrFunctionAccessExpression::class) {
    override fun check(element: IrFunctionAccessExpression, context: CheckerContext) {
        val function = element.symbol.owner

        if (element.arguments.size != function.parameters.size) {
            context.error(
                element, "The call provides ${element.arguments.size} argument(s) " +
                        "but the called function has ${function.parameters.size} parameter(s)"
            )
        }
    }
}