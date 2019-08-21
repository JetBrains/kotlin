/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isInterface


fun WasmCodegenContext.transformType(irType: IrType): WasmValueType =
    when {
        irType.isBoolean() -> WasmI32
        irType.isByte() -> WasmI32
        irType.isShort() -> WasmI32
        irType.isInt() -> WasmI32
        irType.isLong() -> WasmI64
        irType.isChar() -> WasmI32
        irType.isFloat() -> WasmF32
        irType.isDouble() -> WasmF64
        irType.isString() || irType.isNullableString() -> WasmAnyRef

        // irType.isAny() || irType.isNullableAny() -> WasmAnyRef
        else -> WasmRef(getStructTypeName(irType.erasedUpperBound ?: backendContext.irBuiltIns.anyClass.owner))
    }

fun WasmCodegenContext.resultType(type: IrType): WasmValueType? {
    if (type.isUnit() || type.isNothing()) return null
    return transformType(type)
}

fun WasmCodegenContext.wasmFunctionType(function: IrSimpleFunction): WasmFunctionType {
    val irParameters = function.run {
        listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters
    }
    return WasmFunctionType(getFunctionTypeName(function), irParameters.map { transformType(it.type) }, resultType(function.returnType))
}


fun irFunctionToWasmFunctionType(function: IrFunction) {
    val irParameters = function.run {
        listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters
    }
}

fun WasmCodegenContext.wasmStructType(klass: IrClass): WasmStructType {
    val classMetadata: ClassMetadata = typeInfo.classes[klass] ?: error("Can't find class metadata for class ${klass.fqNameWhenAvailable}")

    return WasmStructType(
        getStructTypeName(klass),
        classMetadata.fields.map { it.owner }.map { WasmStructField(getGlobalName(it), transformType(it.type), true) }
    )
}


// Return null if upper bound is Any
val IrTypeParameter.erasedUpperBound: IrClass?
    get() {
        // Pick the (necessarily unique) non-interface upper bound if it exists
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
