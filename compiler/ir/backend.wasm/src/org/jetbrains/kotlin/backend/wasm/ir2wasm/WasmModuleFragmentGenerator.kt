/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class WasmModuleFragmentGenerator(
    private val backendContext: WasmBackendContext,
    private val wasmModuleMetadataCache: WasmModuleMetadataCache,
    private val idSignatureRetriever: IdSignatureRetriever,
    private val allowIncompleteImplementations: Boolean,
) {
    fun generateModule(irModuleFragment: IrModuleFragment): List<WasmCompiledFileFragment> {
        val wasmCompiledModuleFragments = mutableListOf<WasmCompiledFileFragment>()
        for (irFile in irModuleFragment.files) {
            val wasmFileFragment = compileIrFile(
                irFile,
                backendContext,
                idSignatureRetriever,
                wasmModuleMetadataCache,
                allowIncompleteImplementations
            )
            wasmCompiledModuleFragments.add(wasmFileFragment)
        }
        return wasmCompiledModuleFragments
    }
}

internal fun compileIrFile(
    irFile: IrFile,
    backendContext: WasmBackendContext,
    idSignatureRetriever: IdSignatureRetriever,
    wasmModuleMetadataCache: WasmModuleMetadataCache,
    allowIncompleteImplementations: Boolean
): WasmCompiledFileFragment {
    val wasmFileFragment = WasmCompiledFileFragment()
    val wasmFileCodegenContext = WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever)
    val wasmModuleTypeTransformer = WasmModuleTypeTransformer(backendContext, wasmFileCodegenContext)

    val generator = DeclarationGenerator(
        backendContext,
        wasmFileCodegenContext,
        wasmModuleTypeTransformer,
        wasmModuleMetadataCache,
        allowIncompleteImplementations,
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
        wasmFileCodegenContext.addClosureCallExport(exportSignature, function.symbol)
    }

    fileContext.classAssociatedObjects.forEach { (klass, associatedObjects) ->
        val associatedObjectsInstanceGettersSignatures = associatedObjects.map { (key, obj) ->
            key.symbol to backendContext.mapping.objectToGetInstanceFunction[obj]!!.symbol
        }
        wasmFileCodegenContext.addClassAssociatedObjects(klass.symbol, associatedObjectsInstanceGettersSignatures)
    }

    val tryGetAssociatedObjectFunction = backendContext.wasmSymbols.tryGetAssociatedObject
    if (irFile == tryGetAssociatedObjectFunction.owner.fileOrNull) {
        wasmFileCodegenContext.defineTryGetAssociatedObjectFun(tryGetAssociatedObjectFunction)
    }

    return wasmFileFragment
}