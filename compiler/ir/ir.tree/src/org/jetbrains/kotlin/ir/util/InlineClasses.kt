/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType

fun IrBuiltIns.getInlineClassUnderlyingType(irClass: IrClass): IrType =
    if (irClass.isSealedInline) anyNType
    else irClass.inlineClassRepresentation?.underlyingType
        ?: error("Not an inline class: ${irClass.render()}")

fun getInlineClassBackingField(irClass: IrClass): IrField {
    for (declaration in irClass.declarations) {
        if (declaration is IrField && !declaration.isStatic)
            return declaration

        if (declaration is IrProperty) {
            val backingField = declaration.backingField
            if (backingField != null && !backingField.isStatic) {
                return backingField
            }
        }
    }
    error("Inline class has no field: ${irClass.fqNameWhenAvailable}")
}
