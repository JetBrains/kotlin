/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.wasm.ir.*

fun getAllReferencedDeclarations(wasmCompiledFileFragment: WasmCompiledFileFragment): Set<IdSignature> {
    val signatures = mutableSetOf<IdSignature>()
    signatures.addAll(wasmCompiledFileFragment.functions.unbound.keys)
    signatures.addAll(wasmCompiledFileFragment.globalFields.unbound.keys)
    signatures.addAll(wasmCompiledFileFragment.globalVTables.unbound.keys)
    signatures.addAll(wasmCompiledFileFragment.globalClassITables.unbound.keys)
    wasmCompiledFileFragment.rttiElements?.let {
        signatures.addAll(it.globalReferences.unbound.keys)
    }
    return signatures
}

class WasmFileCodegenContextWithImport(
    wasmFileFragment: WasmCompiledFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
    private val moduleName: String,
    private val importDeclarations: Set<IdSignature>,
) : WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever) {
    override fun handleFunctionWithImport(declaration: IrFunctionSymbol): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        if (signature !in importDeclarations) return true
        val functionTypeSymbol = referenceFunctionType(declaration)
        defineFunction(
            declaration,
            WasmFunction.Imported(
                name = declaration.owner.fqNameWhenAvailable.toString(),
                type = functionTypeSymbol,
                importPair = WasmImportDescriptor(moduleName, WasmSymbol("$FunctionImportPrefix$signature"))
            )
        )
        return true
    }

    override fun handleVTableWithImport(declaration: IrClassSymbol): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        if (signature !in importDeclarations) return true
        val global = WasmGlobal(
            name = "<classVTable>",
            type = WasmRefType(WasmHeapType.Type(referenceVTableGcType(declaration))),
            isMutable = false,
            init = emptyList(),
            importPair = WasmImportDescriptor(moduleName, WasmSymbol("${TypeGlobalImportPrefix.VTABLE.prefix}$signature"))
        )
        defineGlobalVTable(irClass = declaration, wasmGlobal = global)
        return true
    }

    override fun handleClassITableWithImport(declaration: IrClassSymbol): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        if (signature !in importDeclarations) return true
        val global = WasmGlobal(
            name = "<classITable>",
            type = WasmRefType(WasmHeapType.Type(interfaceTableTypes.wasmAnyArrayType)),
            isMutable = false,
            init = emptyList(),
            importPair = WasmImportDescriptor(moduleName, WasmSymbol("${TypeGlobalImportPrefix.ITABLE.prefix}$signature"))
        )
        defineGlobalClassITable(irClass = declaration, wasmGlobal = global)
        return true
    }

    override fun handleRTTIWithImport(declaration: IrClassSymbol, superType: IrClassSymbol?): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        if (signature !in importDeclarations) return true
        val rttiGlobal = WasmGlobal(
            name = "${declaration.owner.fqNameWhenAvailable}_rtti",
            type = WasmRefType(WasmHeapType.Type(rttiType)),
            isMutable = false,
            init = emptyList(),
            importPair = WasmImportDescriptor(moduleName, WasmSymbol("${TypeGlobalImportPrefix.RTTI.prefix}$signature"))
        )
        defineRttiGlobal(global = rttiGlobal, irClass = declaration, irSuperClass = superType)
        return true
    }
}