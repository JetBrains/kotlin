/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

/**
 * Interface for generating WebAssembly module.
 */
interface WasmModuleCodegenContext : WasmBaseCodegenContext {
    fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction)
    fun defineGlobal(irField: IrFieldSymbol, wasmGlobal: WasmGlobal)
    fun defineStructType(irClass: IrClassSymbol, wasmStructType: WasmStructType)
    fun defineFunctionType(irFunction: IrFunctionSymbol, wasmFunctionType: WasmFunctionType)

    fun setStartFunction(wasmFunction: WasmFunction)
    fun addExport(wasmExport: WasmExport)

    fun registerVirtualFunction(irFunction: IrSimpleFunctionSymbol)
    fun registerInterface(irInterface: IrClassSymbol)
    fun registerClass(irClass: IrClassSymbol)

    fun generateTypeInfo(irClass: IrClassSymbol, typeInfo: ConstantDataElement)
}