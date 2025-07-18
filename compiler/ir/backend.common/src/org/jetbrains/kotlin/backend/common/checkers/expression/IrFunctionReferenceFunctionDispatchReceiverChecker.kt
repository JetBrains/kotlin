/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.checkFunctionDispatchReceiver
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference

internal object IrFunctionReferenceFunctionDispatchReceiverChecker : IrElementChecker<IrFunctionReference>(IrFunctionReference::class) {
    override fun check(
        element: IrFunctionReference,
        context: CheckerContext,
    ) {
        element.checkFunctionDispatchReceiver(element.symbol.owner, context)
    }
}