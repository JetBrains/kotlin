/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.WasmFunction
import org.jetbrains.kotlin.wasm.ir.WasmGlobal

class WasmFileCodegenContextWithExport(
    wasmFileFragment: WasmCompiledFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
) : WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever) {
    override fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        super.defineFunction(irFunction, wasmFunction)
        val owner = irFunction.owner
        if (owner.isEffectivelyPrivate()) return
        val signature = idSignatureRetriever.declarationSignature(owner)
        addExport(
            WasmExport.Function(
                field = wasmFunction,
                name = "$FunctionImportPrefix$signature"
            )
        )
    }

    override fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        super.defineGlobalVTable(irClass, wasmGlobal)
        exportDeclarationGlobal(irClass.owner, TypeGlobalImportPrefix.VTABLE, wasmGlobal)
    }

    override fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        super.defineGlobalClassITable(irClass, wasmGlobal)
        exportDeclarationGlobal(irClass.owner, TypeGlobalImportPrefix.ITABLE, wasmGlobal)
    }

    override fun defineRttiGlobal(global: WasmGlobal, irClass: IrClassSymbol, irSuperClass: IrClassSymbol?) {
        super.defineRttiGlobal(global, irClass, irSuperClass)
        exportDeclarationGlobal(irClass.owner, TypeGlobalImportPrefix.RTTI, global)
    }

    private fun exportDeclarationGlobal(declaration: IrDeclarationWithVisibility, prefix: TypeGlobalImportPrefix, global: WasmGlobal) {
        if (declaration.isEffectivelyPrivate()) return
        val signature = idSignatureRetriever.declarationSignature(declaration)
        addExport(
            WasmExport.Global(
                name = "${prefix.prefix}$signature",
                field = global
            )
        )
    }
}