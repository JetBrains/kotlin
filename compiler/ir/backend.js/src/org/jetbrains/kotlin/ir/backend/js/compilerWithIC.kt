/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.lower.collectNativeImplementations
import org.jetbrains.kotlin.ir.backend.js.lower.generateJsTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragments
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import java.io.File

class JsICContext(private val granularity: JsGenerationGranularity) : PlatformDependentICContext {

    override fun createIrFactory(): IrFactory =
        IrFactoryImplForJsIC(WholeWorldStageController())

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun createBackendContext(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        symbolTable: SymbolTable,
        configuration: CompilerConfiguration,
    ): JsCommonBackendContext {
        return JsIrBackendContext(
            mainModule.descriptor,
            irBuiltIns,
            symbolTable,
            configuration = configuration,
            incrementalCacheEnabled = true,
        )
    }

    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration,
        context: JsCommonBackendContext,
    ): IrCompilerICInterface =
        JsIrCompilerWithIC(mainModule, context as JsIrBackendContext, granularity)

    override fun createSrcFileArtifact(srcFilePath: String, fragments: IrICProgramFragments?, astArtifact: File?): SrcFileArtifact =
        JsSrcFileArtifact(srcFilePath, fragments as? JsIrProgramFragments, astArtifact)

    override fun createModuleArtifact(
        moduleName: String,
        fileArtifacts: List<SrcFileArtifact>,
        artifactsDir: File?,
        forceRebuild: Boolean,
        externalModuleName: String?,
    ): ModuleArtifact =
        JsModuleArtifact(moduleName, fileArtifacts.map { it as JsSrcFileArtifact }, artifactsDir, forceRebuild, externalModuleName)
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JsIrCompilerWithIC(
    private val mainModule: IrModuleFragment,
    private val context: JsIrBackendContext,
    private val granularity: JsGenerationGranularity,
) : IrCompilerICInterface {

    override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrICProgramFragments> {
        val shouldGeneratePolyfills = context.configuration.getBoolean(JSConfigurationKeys.GENERATE_POLYFILLS)

        allModules.forEach {
            if (shouldGeneratePolyfills) {
                collectNativeImplementations(context, it)
            }
            moveBodilessDeclarationsToSeparatePlace(context, it)
        }

        generateJsTests(context, mainModule)

        lowerPreservingTags(allModules, context, context.irFactory.stageController as WholeWorldStageController)

        val transformer = IrModuleToJsTransformer(context)
        return transformer.makeIrFragmentsGenerators(dirtyFiles, allModules, granularity)
    }
}

fun lowerPreservingTags(
    modules: Iterable<IrModuleFragment>,
    context: JsIrBackendContext,
    controller: WholeWorldStageController
) {
    // Lower all the things
    controller.currentStage = 0

    val phaserState = PhaserState()
    jsLowerings.forEachIndexed { i, lowering ->
        controller.currentStage = i + 1
        modules.forEach { module ->
            lowering.invoke(context.phaseConfig, phaserState, context, module)
        }
    }

    controller.currentStage = jsLowerings.size + 1
}
