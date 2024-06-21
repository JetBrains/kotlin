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
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors

@OptIn(ObsoleteDescriptorBasedAPI::class)
open class WasmCompilerWithIC(
    mainModule: IrModuleFragment,
    configuration: CompilerConfiguration,
    private val allowIncompleteImplementations: Boolean,
) : IrCompilerICInterface {
    val context: WasmBackendContext
    private val idSignatureRetriever: IdSignatureRetriever
    private val wasmModuleMetadataCache: WasmModuleMetadataCache

    init {
        val irBuiltIns = mainModule.irBuiltins
        val symbolTable = (irBuiltIns as IrBuiltInsOverDescriptors).symbolTable

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
                allowIncompleteImplementations
            )
        )
    }

    override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrProgramFragments> {
//        allModules.forEach {
//            moveBodilessDeclarationsToSeparatePlace(context, it)
//        }

        val phaseConfig = PhaseConfigBuilder(wasmPhases).also {
            it.enabled.addAll(wasmPhases.toPhaseMap().values)
        }.build()

        lowerPreservingTags(allModules, context, phaseConfig, context.irFactory.stageController as WholeWorldStageController)

        return dirtyFiles.map { { compileIrFile(it) } }
    }
}
