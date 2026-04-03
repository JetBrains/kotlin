/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.inlineClassRepresentation
import org.jetbrains.kotlin.ir.types.IrSimpleType

/**
 * Retrieves the underlying type of an inline class.
 *
 * @param irClass The IrClass instance for which to retrieve the underlying type.

 * @param distinguishBasicAndExtended A boolean indicating whether to differentiate between basic and extended value class representations.
 *                                    If `true`, `ExtendedValueClassRepresentation` will not be considered as single-field compatible,
 *                                    regardless of the number of properties in the representation. If `false`, the compatibility
 *                                    for extended value classes depends on whether they have exactly one underlying property.
 *                                    `true` must be used for JVM, `false` for other backends.
 * @return The underlying type of the inline class if it exists, otherwise throws an error.
 */
fun getInlineClassUnderlyingType(irClass: IrClass, distinguishBasicAndExtended: Boolean): IrSimpleType {
    val representation = irClass.inlineClassRepresentation(distinguishBasicAndExtended) ?: error("Not an inline class: ${irClass.render()}")
    return representation.underlyingType
}

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
