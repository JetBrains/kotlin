/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.SuspendFunctionsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.FunctionInlining
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineFunctionsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.ReturnableBlockLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.replaceUnboundSymbols
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator

data class Result(val moduleDescriptor: ModuleDescriptor, val generatedCode: String)

fun compile(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    export: FqName? = null,
    dependencies: List<ModuleDescriptor> = listOf()
): Result {
    val analysisResult =
        TopDownAnalyzerFacadeForJS.analyzeFiles(files, project, configuration, dependencies.filterIsInstance(), emptyList())

    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    TopDownAnalyzerFacadeForJS.checkForErrors(files, analysisResult.bindingContext)

    val psi2IrTranslator = Psi2IrTranslator(configuration.languageVersionSettings)
    val psi2IrContext = psi2IrTranslator.createGeneratorContext(analysisResult.moduleDescriptor, analysisResult.bindingContext)

    val moduleFragment = psi2IrTranslator.generateModuleFragment(psi2IrContext, files)

    val context = JsIrBackendContext(
        analysisResult.moduleDescriptor,
        psi2IrContext.irBuiltIns,
        psi2IrContext.symbolTable,
        moduleFragment
    )

    ExternalDependenciesGenerator(psi2IrContext.moduleDescriptor, psi2IrContext.symbolTable, psi2IrContext.irBuiltIns)
        .generateUnboundSymbolsAsDependencies(moduleFragment)

    val extensions = IrGenerationExtension.getInstances(project)
    extensions.forEach { extension ->
        moduleFragment.files.forEach { irFile -> extension.generate(irFile, context, psi2IrContext.bindingContext) }
    }

    MoveExternalDeclarationsToSeparatePlace().lower(moduleFragment.files)

    context.performInlining(moduleFragment)

    context.lower(moduleFragment)

    val program = moduleFragment.accept(IrModuleToJsTransformer(context), null)

    return Result(analysisResult.moduleDescriptor, program.toString())
}

private fun JsIrBackendContext.performInlining(moduleFragment: IrModuleFragment) {
    FunctionInlining(this).inline(moduleFragment)


    moduleFragment.replaceUnboundSymbols(this)
    moduleFragment.patchDeclarationParents()

    moduleFragment.files.forEach { file ->
        RemoveInlineFunctionsWithReifiedTypeParametersLowering.runOnFilePostfix(file)
    }
}

private fun JsIrBackendContext.lower(moduleFragment: IrModuleFragment) {
    moduleFragment.files.forEach(LateinitLowering(this, true)::lower)
    moduleFragment.files.forEach(DefaultArgumentStubGenerator(this)::runOnFilePostfix)
    moduleFragment.files.forEach(DefaultParameterInjector(this)::runOnFilePostfix)
    moduleFragment.files.forEach(DefaultParameterCleaner(this)::runOnFilePostfix)
    moduleFragment.files.forEach(SharedVariablesLowering(this)::runOnFilePostfix)
    moduleFragment.files.forEach(EnumClassLowering(this)::runOnFilePostfix)
    moduleFragment.files.forEach(EnumUsageLowering(this)::lower)
    moduleFragment.files.forEach(ReturnableBlockLowering(this)::lower)
    moduleFragment.files.forEach(LocalDelegatedPropertiesLowering()::lower)
    moduleFragment.files.forEach(LocalDeclarationsLowering(this)::runOnFilePostfix)
    moduleFragment.files.forEach(InnerClassesLowering(this)::runOnFilePostfix)
    moduleFragment.files.forEach(InnerClassConstructorCallsLowering(this)::runOnFilePostfix)
    moduleFragment.files.forEach(SuspendFunctionsLowering(this)::lower)
    moduleFragment.files.forEach(PropertiesLowering()::lower)
    moduleFragment.files.forEach(InitializersLowering(this, JsLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER, false)::runOnFilePostfix)
    moduleFragment.files.forEach(MultipleCatchesLowering(this)::lower)
    moduleFragment.files.forEach(BridgesConstruction(this)::runOnFilePostfix)
    moduleFragment.files.forEach(TypeOperatorLowering(this)::lower)
    moduleFragment.files.forEach(BlockDecomposerLowering(this)::runOnFilePostfix)
    val sctor = SecondaryCtorLowering(this)
    moduleFragment.files.forEach(sctor.getConstructorProcessorLowering())
    moduleFragment.files.forEach(sctor.getConstructorRedirectorLowering())
    val clble = CallableReferenceLowering(this)
    moduleFragment.files.forEach(clble.getReferenceCollector())
    moduleFragment.files.forEach(clble.getClosureBuilder())
    moduleFragment.files.forEach(clble.getReferenceReplacer())
    moduleFragment.files.forEach(ClassReferenceLowering(this)::lower)
    moduleFragment.files.forEach(PrimitiveCompanionLowering(this)::lower)
    moduleFragment.files.forEach(IntrinsicifyCallsLowering(this)::lower)
}

private fun FileLoweringPass.lower(files: List<IrFile>) = files.forEach { lower(it) }
