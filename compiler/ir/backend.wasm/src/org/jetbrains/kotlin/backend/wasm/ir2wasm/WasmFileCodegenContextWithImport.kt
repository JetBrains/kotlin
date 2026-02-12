/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.wasm.ir.*

abstract class WasmFileCodegenContextWithImportBase(
    wasmFileFragment: WasmCompiledFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
    private val moduleName: String,
) : WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever) {

    protected fun createFunctionImport(declaration: IrFunctionSymbol, tag: String) {
        val functionTypeSymbol = referenceFunctionHeapType(declaration)
        defineFunction(
            declaration,
            WasmFunction.Imported(
                name = declaration.owner.fqNameWhenAvailable.toString(),
                type = functionTypeSymbol,
                importPair = WasmImportDescriptor(moduleName, WasmSymbol("${WasmServiceImportExportKind.FUNC.prefix}$tag"))
            )
        )
    }

    protected fun createVTableImport(declaration: IrClassSymbol, tag: String) {
        val global = WasmGlobal(
            name = "<classVTable>",
            type = WasmRefType(VTableHeapTypeSymbol(declaration.getReferenceKey())),
            isMutable = false,
            init = emptyList(),
            importPair = WasmImportDescriptor(moduleName, WasmSymbol("${WasmServiceImportExportKind.VTABLE.prefix}$tag"))
        )
        defineGlobalVTable(irClass = declaration, wasmGlobal = global)
    }

    protected fun createITableImport(declaration: IrClassSymbol, tag: String) {
        val global = WasmGlobal(
            name = "<classITable>",
            type = WasmRefType(Synthetics.HeapTypes.wasmAnyArrayType),
            isMutable = false,
            init = emptyList(),
            importPair = WasmImportDescriptor(moduleName, WasmSymbol("${WasmServiceImportExportKind.ITABLE.prefix}$tag"))
        )
        defineGlobalClassITable(irClass = declaration, wasmGlobal = global)
    }

    protected fun createRTTIImport(declaration: IrClassSymbol, superType: IrClassSymbol?, tag: String) {
        val rttiGlobal = WasmGlobal(
            name = "${declaration.owner.fqNameWhenAvailable}_rtti",
            type = WasmRefType(Synthetics.HeapTypes.rttiType),
            isMutable = false,
            init = emptyList(),
            importPair = WasmImportDescriptor(moduleName, WasmSymbol("${WasmServiceImportExportKind.RTTI.prefix}$tag"))
        )
        defineRttiGlobal(global = rttiGlobal, irClass = declaration, irSuperClass = superType)
    }


    override fun handleGlobalField(declaration: IrFieldSymbol): Boolean = true

    override fun addObjectInstanceFieldInitializer(initializer: IrFunctionSymbol) {
    }

    override fun addNonConstantFieldInitializers(initializer: IrFunctionSymbol) {
    }

    override fun addMainFunctionWrapper(mainFunctionWrapper: IrFunctionSymbol) {
    }

    override fun addTestFunDeclarator(testFunctionDeclarator: IrFunctionSymbol) {
    }

    override fun addEquivalentFunction(key: String, function: IrFunctionSymbol) {
    }

    override fun addClassAssociatedObjects(klass: IrClassSymbol, associatedObjectsGetters: List<AssociatedObjectBySymbols>) {
    }

    override fun addJsModuleAndQualifierReferences(reference: JsModuleAndQualifierReference) {
    }
}

open class WasmFileCodegenContextWithImport(
    wasmFileFragment: WasmCompiledFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
    moduleName: String,
    private val moduleReferencedDeclarations: ModuleReferencedDeclarations,
) : WasmFileCodegenContextWithImportBase(wasmFileFragment, idSignatureRetriever, moduleName) {

    var declarationImported: Boolean = false
        private set

    override fun handleFunctionWithImport(declaration: IrFunctionSymbol): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        if (signature !in moduleReferencedDeclarations.referencedFunction) return true
        createFunctionImport(declaration, signature.toString())
        declarationImported = true
        return true
    }

    override fun handleVTableWithImport(declaration: IrClassSymbol): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        if (signature !in moduleReferencedDeclarations.referencedGlobalVTable) return true
        createVTableImport(declaration, signature.toString())
        declarationImported = true
        return true
    }

    override fun handleClassITableWithImport(declaration: IrClassSymbol): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        if (signature !in moduleReferencedDeclarations.referencedGlobalClassITable) return true
        createITableImport(declaration, signature.toString())
        declarationImported = true
        return true
    }

    override fun handleRTTIWithImport(declaration: IrClassSymbol, superType: IrClassSymbol?): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        if (signature !in moduleReferencedDeclarations.referencedRttiGlobal) return true
        createRTTIImport(declaration, superType, signature.toString())
        declarationImported = true
        return true
    }
}

open class WasmFileCodegenContextWithAllImport(
    wasmFileFragment: WasmCompiledFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
    moduleName: String,
) : WasmFileCodegenContextWithImportBase(wasmFileFragment, idSignatureRetriever, moduleName) {

    override fun handleFunctionWithImport(declaration: IrFunctionSymbol): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        createFunctionImport(declaration, signature.toString())
        return true
    }

    override fun handleVTableWithImport(declaration: IrClassSymbol): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        createVTableImport(declaration, signature.toString())
        return true
    }

    override fun handleClassITableWithImport(declaration: IrClassSymbol): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        createITableImport(declaration, signature.toString())
        return true
    }

    override fun handleRTTIWithImport(declaration: IrClassSymbol, superType: IrClassSymbol?): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        createRTTIImport(declaration, superType, signature.toString())
        return true
    }
}