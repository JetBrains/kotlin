/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.utils.erasedUpperBound
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.wasm.ir.*

class WasmTypeTransformer(
    val backendContext: WasmBackendContext,
    val wasmFileCodegenContext: WasmFileCodegenContext,
) {
    private val builtIns: IrBuiltIns = backendContext.irBuiltIns
    private val symbols = backendContext.wasmSymbols

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
            // TODO: Lower blocks with Nothing type?
            builtIns.nothingType ->
                WasmUnreachableType

            symbols.voidType ->
                null

            else ->
                toWasmValueType()
        }

    private fun IrType.toWasmGcRefType(): WasmType =
        WasmRefNullType(WasmHeapType.Type(wasmFileCodegenContext.referenceGcType(getRuntimeClass(backendContext.irBuiltIns).symbol)))

    fun IrType.toBoxedInlineClassType(): WasmType =
        toWasmGcRefType()

    fun IrType.toWasmFieldType(): WasmType =
        when (this) {
            builtIns.booleanType,
            builtIns.byteType ->
                WasmI8

            builtIns.shortType,
            builtIns.charType ->
                WasmI16

            else -> toWasmValueType()
        }

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

            builtIns.nothingNType ->
                WasmRefNullrefType

            // Value will not be created. Just using a random Wasm type.
            builtIns.nothingType ->
                WasmAnyRef

            symbols.voidType ->
                error("Void type can't be used as a value")

            else -> {
                val klass = this.erasedUpperBound ?: builtIns.anyClass.owner
                val ic = backendContext.inlineClassesUtils.getInlinedClass(this)

                if (klass.isExternal) {
                    WasmExternRef
                } else if (isBuiltInWasmRefType(this)) {
                    when (val name = klass.name.identifier) {
                        "anyref" -> WasmAnyRef
                        "eqref" -> WasmEqRef
                        "structref" -> WasmRefNullType(WasmHeapType.Simple.Struct)
                        "i31ref" -> WasmI31Ref
                        "funcref" -> WasmRefNullType(WasmHeapType.Simple.Func)
                        else -> error("Unknown reference type $name")
                    }
                } else if (ic != null) {
                    backendContext.inlineClassesUtils.getInlineClassUnderlyingType(ic).toWasmValueType()
                } else {
                    this.toWasmGcRefType()
                }
            }
        }
}

fun isBuiltInWasmRefType(type: IrType): Boolean {
    return type.classOrNull?.owner?.packageFqName == FqName("kotlin.wasm.internal.reftypes")
}

fun isExternalType(type: IrType): Boolean =
    type.erasedUpperBound?.isExternal ?: false

fun IrType.getRuntimeClass(irBuiltIns: IrBuiltIns): IrClass =
    erasedUpperBound?.takeIf { !it.isInterface } ?: irBuiltIns.anyClass.owner