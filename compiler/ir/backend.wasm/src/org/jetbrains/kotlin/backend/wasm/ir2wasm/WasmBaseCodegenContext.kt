/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.wasm.ir.*

interface WasmBaseCodegenContext {
    val backendContext: WasmBackendContext

    val scratchMemAddr: WasmSymbol<Int>
    val scratchMemSizeInBytes: Int

    val stringPoolSize: WasmSymbol<Int>

    fun referenceFunction(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunction>
    fun referenceGlobalField(irField: IrFieldSymbol): WasmSymbol<WasmGlobal>
    fun referenceGlobalVTable(irClass: IrClassSymbol): WasmSymbol<WasmGlobal>
    fun referenceGlobalClassITable(irClass: IrClassSymbol): WasmSymbol<WasmGlobal>
    fun referenceGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration>
    fun referenceVTableGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration>
    fun referenceClassITableGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration>
    fun defineClassITableGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration)
    fun isAlreadyDefinedClassITableGcType(irClass: IrClassSymbol): Boolean
    fun referenceClassITableInterfaceSlot(irClass: IrClassSymbol): WasmSymbol<Int>
    fun defineClassITableInterfaceSlot(irClass: IrClassSymbol, slot: Int)
    fun referenceFunctionType(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunctionType>

    fun referenceClassId(irClass: IrClassSymbol): WasmSymbol<Int>
    fun referenceInterfaceId(irInterface: IrClassSymbol): WasmSymbol<Int>

    fun referenceStringLiteralAddressAndId(string: String): Pair<WasmSymbol<Int>, WasmSymbol<Int>>

    fun referenceConstantArray(resource: Pair<List<Long>, WasmType>): WasmSymbol<Int>

    fun transformType(irType: IrType): WasmType
    fun transformFieldType(irType: IrType): WasmType

    fun transformBoxedType(irType: IrType): WasmType
    fun transformValueParameterType(irValueParameter: IrValueParameter): WasmType
    fun transformResultType(irType: IrType): WasmType?
    fun transformBlockResultType(irType: IrType): WasmType?


    fun getStructFieldRef(field: IrField): WasmSymbol<Int>
    fun getClassMetadata(irClass: IrClassSymbol): ClassMetadata
    fun getInterfaceMetadata(irClass: IrClassSymbol): InterfaceMetadata
}