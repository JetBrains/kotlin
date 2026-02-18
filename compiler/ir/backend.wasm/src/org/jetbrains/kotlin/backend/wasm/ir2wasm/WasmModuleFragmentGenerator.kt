/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.getInstanceFunctionForExternalObject
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.utils.findUnitGetInstanceFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class ModuleReferencedDeclarations {
    val referencedFunction = mutableSetOf<IdSignature>()
    val referencedGlobalVTable = mutableSetOf<IdSignature>()
    val referencedGlobalClassITable = mutableSetOf<IdSignature>()
    val referencedRttiGlobal = mutableSetOf<IdSignature>()
}

class WasmModuleFragmentGenerator(
    private val backendContext: WasmBackendContext,
    private val wasmModuleMetadataCache: WasmModuleMetadataCache,
    private val idSignatureRetriever: IdSignatureRetriever,
    private val allowIncompleteImplementations: Boolean,
    private val skipCommentInstructions: Boolean,
    private val skipLocations: Boolean,
) {
    fun generateModuleAsSingleFileFragment(
        irModuleFragment: IrModuleFragment,
    ): WasmCompiledFileFragment {
        val wasmFileFragment = WasmCompiledFileFragment()
        val wasmFileCodegenContext = WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever)
        generate(irModuleFragment, wasmFileCodegenContext)
        return wasmFileFragment
    }

    fun generateModuleAsSingleFileFragmentWithModuleImport(
        irModuleFragment: IrModuleFragment,
        moduleName: String,
        referencedDeclarations: ModuleReferencedDeclarations,
        referencedTypes: ModuleReferencedTypes?,
    ): Pair<WasmCompiledFileFragment, Boolean> {
        val wasmFileFragment = WasmCompiledFileFragment()
        val wasmFileCodegenContext =
            if (referencedTypes != null) WasmFileCodegenContextWithImportTrackedTypes(wasmFileFragment, idSignatureRetriever, moduleName, referencedDeclarations, referencedTypes)
            else WasmFileCodegenContextWithImport(wasmFileFragment, idSignatureRetriever, moduleName, referencedDeclarations)
        generate(irModuleFragment, wasmFileCodegenContext)
        return wasmFileFragment to wasmFileCodegenContext.declarationImported
    }

    fun generateModuleAsSingleFileFragmentWithModuleExport(
        irModuleFragment: IrModuleFragment,
        referencedDeclarations: ModuleReferencedDeclarations,
        referencedTypes: ModuleReferencedTypes?,
    ): WasmCompiledFileFragment {
        val wasmFileFragment = WasmCompiledFileFragment()
        val wasmFileCodegenContext =
            if (referencedTypes != null) WasmFileCodegenContextWithExportTrackedTypes(wasmFileFragment, idSignatureRetriever, referencedDeclarations, referencedTypes)
            else WasmFileCodegenContextWithExport(wasmFileFragment, idSignatureRetriever, referencedDeclarations)
        generate(irModuleFragment, wasmFileCodegenContext)
        return wasmFileFragment
    }

    private fun generate(irModuleFragment: IrModuleFragment, wasmFileCodegenContext: WasmFileCodegenContext) {
        val wasmModuleTypeTransformer = WasmModuleTypeTransformer(backendContext, wasmFileCodegenContext)
        for (irFile in irModuleFragment.files) {
            compileIrFile(
                irFile,
                backendContext,
                wasmModuleMetadataCache,
                allowIncompleteImplementations,
                wasmFileCodegenContext,
                wasmModuleTypeTransformer,
                skipCommentInstructions,
                skipLocations,
            )
        }
    }
}

internal fun compileIrFile(
    irFile: IrFile,
    backendContext: WasmBackendContext,
    idSignatureRetriever: IdSignatureRetriever,
    wasmModuleMetadataCache: WasmModuleMetadataCache,
    allowIncompleteImplementations: Boolean,
    skipCommentInstructions: Boolean,
    skipLocations: Boolean,
): WasmCompiledFileFragment {
    val wasmFileFragment = WasmCompiledFileFragment()
    val wasmFileCodegenContext = WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever)
    val wasmModuleTypeTransformer = WasmModuleTypeTransformer(backendContext, wasmFileCodegenContext)
    compileIrFile(
        irFile,
        backendContext,
        wasmModuleMetadataCache,
        allowIncompleteImplementations,
        wasmFileCodegenContext,
        wasmModuleTypeTransformer,
        skipCommentInstructions,
        skipLocations,
    )
    return wasmFileFragment
}

private fun compileIrFile(
    irFile: IrFile,
    backendContext: WasmBackendContext,
    wasmModuleMetadataCache: WasmModuleMetadataCache,
    allowIncompleteImplementations: Boolean,
    wasmFileCodegenContext: WasmFileCodegenContext,
    wasmModuleTypeTransformer: WasmModuleTypeTransformer,
    skipCommentInstructions: Boolean,
    skipLocations: Boolean,
) {
    val generator = DeclarationGenerator(
        backendContext,
        wasmFileCodegenContext,
        wasmModuleTypeTransformer,
        wasmModuleMetadataCache,
        allowIncompleteImplementations,
        skipCommentInstructions,
        skipLocations,
    )
    for (irDeclaration in irFile.declarations) {
        irDeclaration.acceptVoid(generator)
    }

    val fileContext = backendContext.getFileContext(irFile)
    fileContext.mainFunctionWrapper?.apply {
        wasmFileCodegenContext.addMainFunctionWrapper(symbol)
    }
    fileContext.testFunctionDeclarator?.apply {
        wasmFileCodegenContext.addTestFunDeclarator(symbol)
    }
    fileContext.closureCallExports.forEach { (exportSignature, function) ->
        wasmFileCodegenContext.addEquivalentFunction("<1>_$exportSignature", function.symbol)
    }
    fileContext.kotlinClosureToJsConverters.forEach { (exportSignature, function) ->
        wasmFileCodegenContext.addEquivalentFunction("<2>_$exportSignature", function.symbol)
    }
    fileContext.jsClosureCallers.forEach { (exportSignature, function) ->
        wasmFileCodegenContext.addEquivalentFunction("<3>_$exportSignature", function.symbol)
    }
    fileContext.jsToKotlinClosures.forEach { (exportSignature, function) ->
        wasmFileCodegenContext.addEquivalentFunction("<4>_$exportSignature", function.symbol)
    }
    fileContext.objectInstanceFieldInitializer?.apply {
        wasmFileCodegenContext.addObjectInstanceFieldInitializer(symbol)
    }
    fileContext.nonConstantFieldInitializer?.apply {
        wasmFileCodegenContext.addNonConstantFieldInitializers(symbol)
    }

    fileContext.classAssociatedObjects.forEach { (klass, associatedObjects) ->
        val associatedObjectsInstanceGetters = associatedObjects.map { (key, obj) ->
            obj.objectGetInstanceFunction?.let {
                AssociatedObjectBySymbols(key.symbol, it.symbol, false)
            } ?: obj.getInstanceFunctionForExternalObject?.let {
                AssociatedObjectBySymbols(key.symbol, it.symbol, true)
            } ?: error("Could not find instance getter for $obj")
        }
        wasmFileCodegenContext.addClassAssociatedObjects(klass.symbol, associatedObjectsInstanceGetters)
    }

    fileContext.jsModuleAndQualifierReferences.forEach { reference ->
        wasmFileCodegenContext.addJsModuleAndQualifierReferences(reference)
    }

    backendContext.defineBuiltinSignatures(irFile, wasmFileCodegenContext)
}

private fun WasmBackendContext.defineBuiltinSignatures(irFile: IrFile, wasmFileCodegenContext: WasmFileCodegenContext) {
    val throwableClass = irBuiltIns.throwableClass.takeIf {
        irFile == it.owner.fileOrNull
    }

    val kotlinAnyClass = irBuiltIns.anyClass.takeIf {
        irFile == it.owner.fileOrNull
    }

    val tryGetAssociatedObjectFunction = wasmSymbols.tryGetAssociatedObject.takeIf {
        irFile == it.owner.fileOrNull
    }

    val jsToKotlinAnyAdapter: IrFunctionSymbol? = if (isWasmJsTarget) {
        wasmSymbols.jsRelatedSymbols.jsInteropAdapters.jsToKotlinAnyAdapter.takeIf {
            irFile == it.owner.fileOrNull
        }
    } else {
        null
    }

    val jsToKotlinStringAdapter: IrFunctionSymbol? = if (isWasmJsTarget) {
        wasmSymbols.jsRelatedSymbols.jsInteropAdapters.jsToKotlinStringAdapter.takeIf {
            irFile == it.owner.fileOrNull
        }
    } else {
        null
    }

    val unitGetInstance = findUnitGetInstanceFunction().takeIf {
        irFile == it.fileOrNull
    }

    val runRootSuites = wasmSymbols.runRootSuites?.takeIf {
        irFile == it.owner.fileOrNull
    }

    val createString = wasmSymbols.createString.takeIf {
        irFile == it.owner.fileOrNull
    }

    val registerModuleDescriptor = wasmSymbols.registerModuleDescriptor.takeIf {
        irFile == it.owner.fileOrNull
    }

    wasmFileCodegenContext.defineBuiltinIdSignatures(
        throwable = throwableClass,
        kotlinAny = kotlinAnyClass,
        tryGetAssociatedObject = tryGetAssociatedObjectFunction,
        jsToKotlinAnyAdapter = jsToKotlinAnyAdapter,
        jsToKotlinStringAdapter = jsToKotlinStringAdapter,
        unitGetInstance = unitGetInstance?.symbol,
        runRootSuites = runRootSuites,
        createString = createString,
        registerModuleDescriptor = registerModuleDescriptor,
    )
}
