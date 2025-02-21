/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.util.constructedClass

internal object IrConstructorReceiverChecker : IrFunctionChecker {
    override fun check(
        declaration: IrFunction,
        context: CheckerContext,
    ) {
        if (declaration !is IrConstructor) return
        if (!declaration.constructedClass.isInner && declaration.dispatchReceiverParameter != null) {
            context.error(declaration, "Constructors of non-inner classes can't have dispatch receiver parameters")
        }
        if (declaration.parameters.any { it.kind == IrParameterKind.ExtensionReceiver }) {
            context.error(declaration, "Constructors can't have extension receiver parameters")
        }
    }
}
