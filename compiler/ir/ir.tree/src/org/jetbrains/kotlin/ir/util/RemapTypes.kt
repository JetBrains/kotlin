/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrTypeTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Modifies the node in place, applying [typeRemapper] to all its [IrType]-containing fields.
 */
fun IrElement.remapTypes(typeRemapper: TypeRemapper) {
    acceptVoid(
        object : IrTypeTransformerVoid() {
            override fun <Type : IrType?> transformTypeRecursively(container: IrElement, type: Type): Type {
                @Suppress("UNCHECKED_CAST")
                return type?.let { typeRemapper.remapType(it) } as Type
            }
        }
    )
}
