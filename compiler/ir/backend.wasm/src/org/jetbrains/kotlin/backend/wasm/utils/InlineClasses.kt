/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.ir.backend.js.utils.erase
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullable

/**
 * Returns inline class for given class or null of type is not inlined
 */
fun IrType.getWasmInlinedClass(): IrClass? {
    if (this is IrSimpleType) {
        // TODO: Make inlining less strict
        if (this.isNullable()) return null
        val erased = erase(this) ?: return null
        if (erased.isInline) {
            return erased
        }
    }
    return null
}

fun IrType.isInlinedWasm(): Boolean = this.getWasmInlinedClass() != null
