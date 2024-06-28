/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.ic.JsIrCompilerICInterface
import org.jetbrains.kotlin.ir.backend.js.lower.collectNativeImplementations
import org.jetbrains.kotlin.ir.backend.js.lower.generateJsTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JsIrCompilerWithIC(
    private val mainModule: IrModuleFragment,
    private val mainArguments: List<String>?,
    configuration: CompilerConfiguration,
    granularity: JsGenerationGranularity,
    private val phaseConfig: PhaseConfig,
    exportedDeclarations: Set<FqName> = emptySet(),
) : JsIrCompilerICInterface {
    private val context: JsIrBackendContext

    init {
        val irBuiltIns = mainModule.irBuiltins
        val symbolTable = (irBuiltIns as IrBuiltInsOverDescriptors).symbolTable

        context = JsIrBackendContext(
            mainModule.descriptor,
            irBuiltIns,
            symbolTable,
            exportedDeclarations,
            keep = emptySet(),
            configuration = configuration,
            granularity = granularity,
            incrementalCacheEnabled = true,
            mainCallArguments = mainArguments
        )
    }

    override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> JsIrProgramFragments> {
        val shouldGeneratePolyfills = context.configuration.getBoolean(JSConfigurationKeys.GENERATE_POLYFILLS)

        allModules.forEach {
            if (shouldGeneratePolyfills) {
                collectNativeImplementations(context, it)
            }
            moveBodilessDeclarationsToSeparatePlace(context, it)
        }

        generateJsTests(context, mainModule, groupByPackage = false)

        lowerPreservingTags(allModules, context, phaseConfig, context.irFactory.stageController as WholeWorldStageController)

        val transformer = IrModuleToJsTransformer(context, shouldReferMainFunction = mainArguments != null)
        return transformer.makeIrFragmentsGenerators(dirtyFiles, allModules)
    }
}

fun lowerPreservingTags(
    modules: Iterable<IrModuleFragment>,
    context: JsIrBackendContext,
    phaseConfig: PhaseConfig,
    controller: WholeWorldStageController
) {
    // Lower all the things
    controller.currentStage = 0

    val phaserState = PhaserState<IrModuleFragment>()

    loweringList.forEachIndexed { i, lowering ->
        controller.currentStage = i + 1
        modules.forEach { module ->
            lowering.invoke(phaseConfig, phaserState, context, module)
        }
    }

    controller.currentStage = loweringList.size + 1
}
