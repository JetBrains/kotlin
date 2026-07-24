/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ValueClassBackendAgnosticApi
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.inlineClassRepresentation
import org.jetbrains.kotlin.ir.types.IrSimpleType

/**
 * Retrieves the underlying type of inline class.
 *
 * Retrieves the underlying type of the [irClass] if the class is an inline class or
 * computes the underlying type if [treatCompatibleFullValueClassesAsInline] is `true` and
 * the class is a compatible full value class.
 *
 * See [ValueClassRepresentation] documentation for more details about value class types and their compatibility.
 *
 * @return The underlying type of the inline class, if it exists, otherwise throws an error.
 */
@ValueClassBackendAgnosticApi
fun getInlineClassUnderlyingType(irClass: IrClass, treatCompatibleFullValueClassesAsInline: Boolean): IrSimpleType {
    val representation = irClass.inlineClassRepresentation(treatCompatibleFullValueClassesAsInline)
        ?: error("Not an inline class: ${irClass.render()}")
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
