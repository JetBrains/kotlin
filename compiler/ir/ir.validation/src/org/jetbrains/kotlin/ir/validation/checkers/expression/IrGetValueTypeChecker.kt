/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.expression

import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.validation.checkers.ensureTypeIs

object IrGetValueTypeChecker : IrElementChecker<IrGetValue>(IrGetValue::class) {
    override fun check(element: IrGetValue, context: CheckerContext) {
        element.ensureTypeIs(element.symbol.owner.type, context)
    }
}