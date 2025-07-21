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

class WasmModuleFragmentGenerator(
    private val backendContext: WasmBackendContext,
    private val wasmModuleMetadataCache: WasmModuleMetadataCache,
    private val idSignatureRetriever: IdSignatureRetriever,
    private val allowIncompleteImplementations: Boolean,
    private val skipCommentInstructions: Boolean,
    private val useStringPool: Boolean,
) {
    fun generateModuleAsSingleFileFragment(
        irModuleFragment: IrModuleFragment,
    ): WasmCompiledFileFragment {
        val wasmFileFragment = WasmCompiledFileFragment(fragmentTag = null)
        val wasmFileCodegenContext = WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever)
        generate(irModuleFragment, wasmFileCodegenContext)
        return wasmFileFragment
    }

    fun generateModuleAsSingleFileFragmentWithIECImport(
        irModuleFragment: IrModuleFragment,
        moduleName: String,
        importDeclarations: Set<IdSignature>,
    ): WasmCompiledFileFragment {
        val wasmFileFragment = WasmCompiledFileFragment(fragmentTag = null)
        val wasmFileCodegenContext = WasmFileCodegenContextWithImport(wasmFileFragment, idSignatureRetriever, moduleName, importDeclarations)
        generate(irModuleFragment, wasmFileCodegenContext)
        return wasmFileFragment
    }

    fun generateModuleAsSingleFileFragmentWithIECExport(
        irModuleFragment: IrModuleFragment,
    ): WasmCompiledFileFragment {
        val wasmFileFragment = WasmCompiledFileFragment(fragmentTag = null)
        val wasmFileCodegenContext = WasmFileCodegenContextWithExport(wasmFileFragment, idSignatureRetriever)
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
                useStringPool,
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
    fragmentTag: String?,
    skipCommentInstructions: Boolean,
    useStringPool: Boolean,
): WasmCompiledFileFragment {
    val wasmFileFragment = WasmCompiledFileFragment(fragmentTag)
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
        useStringPool,
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
    useStringPool: Boolean,
) {
    val generator = DeclarationGenerator(
        backendContext,
        wasmFileCodegenContext,
        wasmModuleTypeTransformer,
        wasmModuleMetadataCache,
        allowIncompleteImplementations,
        skipCommentInstructions,
        useStringPool,
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

    val tryGetAssociatedObjectFunction = wasmSymbols.tryGetAssociatedObject.takeIf {
        irFile == it.owner.fileOrNull
    }

    val jsToKotlinAnyAdapter: IrFunctionSymbol?
    if (isWasmJsTarget) {
        jsToKotlinAnyAdapter = wasmSymbols.jsRelatedSymbols.jsInteropAdapters.jsToKotlinAnyAdapter.takeIf {
            irFile == it.owner.fileOrNull
        }
    } else {
        jsToKotlinAnyAdapter = null
    }

    val unitGetInstance = findUnitGetInstanceFunction().takeIf {
        irFile == it.fileOrNull
    }

    val runRootSuites = wasmSymbols.runRootSuites?.takeIf {
        irFile == it.owner.fileOrNull
    }

    wasmFileCodegenContext.defineBuiltinIdSignatures(
        throwable = throwableClass,
        tryGetAssociatedObject = tryGetAssociatedObjectFunction,
        jsToKotlinAnyAdapter = jsToKotlinAnyAdapter,
        unitGetInstance = unitGetInstance?.symbol,
        runRootSuites = runRootSuites,
    )
}
