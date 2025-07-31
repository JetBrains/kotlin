/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.util.constructedClass

object IrConstructorReceiverChecker : IrElementChecker<IrFunction>(IrFunction::class) {
    override fun check(element: IrFunction, context: CheckerContext) {
        if (element !is IrConstructor) return
        if (!element.constructedClass.isInner && element.dispatchReceiverParameter != null) {
            context.error(element, "Constructors of non-inner classes can't have dispatch receiver parameters")
        }
        if (element.parameters.any { it.kind == IrParameterKind.ExtensionReceiver }) {
            context.error(element, "Constructors can't have extension receiver parameters")
        }
    }
}
