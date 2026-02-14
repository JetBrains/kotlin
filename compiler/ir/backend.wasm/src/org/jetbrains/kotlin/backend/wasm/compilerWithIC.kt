/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.wasm.ic.WasmIrProgramFragments
import org.jetbrains.kotlin.backend.wasm.ic.WasmIrProgramFragmentsMultimodule
import org.jetbrains.kotlin.backend.wasm.ic.overrideBuiltInsSignatures
import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.backend.wasm.lower.markExportedDeclarations
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.ic.IrCompilerICInterface
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragments
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys

@OptIn(ObsoleteDescriptorBasedAPI::class)
open class WasmCompilerWithIC(
    private val mainModule: IrModuleFragment,
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
    private val allowIncompleteImplementations: Boolean,
    private val skipCommentInstructions: Boolean,
    private val skipLocations: Boolean,
) : IrCompilerICInterface {
    val context: WasmBackendContext
    private val idSignatureRetriever: IdSignatureRetriever
    private val wasmModuleMetadataCache: WasmModuleMetadataCache

    init {
        val symbolTable = (irBuiltIns as IrBuiltInsOverDescriptors).symbolTable

        //Hack - pre-load functional interfaces in case if IrLoader cut its count (KT-71039)
        repeat(25) {
            irBuiltIns.functionN(it)
            irBuiltIns.suspendFunctionN(it)
            irBuiltIns.kFunctionN(it)
            irBuiltIns.kSuspendFunctionN(it)
        }

        context = WasmBackendContext(
            mainModule.descriptor,
            irBuiltIns,
            symbolTable,
            mainModule,
            configuration = configuration,
        )

        idSignatureRetriever = context.irFactory as IdSignatureRetriever
        wasmModuleMetadataCache = WasmModuleMetadataCache(context)
    }

    private fun compileIrFile(irFile: IrFile): WasmIrProgramFragments {
        val codeFileFragment = WasmCompiledCodeFileFragment()
        val dependencyFileFragment = WasmCompiledDependencyFileFragment(
            definedTypes = codeFileFragment.definedTypes,
        )

        val moduleReferencedTypes = ModuleReferencedTypes()
        val typeContext = WasmTrackedTypeCodegenContext(
            wasmFileFragment = codeFileFragment.definedTypes,
            moduleReferencedTypes = moduleReferencedTypes,
            idSignatureRetriever = idSignatureRetriever
        )

        val moduleReferencedDeclarations = ModuleReferencedDeclarations()
        val declarationContext = WasmDeclarationCodegenContextWithTrackedReferences(
            moduleReferencedDeclarations = moduleReferencedDeclarations,
            moduleReferencedTypes = moduleReferencedTypes,
            wasmFileFragment = codeFileFragment.definedDeclarations,
            idSignatureRetriever = idSignatureRetriever,
        )

        val importContext = WasmDeclarationCodegenContext(
            wasmFileFragment = dependencyFileFragment.definedDeclarations,
            idSignatureRetriever = idSignatureRetriever,
        )

        val serviceDataContext = WasmServiceDataCodegenContext(
            wasmFileFragment = codeFileFragment.serviceData,
            idSignatureRetriever = idSignatureRetriever
        )

        val wasmModuleTypeTransformer = WasmModuleTypeTransformer(
            backendContext = context,
            wasmTypeContext = typeContext
        )

        compileIrFile(
            irFile = irFile,
            backendContext = context,
            wasmModuleMetadataCache = wasmModuleMetadataCache,
            allowIncompleteImplementations = allowIncompleteImplementations,
            typeContext = typeContext,
            declarationContext = declarationContext,
            importsContext = importContext,
            serviceDataContext = serviceDataContext,
            wasmModuleTypeTransformer = wasmModuleTypeTransformer,
            skipCommentInstructions = skipCommentInstructions,
            skipLocations = skipLocations,
            enableMultimoduleExports = true,
            moduleName = irFile.module.name.asString()
        )

        return WasmIrProgramFragmentsMultimodule(
            mainFragment = codeFileFragment,
            referencedTypes = moduleReferencedTypes,
            referencedDeclarations = moduleReferencedDeclarations,
            dependencyFragment = dependencyFileFragment,
        )
    }

    override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrICProgramFragments> {
        //TODO: Lower only needed files but not all loaded by IrLoader KT-71041

        overrideBuiltInsSignatures(context)

        context.configuration.put(WasmConfigurationKeys.WASM_DISABLE_CROSS_FILE_OPTIMISATIONS, true)
        lowerPreservingTags(
            allModules,
            context,
            context.irFactory.stageController as WholeWorldStageController,
        )

        return dirtyFiles.map { { compileIrFile(it) } }
    }
}

class WasmCompilerWithICForTesting(
    mainModule: IrModuleFragment,
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
    allowIncompleteImplementations: Boolean,
) : WasmCompilerWithIC(
    mainModule,
    irBuiltIns,
    configuration,
    allowIncompleteImplementations,
    skipCommentInstructions = false,
    skipLocations = false,
) {
    override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrICProgramFragments> {
        val testFile = dirtyFiles.firstOrNull { file ->
            file.declarations.any { declaration -> declaration is IrFunction && declaration.name.asString() == "box" }
        } ?: return super.compile(allModules, dirtyFiles)

        val packageFqName = testFile.packageFqName.asString().takeIf { it.isNotEmpty() }
        markExportedDeclarations(context, testFile, setOf(FqName.fromSegments(listOfNotNull(packageFqName, "box"))))

        return super.compile(allModules, dirtyFiles)
    }
}
