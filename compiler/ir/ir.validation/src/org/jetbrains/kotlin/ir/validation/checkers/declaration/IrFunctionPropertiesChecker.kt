/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

object IrFunctionPropertiesChecker : IrElementChecker<IrSimpleFunction>(IrSimpleFunction::class) {
    override fun check(element: IrSimpleFunction, context: CheckerContext) {
        val property = element.correspondingPropertySymbol?.owner
        if (property != null && property.getter != element && property.setter != element) {
            context.error(element, "Orphaned property getter/setter ${element.render()}")
        }
    }
}