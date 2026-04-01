/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.wasm.ir.*

class WasmTypeTransformer(
    val backendContext: WasmBackendContext,
    val typeCodegenContext: WasmTypeCodegenContext,
) {
    private val builtIns: IrBuiltIns = backendContext.irBuiltIns
    private val symbols = backendContext.wasmSymbols

    fun IrType.hasWasmResultType(): Boolean =
        this != builtIns.unitType && this != builtIns.nothingType

    fun IrType.toWasmResultType(): WasmType? =
        hasWasmResultType().ifTrue { toWasmValueType() }

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
        WasmRefNullType(typeCodegenContext.referenceHeapType(getRuntimeClass(backendContext.irBuiltIns).symbol))

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

    private val irBuiltInToWasmType: HashMap<IrType, WasmType> = hashMapOf(
        builtIns.booleanType to WasmI32,
        builtIns.byteType to WasmI32,
        builtIns.shortType to WasmI32,
        builtIns.intType to WasmI32,
        builtIns.charType to WasmI32,

        builtIns.longType to WasmI64,

        builtIns.floatType to WasmF32,

        builtIns.doubleType to WasmF64,

        builtIns.nothingNType to WasmRefNullrefType,

        builtIns.nothingType to WasmAnyRef, // Value will not be created. Just using a random Wasm type.
    )

    fun IrType.toWasmValueType(): WasmType {
        irBuiltInToWasmType[this]?.let { return it }

        if (this == symbols.voidType) {
            error("Void type can't be used as a value")
        }

        val klass = this.erasedUpperBound
        return if (klass.isExternal) {
            if (klass.name.identifier != "JsStringRef") {
                WasmExternRef
            } else {
                WasmRefType(WasmHeapType.Simple.Extern)
            }
        } else if (isBuiltInWasmRefType(this)) {
            when (val name = klass.name.identifier) {
                "anyref" -> WasmAnyRef
                "eqref" -> WasmEqRef
                "structref" -> WasmRefNullType(WasmHeapType.Simple.Struct)
                "i31ref" -> WasmI31Ref
                "funcref" -> WasmRefNullType(WasmHeapType.Simple.Func)
                else -> error("Unknown reference type $name")
            }
        } else {
            val ic = backendContext.inlineClassesUtils.getInlinedClass(this)
            if (ic != null) {
                backendContext.inlineClassesUtils.getInlineClassUnderlyingType(ic).toWasmValueType()
            } else {
                this.toWasmGcRefType()
            }
        }
    }
}

private val internalReftypesFqName: FqName = FqName("kotlin.wasm.internal.reftypes")

fun isBuiltInWasmRefType(type: IrType): Boolean {
    return type.classOrNull?.owner?.packageFqName == internalReftypesFqName
}

fun isExternalType(type: IrType): Boolean =
    type.erasedUpperBound.isExternal

fun IrType.getRuntimeClass(irBuiltIns: IrBuiltIns): IrClass =
    erasedUpperBound.takeIf { !it.isInterface } ?: irBuiltIns.anyClass.owner