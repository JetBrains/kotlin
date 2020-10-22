/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.backend.wasm.WasmSymbols
import org.jetbrains.kotlin.ir.backend.js.InlineClassesUtils
import org.jetbrains.kotlin.ir.backend.js.utils.erase
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullable

class WasmInlineClassesUtils(private val wasmSymbols: WasmSymbols) : InlineClassesUtils {
    override fun isTypeInlined(type: IrType): Boolean {
        return getInlinedClass(type) != null
    }

    override fun getInlinedClass(type: IrType): IrClass? {
        if (type is IrSimpleType) {
            // TODO: Make inlining less strict
            if (type.isNullable()) return null
            val erased = erase(type) ?: return null
            if (isClassInlineLike(erased)) {
                return erased
            }
        }
        return null
    }

    override fun isClassInlineLike(klass: IrClass): Boolean {
        return klass.isInline || klass.hasWasmPrimitiveAnnotation()
    }

    override val boxIntrinsic: IrSimpleFunctionSymbol
        get() = wasmSymbols.boxIntrinsic

    override val unboxIntrinsic: IrSimpleFunctionSymbol
        get() = wasmSymbols.unboxIntrinsic
}