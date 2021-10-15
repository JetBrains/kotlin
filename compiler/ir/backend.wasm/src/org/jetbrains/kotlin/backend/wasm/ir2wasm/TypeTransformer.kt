/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.utils.hasWasmForeignAnnotation
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.wasm.ir.*

class WasmTypeTransformer(
    val context: WasmBaseCodegenContext,
    val builtIns: IrBuiltIns
) {
    val symbols = context.backendContext.wasmSymbols

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

    fun IrType.toWasmGcRefType(): WasmType =
        WasmRefNullType(WasmHeapType.Type(context.referenceGcType(getRuntimeClass?.symbol ?: builtIns.anyClass)))

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
                WasmExternRef

            // Value will not be created. Just using a random Wasm type.
            builtIns.nothingType ->
                WasmExternRef

            symbols.voidType ->
                error("Void type can't be used as a value")

            else -> {
                val klass = this.getClass()
                val ic = context.backendContext.inlineClassesUtils.getInlinedClass(this)

                if (klass != null && (klass.hasWasmForeignAnnotation() || klass.isExternal)) {
                    WasmAnyRef
                } else if (klass != null && isBuiltInWasmRefType(this)) {
                    when (val name = klass.name.identifier) {
                        "anyref" -> WasmAnyRef
                        "eqref" -> WasmEqRef
                        "dataref" -> WasmRefNullType(WasmHeapType.Simple.Data)
                        "i31ref" -> WasmI31Ref
                        "funcref" -> WasmRefNullType(WasmHeapType.Simple.Func)
                        else -> error("Unknown reference type $name")
                    }
                } else if (ic != null) {
                    context.backendContext.inlineClassesUtils.getInlineClassUnderlyingType(ic).toWasmValueType()
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

// Return null if upper bound is Any
private val IrTypeParameter.erasedUpperBound: IrClass?
    get() {
        // Pick the (necessarily unique) non-interface upper bound if it exists
        for (type in superTypes) {
            return type.erasedUpperBound ?: continue
        }

        return null
    }

val IrType.erasedUpperBound: IrClass?
    get() = when (val classifier = classifierOrNull) {
        is IrClassSymbol -> classifier.owner
        is IrTypeParameterSymbol -> classifier.owner.erasedUpperBound
        else -> throw IllegalStateException()
    }

val IrType.getRuntimeClass: IrClass?
    get() = erasedUpperBound.let {
        if (it?.isInterface == true) null
        else it
    }
