/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.wasm.ic.WasmIrProgramFragments
import org.jetbrains.kotlin.backend.wasm.ic.overrideBuiltInsSignatures
import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.ic.IrCompilerICInterface
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragments
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.wasm.config.wasmDisableCrossFileOptimisations

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class WasmCompilerWithIC(
    private val mainModule: IrModuleFragment,
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
) : IrCompilerICInterface {
    val context: WasmBackendContext
    protected val idSignatureRetriever: IdSignatureRetriever
    protected val wasmModuleMetadataCache: WasmModuleMetadataCache

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

    abstract fun compileFile(irFile: IrFile): IrICProgramFragments

    override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrICProgramFragments> {
        //TODO: Lower only needed files but not all loaded by IrLoader KT-71041

        context.configuration.wasmDisableCrossFileOptimisations = true
        lowerPreservingTags(
            allModules,
            context,
            context.irFactory.stageController as WholeWorldStageController,
        )

        overrideBuiltInsSignatures(context)

        return dirtyFiles.map { { compileFile(it) } }
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
open class WasmCompilerWithICWholeWorld(
    mainModule: IrModuleFragment,
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
    private val allowIncompleteImplementations: Boolean,
    private val skipCommentInstructions: Boolean,
    private val skipLocations: Boolean,
) : WasmCompilerWithIC(
    mainModule = mainModule,
    irBuiltIns = irBuiltIns,
    configuration = configuration,
) {
    override fun compileFile(irFile: IrFile): WasmIrProgramFragments {
        val codeFileFragment = WasmCompiledCodeFileFragment()

        val typeContext = WasmTypeCodegenContext(
            wasmFileFragment = codeFileFragment.definedTypes,
            idSignatureRetriever = idSignatureRetriever
        )

        val declarationContext = WasmDeclarationCodegenContext(
            wasmFileFragment = codeFileFragment.definedDeclarations,
            idSignatureRetriever = idSignatureRetriever,
        )

        val linkerDataContext = WasmLinkerDataCodegenContext(
            wasmFileFragment = codeFileFragment.linkerData,
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
            importsContext = null,
            linkerDataContext = linkerDataContext,
            wasmModuleTypeTransformer = wasmModuleTypeTransformer,
            skipCommentInstructions = skipCommentInstructions,
            skipLocations = skipLocations,
            enableMultimoduleExports = false,
            moduleName = irFile.module.name.asString()
        )

        return WasmIrProgramFragments(codeFileFragment)
    }
}