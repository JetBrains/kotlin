/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ValueClassBackendAgnosticApi
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.inlineClassRepresentation
import org.jetbrains.kotlin.ir.types.IrSimpleType

/**
 * Retrieves the underlying type of inline class.
 *
 * @param irClass The [IrClass] instance for which to retrieve the underlying type.
 *
 * **Full** value classes are value classes described in [this KEEP](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0454-better-immutability-value-classes-MFVC.md).
 *
 * **Basic** value classes are [inline classes](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0104-inline-classes.md) and [jvm inline multi-field value classes](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0340-multi-field-value-classes.md)
 *
 * The overview of full value classes is that they are value classes without @JvmInline annotation on all backends, supporting one or multiple underlying fields.
 *
 * They are not optimized on JVM, regardless of the number of underlying fields. On other backends, they are optimized if there is only one underlying field.
 *
 * @param treatFullValueClassesWithOneFieldAsBasic A boolean indicating whether to treat full value classes with one underlying field as basic (inline class).
 *                                                 On JVM full value classes are not unboxed on the behalf of Kotlin compiler while `inline class`es/`@JvmInline value class`es are.
 *                                                 On other platforms there is no `@JvmInline` annotation and unboxing is done by the compiler in both basic and full value classes with a single field.
 *                                                 Therefore, full value classes with one field are actually preexisting value classes on other platforms.
 *                                                 `false` must be used for JVM, `true` for other backends.
 * @return The underlying type of the inline class if it exists, otherwise throws an error.
 */
@ValueClassBackendAgnosticApi
fun getInlineClassUnderlyingType(irClass: IrClass, treatFullValueClassesWithOneFieldAsBasic: Boolean): IrSimpleType {
    val representation = irClass.inlineClassRepresentation(treatFullValueClassesWithOneFieldAsBasic) ?: error("Not an inline class: ${irClass.render()}")
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
