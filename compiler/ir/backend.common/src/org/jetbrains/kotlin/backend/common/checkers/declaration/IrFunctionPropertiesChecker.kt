/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.render

internal object IrFunctionPropertiesChecker : IrFunctionChecker {
    override fun check(
        declaration: IrFunction,
        context: CheckerContext,
    ) {
        if (declaration is IrSimpleFunction) {
            val property = declaration.correspondingPropertySymbol?.owner
            if (property != null && property.getter != declaration && property.setter != declaration) {
                context.error(declaration, "Orphaned property getter/setter ${declaration.render()}")
            }
        }
    }
}