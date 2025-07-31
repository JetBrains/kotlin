/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrDynamicType

internal object IrFunctionDispatchReceiverChecker : IrElementChecker<IrFunction>(IrFunction::class) {
    override fun check(element: IrFunction, context: CheckerContext) {
        if (element.dispatchReceiverParameter?.type is IrDynamicType) {
            context.error(element, "Dispatch receivers with 'dynamic' type are not allowed")
        }
    }
}