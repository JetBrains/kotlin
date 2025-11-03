/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.expression

import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

object IrTypeOperatorRedundancyChecker : IrElementChecker<IrTypeOperatorCall>(IrTypeOperatorCall::class) {
    override fun check(element: IrTypeOperatorCall, context: CheckerContext) {
        if (element.operator == IrTypeOperator.IMPLICIT_CAST) {
            if (element.type == element.argument.type) {
                context.error(element, "Redundant IMPLICIT_CAST: ${element.render()}")
            }
        }
    }
}