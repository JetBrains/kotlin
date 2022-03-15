/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.ic.ArtifactCache
import org.jetbrains.kotlin.ir.backend.js.ic.KLibArtifact
import org.jetbrains.kotlin.ir.backend.js.lower.collectNativeImplementations
import org.jetbrains.kotlin.ir.backend.js.lower.generateJsTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.backend.js.utils.serialization.JsIrAstDeserializer
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.ByteArrayInputStream

@Suppress("UNUSED_PARAMETER")
@OptIn(ObsoleteDescriptorBasedAPI::class)
fun compileWithIC(
    module: IrModuleFragment,
    configuration: CompilerConfiguration,
    deserializer: JsIrLinker,
    dependencies: Collection<IrModuleFragment>,
    mainArguments: List<String>? = null,
    exportedDeclarations: Set<FqName> = emptySet(),
    generateFullJs: Boolean = true,
    generateDceJs: Boolean = false,
    dceDriven: Boolean = false,
    dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    es6mode: Boolean = false,
    multiModule: Boolean = false,
    relativeRequirePath: Boolean = false,
    verifySignatures: Boolean = true,
    baseClassIntoMetadata: Boolean = false,
    lowerPerModule: Boolean = false,
    safeExternalBoolean: Boolean = false,
    safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null,
    filesToLower: Set<String>?,
    artifactCache: ArtifactCache,
) {

    val mainModule = module
    val allModules = dependencies
    val moduleDescriptor = module.descriptor
    val irBuiltIns = module.irBuiltins
    val symbolTable = (irBuiltIns as IrBuiltInsOverDescriptors).symbolTable

    val context = JsIrBackendContext(
        moduleDescriptor,
        irBuiltIns,
        symbolTable,
        module,
        exportedDeclarations,
        configuration,
        es6mode = es6mode,
        dceRuntimeDiagnostic = dceRuntimeDiagnostic,
        baseClassIntoMetadata = baseClassIntoMetadata,
        safeExternalBoolean = safeExternalBoolean,
        safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic,
        icCompatibleIr2Js = true,
    )

    // Load declarations referenced during `context` initialization
    val irProviders = listOf(deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

    deserializer.postProcess()
    symbolTable.noUnboundLeft("Unbound symbols at the end of linker")

    allModules.forEach {
        collectNativeImplementations(context, module)
        moveBodilessDeclarationsToSeparatePlace(context, it)
    }

    generateJsTests(context, mainModule)

    lowerPreservingTags(allModules, context, PhaseConfig(jsPhases), symbolTable.irFactory.stageController as WholeWorldStageController)

    val transformer = IrModuleToJsTransformerTmp(
        context,
        mainArguments,
        relativeRequirePath = relativeRequirePath,
    )

    val dirtyFiles = filesToLower?.let { dirties ->
        module.files.filter { it.fileEntry.name in dirties }
    } ?: module.files

    val ast = transformer.generateBinaryAst(dirtyFiles, allModules)

    ast.entries.forEach { (path, bytes) -> artifactCache.saveBinaryAst(path, bytes) }
}

fun lowerPreservingTags(modules: Iterable<IrModuleFragment>, context: JsIrBackendContext, phaseConfig: PhaseConfig, controller: WholeWorldStageController) {
    // Lower all the things
    controller.currentStage = 0

    val phaserState = PhaserState<Iterable<IrModuleFragment>>()

    loweringList.forEachIndexed { i, lowering ->
        controller.currentStage = i + 1
        lowering.modulePhase.invoke(phaseConfig, phaserState, context, modules)
    }

    controller.currentStage = pirLowerings.size + 1
}


@Suppress("UNUSED_PARAMETER")
fun generateJsFromAst(
    mainModuleName: String,
    moduleKind: ModuleKind,
    sourceMapsInfo: SourceMapsInfo?,
    translationModes: Set<TranslationMode>,
    caches: List<KLibArtifact>,
    relativeRequirePath: Boolean = false,
): CompilerResult {
    fun compilationOutput(multiModule: Boolean): CompilationOutputs {
        val deserializer = JsIrAstDeserializer()
        val jsIrProgram = JsIrProgram(caches.map { cacheArtifact ->
            JsIrModule(
                cacheArtifact.moduleName.safeModuleName,
                sanitizeName(cacheArtifact.moduleName.safeModuleName),
                cacheArtifact.fileArtifacts.sortedBy { it.srcFilePath }.mapNotNull { srcFileArtifact ->
                    srcFileArtifact.astFileArtifact.fetchBinaryAst()?.let { deserializer.deserialize(ByteArrayInputStream(it)) }
                })
        })

        return generateWrappedModuleBody(
            multiModule = multiModule,
            mainModuleName = mainModuleName,
            moduleKind = moduleKind,
            jsIrProgram,
            sourceMapsInfo = sourceMapsInfo,
            relativeRequirePath = relativeRequirePath,
            generateScriptModule = false,
        )
    }

    return CompilerResult(translationModes.associate { it to compilationOutput(it.perModule) }, null)
}
