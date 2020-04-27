/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.backend.wasm.lower.WasmSignature
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType

interface WasmBaseCodegenContext {
    val backendContext: WasmBackendContext

    fun referenceFunction(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunction>
    fun referenceGlobal(irField: IrFieldSymbol): WasmSymbol<WasmGlobal>
    fun referenceStructType(irClass: IrClassSymbol): WasmSymbol<WasmStructType>
    fun referenceFunctionType(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunctionType>

    fun referenceClassId(irClass: IrClassSymbol): WasmSymbol<Int>
    fun referenceInterfaceId(irInterface: IrClassSymbol): WasmSymbol<Int>
    fun referenceVirtualFunctionId(irFunction: IrSimpleFunctionSymbol): WasmSymbol<Int>

    fun referenceSignatureId(signature: WasmSignature): WasmSymbol<Int>

    fun referenceStringLiteral(string: String): WasmSymbol<Int>

    fun transformType(irType: IrType): WasmValueType
    fun transformBoxedType(irType: IrType): WasmValueType
    fun transformValueParameterType(irValueParameter: IrValueParameter): WasmValueType
    fun transformResultType(irType: IrType): WasmValueType?
    fun transformBlockResultType(irType: IrType): WasmValueType?


    fun getStructFieldRef(field: IrField): WasmSymbol<Int>
    fun getClassMetadata(irClass: IrClassSymbol): ClassMetadata
}