/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class WasmModuleFragmentGenerator(
    private val backendContext: WasmBackendContext,
    private val wasmModuleMetadataCache: WasmModuleMetadataCache,
    private val idSignatureRetriever: IdSignatureRetriever,
    private val allowIncompleteImplementations: Boolean,
    private val skipCommentInstructions: Boolean,
) {
    fun generateModuleAsSingleFileFragment(irModuleFragment: IrModuleFragment): WasmCompiledFileFragment {
        val wasmFileFragment = WasmCompiledFileFragment(fragmentTag = null)
        val wasmFileCodegenContext = WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever)
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
            )
        }
        return wasmFileFragment
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
) {
    val generator = DeclarationGenerator(
        backendContext,
        wasmFileCodegenContext,
        wasmModuleTypeTransformer,
        wasmModuleMetadataCache,
        allowIncompleteImplementations,
        skipCommentInstructions,
    )
    for (irDeclaration in irFile.declarations) {
        irDeclaration.acceptVoid(generator)
    }

    val testFun = backendContext.testFunsPerFile[irFile]
    if (testFun != null) {
        wasmFileCodegenContext.defineTestFun(testFun.symbol)
    }

    val fileContext = backendContext.getFileContext(irFile)
    fileContext.mainFunctionWrapper?.apply {
        wasmFileCodegenContext.addMainFunctionWrapper(symbol)
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
            backendContext.mapping.objectToGetInstanceFunction[obj]?.let {
                AssociatedObjectBySymbols(key.symbol, it.symbol, false)
            } ?: backendContext.mapping.wasmExternalObjectToGetInstanceFunction[obj]?.let {
                AssociatedObjectBySymbols(key.symbol, it.symbol, true)
            } ?: error("Could not find instance getter for $obj")
        }
        wasmFileCodegenContext.addClassAssociatedObjects(klass.symbol, associatedObjectsInstanceGetters)
    }

    fileContext.jsModuleAndQualifierReferences.forEach { reference ->
        wasmFileCodegenContext.addJsModuleAndQualifierReferences(reference)
    }

    val tryGetAssociatedObjectFunction = backendContext.wasmSymbols.tryGetAssociatedObject
    if (irFile == tryGetAssociatedObjectFunction.owner.fileOrNull) {
        wasmFileCodegenContext.defineTryGetAssociatedObjectFun(tryGetAssociatedObjectFunction)
    }

    if (backendContext.isWasmJsTarget) {
        val jsToKotlinAnyAdapter = backendContext.wasmSymbols.jsRelatedSymbols.jsInteropAdapters.jsToKotlinAnyAdapter
        if (irFile == jsToKotlinAnyAdapter.owner.fileOrNull) {
            wasmFileCodegenContext.defineJsToKotlinAnyAdapterFun(jsToKotlinAnyAdapter)
        }
    }
}