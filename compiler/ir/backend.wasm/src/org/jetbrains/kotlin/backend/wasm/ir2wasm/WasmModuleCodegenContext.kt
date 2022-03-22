/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.wasm.ir.*

/**
 * Interface for generating WebAssembly module.
 */
interface WasmModuleCodegenContext : WasmBaseCodegenContext {
    fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction)
    fun defineGlobalField(irField: IrFieldSymbol, wasmGlobal: WasmGlobal)
    fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal)
    fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal)
    fun defineGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration)
    fun defineVTableGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration)
    fun defineFunctionType(irFunction: IrFunctionSymbol, wasmFunctionType: WasmFunctionType)
    fun addJsFun(importName: String, jsCode: String)

    fun registerInitFunction(wasmFunction: WasmFunction, priority: String)
    fun addExport(wasmExport: WasmExport<*>)

    fun generateTypeInfo(irClass: IrClassSymbol, typeInfo: ConstantDataElement)
}