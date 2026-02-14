/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.getInstanceFunctionForExternalObject
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class ModuleReferencedDeclarations(
    val functions: MutableSet<IdSignature> = mutableSetOf(),
    val globalVTable: MutableSet<IdSignature> = mutableSetOf(),
    val globalClassITable: MutableSet<IdSignature> = mutableSetOf(),
    val rttiGlobal: MutableSet<IdSignature> = mutableSetOf(),
)

class WasmModuleFragmentGenerator(
    private val backendContext: WasmBackendContext,
    private val wasmModuleMetadataCache: WasmModuleMetadataCache,
    private val idSignatureRetriever: IdSignatureRetriever,
    private val allowIncompleteImplementations: Boolean,
    private val skipCommentInstructions: Boolean,
    private val skipLocations: Boolean,
) {

    fun generateDependencyAsSingleFileFragment(irModuleFragment: IrModuleFragment): WasmCompiledDependencyFileFragment {
        val definedTypes = WasmCompiledTypesFileFragment()
        val typeCodegenContext = WasmTypeCodegenContext(
            wasmFileFragment = definedTypes,
            idSignatureRetriever = idSignatureRetriever
        )

        val definedDeclarations = WasmCompiledDeclarationsFileFragment()
        val declarationsCodegenContext = WasmDeclarationCodegenContext(
            wasmFileFragment = definedDeclarations,
            idSignatureRetriever = idSignatureRetriever
        )

        val wasmModuleTypeTransformer = WasmModuleTypeTransformer(backendContext, typeCodegenContext)
        val moduleName = irModuleFragment.name.asString()

        for (irFile in irModuleFragment.files) {
            compileIrFile(
                irFile = irFile,
                backendContext = backendContext,
                wasmModuleMetadataCache = wasmModuleMetadataCache,
                allowIncompleteImplementations = allowIncompleteImplementations,
                typeContext = typeCodegenContext,
                declarationContext = null,
                importsContext = declarationsCodegenContext,
                serviceDataContext = null,
                wasmModuleTypeTransformer = wasmModuleTypeTransformer,
                skipCommentInstructions = skipCommentInstructions,
                skipLocations = skipLocations,
                enableMultimoduleExports = false,
                moduleName = moduleName,
            )
        }

        return WasmCompiledDependencyFileFragment(
            definedTypes = definedTypes,
            definedDeclarations = definedDeclarations,
        )
    }


    fun generateAsSingleFileFragment(
        irModuleFragment: IrModuleFragment,
        trackedTypes: ModuleReferencedTypes?,
        trackedReferences: ModuleReferencedDeclarations?,
        enableMultimoduleExports: Boolean,
    ): WasmCompiledCodeFileFragment {

        val definedTypes = WasmCompiledTypesFileFragment()
        val typeCodegenContext = if (trackedTypes == null) {
            WasmTypeCodegenContext(
                wasmFileFragment = definedTypes,
                idSignatureRetriever = idSignatureRetriever
            )
        } else {
            WasmTrackedTypeCodegenContext(
                wasmFileFragment = definedTypes,
                moduleReferencedTypes = trackedTypes,
                idSignatureRetriever = idSignatureRetriever
            )
        }

        val definedDeclarations = WasmCompiledDeclarationsFileFragment()
        val declarationsCodegenContext = if (trackedReferences == null) {
            WasmDeclarationCodegenContext(
                wasmFileFragment = definedDeclarations,
                idSignatureRetriever = idSignatureRetriever
            )
        } else {
            WasmDeclarationCodegenContextWithTrackedReferences(
                moduleReferencedDeclarations = trackedReferences,
                moduleReferencedTypes = trackedTypes,
                wasmFileFragment = definedDeclarations,
                idSignatureRetriever = idSignatureRetriever
            )
        }

        val serviceData = WasmCompiledServiceFileFragment()
        val serviceDataCodegenContext = WasmServiceDataCodegenContext(
            wasmFileFragment = serviceData,
            idSignatureRetriever = idSignatureRetriever
        )

        generate(
            irModuleFragment = irModuleFragment,
            typeCodegenContext = typeCodegenContext,
            declarationCodegenContext = declarationsCodegenContext,
            serviceDataCodegenContext = serviceDataCodegenContext,
            enableMultimoduleExports = enableMultimoduleExports,
        )

        return WasmCompiledCodeFileFragment(
            definedTypes = definedTypes,
            definedDeclarations = definedDeclarations,
            serviceData = serviceData,
        )
    }

    private fun generate(
        irModuleFragment: IrModuleFragment,
        typeCodegenContext: WasmTypeCodegenContext,
        declarationCodegenContext: WasmDeclarationCodegenContext,
        serviceDataCodegenContext: WasmServiceDataCodegenContext,
        enableMultimoduleExports: Boolean,
    ) {
        val wasmModuleTypeTransformer = WasmModuleTypeTransformer(backendContext, typeCodegenContext)
        val moduleName = irModuleFragment.name.asString()
        for (irFile in irModuleFragment.files) {
            compileIrFile(
                irFile = irFile,
                backendContext = backendContext,
                wasmModuleMetadataCache = wasmModuleMetadataCache,
                allowIncompleteImplementations = allowIncompleteImplementations,
                typeContext = typeCodegenContext,
                declarationContext = declarationCodegenContext,
                importsContext = null,
                serviceDataContext = serviceDataCodegenContext,
                wasmModuleTypeTransformer = wasmModuleTypeTransformer,
                skipCommentInstructions = skipCommentInstructions,
                skipLocations = skipLocations,
                enableMultimoduleExports = enableMultimoduleExports,
                moduleName = moduleName,
            )
        }
    }
}

fun compileIrFile(
    irFile: IrFile,
    backendContext: WasmBackendContext,
    wasmModuleMetadataCache: WasmModuleMetadataCache,
    allowIncompleteImplementations: Boolean,
    typeContext: WasmTypeCodegenContext,
    declarationContext: WasmDeclarationCodegenContext?,
    importsContext: WasmDeclarationCodegenContext?,
    serviceDataContext: WasmServiceDataCodegenContext?,
    wasmModuleTypeTransformer: WasmModuleTypeTransformer,
    skipCommentInstructions: Boolean,
    skipLocations: Boolean,
    enableMultimoduleExports: Boolean,
    moduleName: String,
) {
    val typeGenerator = TypeGenerator(
        backendContext = backendContext,
        typeCodegenContext = typeContext,
        wasmModuleTypeTransformer = wasmModuleTypeTransformer,
        wasmModuleMetadataCache = wasmModuleMetadataCache
    )

    val declarationGenerator = declarationContext?.let { context ->
        DeclarationGenerator(
            backendContext = backendContext,
            typeCodegenContext = typeContext,
            declarationCodegenContext = context,
            serviceDataContext = serviceDataContext ?: error("Declaration codegen should have service data"),
            wasmModuleTypeTransformer = wasmModuleTypeTransformer,
            wasmModuleMetadataCache = wasmModuleMetadataCache,
            allowIncompleteImplementations = allowIncompleteImplementations,
            skipCommentInstructions = skipCommentInstructions,
            skipLocations = skipLocations,
            enableMultimoduleExports = enableMultimoduleExports,
        )
    }

    val importsGenerator = importsContext?.let { context ->
        ImportsGenerator(
            typeContext = typeContext,
            declarationContext = context,
            moduleName = moduleName,
        )
    }

    val generator = IrFileToWasmIrGenerator(
        typeGenerator = typeGenerator,
        declarationGenerator = declarationGenerator,
        importsGenerator = importsGenerator,
        backendContext = backendContext,
    )

    for (irDeclaration in irFile.declarations) {
        irDeclaration.acceptVoid(generator)
    }

    if (serviceDataContext == null) return

    val fileContext = backendContext.getFileContext(irFile)
    fileContext.mainFunctionWrapper?.apply {
        serviceDataContext.addMainFunctionWrapper(symbol)
    }
    fileContext.testFunctionDeclarator?.apply {
        serviceDataContext.addTestFunDeclarator(symbol)
    }
    fileContext.closureCallExports.forEach { (exportSignature, function) ->
        serviceDataContext.addEquivalentFunction("<1>_$exportSignature", function.symbol)
    }
    fileContext.kotlinClosureToJsConverters.forEach { (exportSignature, function) ->
        serviceDataContext.addEquivalentFunction("<2>_$exportSignature", function.symbol)
    }
    fileContext.jsClosureCallers.forEach { (exportSignature, function) ->
        serviceDataContext.addEquivalentFunction("<3>_$exportSignature", function.symbol)
    }
    fileContext.jsToKotlinClosures.forEach { (exportSignature, function) ->
        serviceDataContext.addEquivalentFunction("<4>_$exportSignature", function.symbol)
    }
    fileContext.objectInstanceFieldInitializer?.apply {
        serviceDataContext.addObjectInstanceFieldInitializer(symbol)
    }
    fileContext.nonConstantFieldInitializer?.apply {
        serviceDataContext.addNonConstantFieldInitializers(symbol)
    }

    fileContext.classAssociatedObjects.forEach { (klass, associatedObjects) ->
        val associatedObjectsInstanceGetters = associatedObjects.map { (key, obj) ->
            obj.objectGetInstanceFunction?.let {
                AssociatedObjectBySymbols(key.symbol, it.symbol, false)
            } ?: obj.getInstanceFunctionForExternalObject?.let {
                AssociatedObjectBySymbols(key.symbol, it.symbol, true)
            } ?: error("Could not find instance getter for $obj")
        }
        serviceDataContext.addClassAssociatedObjects(klass.symbol, associatedObjectsInstanceGetters)
    }

    fileContext.jsModuleAndQualifierReferences.forEach { reference ->
        serviceDataContext.addJsModuleAndQualifierReferences(reference)
    }
}