/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isInterface


// TODO: Refactor
fun WasmCodegenContext.transformType(irType: IrType): WasmValueType {
    val typeTransformer = TypeTransformer(this, backendContext.irBuiltIns)
    return with(typeTransformer) { irType.toWasmValueType() }
}

// TODO: Refactor
fun WasmCodegenContext.resultType(type: IrType): WasmValueType? {
    if (type.isUnit() || type.isNothing()) return null
    return transformType(type)
}

class TypeTransformer(val context: WasmCodegenContext, val builtIns: IrBuiltIns) {
    fun IrType.toWasmResultType(): WasmValueType? =
        when (this) {
            builtIns.unitType,
            builtIns.nothingType ->
                null

            else ->
                toWasmValueType()
        }

    fun IrType.toWasmValueType(): WasmValueType =
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

            builtIns.stringType,
            builtIns.stringType.makeNullable() ->
                WasmAnyRef

            else ->
                WasmStructRef(
                    context.getStructTypeName(erasedUpperBound ?: builtIns.anyClass.owner)
                )
        }

    fun IrFunction.toWasmFunctionType(): WasmFunctionType =
        WasmFunctionType(
            fqNameWhenAvailable.toString(),
            parameterTypes = getEffectiveValueParameters().map { it.type.toWasmValueType() },
            resultType = returnType.toWasmResultType()
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
