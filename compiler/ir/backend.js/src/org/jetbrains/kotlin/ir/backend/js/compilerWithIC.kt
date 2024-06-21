/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.lower.collectNativeImplementations
import org.jetbrains.kotlin.ir.backend.js.lower.generateJsTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import java.io.File

class JsICContext(
    private val mainArguments: List<String>?,
    private val granularity: JsGenerationGranularity,
    private val phaseConfig: PhaseConfig,
    private val exportedDeclarations: Set<FqName> = emptySet(),
) : PlatformDependentICContext {

    override fun createIrFactory(): IrFactory =
        IrFactoryImplForJsIC(WholeWorldStageController())

    override fun createCompiler(mainModule: IrModuleFragment, configuration: CompilerConfiguration): IrCompilerICInterface =
        JsIrCompilerWithIC(mainModule, mainArguments, configuration, granularity, phaseConfig, exportedDeclarations)

    override fun createSrcFileArtifact(srcFilePath: String, fragments: IrProgramFragments?, astArtifact: File?): SrcFileArtifact =
        JsSrcFileArtifact(srcFilePath, fragments as? JsIrProgramFragments, astArtifact)

    override fun createModuleArtifact(
        moduleName: String,
        fileArtifacts: List<SrcFileArtifact>,
        artifactsDir: File?,
        forceRebuildJs: Boolean,
        externalModuleName: String?,
    ): ModuleArtifact =
        JsModuleArtifact(moduleName, fileArtifacts.map { it as JsSrcFileArtifact }, artifactsDir, forceRebuildJs, externalModuleName)
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JsIrCompilerWithIC(
    private val mainModule: IrModuleFragment,
    private val mainArguments: List<String>?,
    configuration: CompilerConfiguration,
    granularity: JsGenerationGranularity,
    private val phaseConfig: PhaseConfig,
    exportedDeclarations: Set<FqName> = emptySet(),
) : IrCompilerICInterface {
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

    override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrProgramFragments> {
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
    val jsLowerings = getJsLowerings(context.configuration)

    jsLowerings.forEachIndexed { i, lowering ->
        controller.currentStage = i + 1
        modules.forEach { module ->
            lowering.invoke(phaseConfig, phaserState, context, module)
        }
    }

    controller.currentStage = jsLowerings.size + 1
}
