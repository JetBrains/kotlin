/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

object IrClassSuperTypesChecker : IrElementChecker<IrClass>(IrClass::class) {
    override fun check(element: IrClass, context: CheckerContext) {
        if (element.superTypes.isEmpty() && !(element.symbol == context.irBuiltIns.anyClass || element.symbol == context.irBuiltIns.nothingClass)) {
            context.error(element, "IrClass must have at least one supertype")
        }
    }
}
