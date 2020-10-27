/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.backend.wasm.utils.hasWasmForeignAnnotation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.getInlineClassUnderlyingType
import org.jetbrains.kotlin.ir.util.isInterface

class WasmTypeTransformer(
    val context: WasmBaseCodegenContext,
    val builtIns: IrBuiltIns
) {
    fun IrType.toWasmResultType(): WasmType? =
        when (this) {
            builtIns.unitType,
            builtIns.nothingType ->
                null

            else ->
                toWasmValueType()
        }

    fun IrType.toWasmBlockResultType(): WasmType? =
        when (this) {
            builtIns.unitType ->
                null

            // TODO: Lower blocks with Nothing type?
            builtIns.nothingType ->
                WasmUnreachableType

            else ->
                toWasmValueType()
        }

    fun IrType.toStructType(): WasmType =
        WasmRefNullType(WasmHeapType.Type(context.referenceStructType(erasedUpperBound?.symbol ?: builtIns.anyClass)))

    fun IrType.toBoxedInlineClassType(): WasmType =
        toStructType()

    fun IrType.toWasmValueType(): WasmType =
        when (this) {
            builtIns.booleanType,
            builtIns.byteType,
            builtIns.shortType,
            builtIns.intType,
            builtIns.charType ->
                WasmI32

            builtIns.longType ->
                WasmI64

            builtIns.floatType ->
                WasmF32

            builtIns.doubleType ->
                WasmF64

            builtIns.stringType ->
                WasmExternRef

            builtIns.nothingNType ->
                WasmExternRef

            // Value will not be created. Just using a random Wasm type.
            builtIns.nothingType ->
                WasmExternRef

            else -> {
                val klass = this.getClass()
                val ic = context.backendContext.inlineClassesUtils.getInlinedClass(this)

                if (klass != null && klass.hasWasmForeignAnnotation()) {
                    WasmExternRef
                } else if (ic != null) {
                    getInlineClassUnderlyingType(ic).toWasmValueType()
                } else {
                    this.toStructType()
                }
            }
        }
}


// Return null if upper bound is Any
val IrTypeParameter.erasedUpperBound: IrClass?
    get() {
        // Pick the (necessarily unique) non-interface upper bound if it exists
        // TODO check if it should be recursive
        for (type in superTypes) {
            return type.classOrNull?.owner ?: continue
        }

        return null
    }


val IrType.erasedUpperBound: IrClass?
    get() = when (val classifier = classifierOrNull) {
        is IrClassSymbol -> classifier.owner
        is IrTypeParameterSymbol -> classifier.owner.erasedUpperBound
        else -> throw IllegalStateException()
    }.let {
        if (it?.isInterface == true) null
        else it
    }
