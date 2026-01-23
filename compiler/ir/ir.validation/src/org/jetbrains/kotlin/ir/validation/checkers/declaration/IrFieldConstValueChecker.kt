/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

object IrFieldConstValueChecker : IrElementChecker<IrField>(IrField::class) {
    override fun check(element: IrField, context: CheckerContext) {
        if (element.correspondingPropertySymbol?.owner?.isConst == true && element.initializer?.expression !is IrConst) {
            context.error(element, "Const field is not containing const expression. Got ${element.initializer?.render()}")
        }
    }
}