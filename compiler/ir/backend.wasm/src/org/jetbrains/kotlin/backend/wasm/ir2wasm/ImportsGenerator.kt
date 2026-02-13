/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.superClass
import org.jetbrains.kotlin.wasm.ir.WasmFunction
import org.jetbrains.kotlin.wasm.ir.WasmGlobal
import org.jetbrains.kotlin.wasm.ir.WasmImportDescriptor
import org.jetbrains.kotlin.wasm.ir.WasmRefType
import org.jetbrains.kotlin.wasm.ir.WasmSymbol

class ImportsGenerator(
    private val typeContext: WasmTypeCodegenContext,
    private val declarationContext: WasmDeclarationCodegenContext,
    private val moduleName: String,
) {
    fun generateFunctionImport(declaration: IrFunction) {
        val functionTypeSymbol = typeContext.referenceFunctionHeapType(declaration.symbol)
        val tag = typeContext.getDeclarationTag(declaration)

        declarationContext.defineFunction(
            declaration.symbol,
            WasmFunction.Imported(
                name = declaration.fqNameWhenAvailable.toString(),
                type = functionTypeSymbol,
                importPair = WasmImportDescriptor(moduleName, WasmSymbol("${WasmServiceImportExportKind.FUNC.prefix}$tag"))
            )
        )
    }

    fun generateClassImports(declaration: IrClass) {
        if (declaration.isInterface) return

        val symbol = declaration.symbol
        val tag = typeContext.getDeclarationTag(declaration)

        val vtableGlobal = WasmGlobal(
            name = "<classVTable>",
            type = WasmRefType(typeContext.referenceVTableHeapType(symbol)),
            isMutable = false,
            init = emptyList(),
            importPair = WasmImportDescriptor(moduleName, WasmSymbol("${WasmServiceImportExportKind.VTABLE.prefix}$tag"))
        )
        declarationContext.defineGlobalVTable(irClass = symbol, wasmGlobal = vtableGlobal)

        val iTableGlobal = WasmGlobal(
            name = "<classITable>",
            type = WasmRefType(Synthetics.HeapTypes.wasmAnyArrayType),
            isMutable = false,
            init = emptyList(),
            importPair = WasmImportDescriptor(moduleName, WasmSymbol("${WasmServiceImportExportKind.ITABLE.prefix}$tag"))
        )
        declarationContext.defineGlobalClassITable(irClass = symbol, wasmGlobal = iTableGlobal)

        val rttiGlobal = WasmGlobal(
            name = "${declaration.fqNameWhenAvailable}_rtti",
            type = WasmRefType(Synthetics.HeapTypes.rttiType),
            isMutable = false,
            init = emptyList(),
            importPair = WasmImportDescriptor(moduleName, WasmSymbol("${WasmServiceImportExportKind.RTTI.prefix}$tag"))
        )
        declarationContext.defineRttiGlobal(global = rttiGlobal, irClass = symbol, irSuperClass = declaration.superClass?.symbol)
    }
}