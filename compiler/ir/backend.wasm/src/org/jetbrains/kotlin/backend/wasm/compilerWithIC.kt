/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfigBuilder
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.backend.wasm.ic.WasmIrProgramFragments
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleMetadataCache
import org.jetbrains.kotlin.backend.wasm.ir2wasm.compileIrFile
import org.jetbrains.kotlin.backend.wasm.lower.markExportedDeclarations
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.ic.IrProgramFragments
import org.jetbrains.kotlin.ir.backend.js.ic.IrCompilerICInterface
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors

@OptIn(ObsoleteDescriptorBasedAPI::class)
open class WasmCompilerWithIC(
    private val mainModule: IrModuleFragment,
    configuration: CompilerConfiguration,
    private val allowIncompleteImplementations: Boolean,
    private val safeFragmentTags: Boolean,
) : IrCompilerICInterface {
    val context: WasmBackendContext
    private val idSignatureRetriever: IdSignatureRetriever
    private val wasmModuleMetadataCache: WasmModuleMetadataCache

    init {
        val irBuiltIns = mainModule.irBuiltins
        val symbolTable = (irBuiltIns as IrBuiltInsOverDescriptors).symbolTable

        //Hack - pre-load functional interfaces in case if IrLoader cut its count (KT-71039)
        repeat(25) {
            mainModule.irBuiltins.functionN(it)
            mainModule.irBuiltins.suspendFunctionN(it)
            mainModule.irBuiltins.kFunctionN(it)
            mainModule.irBuiltins.kSuspendFunctionN(it)
        }

        context = WasmBackendContext(
            mainModule.descriptor,
            irBuiltIns,
            symbolTable,
            mainModule,
            propertyLazyInitialization = configuration.getBoolean(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION),
            configuration = configuration,
        )

        idSignatureRetriever = context.irFactory as IdSignatureRetriever
        wasmModuleMetadataCache = WasmModuleMetadataCache(context)
    }

    private fun compileIrFile(irFile: IrFile): WasmIrProgramFragments {
        return WasmIrProgramFragments(
            compileIrFile(
                irFile,
                context,
                idSignatureRetriever,
                wasmModuleMetadataCache,
                allowIncompleteImplementations,
                if (safeFragmentTags) "${irFile.module.name.asString()}${irFile.path}" else null
            )
        )
    }

    override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrProgramFragments> {
        val wasmPhases = getWasmPhases(true)
        val phaseConfig = PhaseConfigBuilder(wasmPhases).also { lowerings ->
            lowerings.enabled.addAll(wasmPhases.toPhaseMap().values)
        }.build()

        //TODO: Lower only needed files but not all loaded by IrLoader KT-71041

        lowerPreservingTags(
            allModules,
            context,
            phaseConfig,
            context.irFactory.stageController as WholeWorldStageController,
            isIncremental = true
        )

        return dirtyFiles.map { { compileIrFile(it) } }
    }
}

class WasmCompilerWithICForTesting(
    mainModule: IrModuleFragment,
    configuration: CompilerConfiguration,
    allowIncompleteImplementations: Boolean,
    safeFragmentTags: Boolean = false,
) : WasmCompilerWithIC(mainModule, configuration, allowIncompleteImplementations, safeFragmentTags) {
    override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrProgramFragments> {
        val testFile = dirtyFiles.firstOrNull { file ->
            file.declarations.any { declaration -> declaration is IrFunction && declaration.name.asString() == "box" }
        } ?: return super.compile(allModules, dirtyFiles)

        val packageFqName = testFile.packageFqName.asString().takeIf { it.isNotEmpty() }
        markExportedDeclarations(context, testFile, setOf(FqName.fromSegments(listOfNotNull(packageFqName, "box"))))

        return super.compile(allModules, dirtyFiles)
    }
}
