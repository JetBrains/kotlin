/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody

internal object IrExpressionBodyInFunctionChecker : IrElementChecker<IrFunction>(IrFunction::class) {
    override fun check(element: IrFunction, context: CheckerContext) {
        if (element.body is IrExpressionBody) {
            context.error(element, "IrFunction body cannot be of type IrExpressionBody. Use IrBlockBody instead.")
        }
    }
}