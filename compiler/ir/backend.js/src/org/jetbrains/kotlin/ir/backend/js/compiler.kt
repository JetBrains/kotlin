/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.inline.*
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

    MoveExternalDeclarationsToSeparatePlace().lower(moduleFragment.files)

    context.performInlining(moduleFragment)

    context.lower(moduleFragment.files)
    val transformer = SecondaryCtorLowering.CallsiteRedirectionTransformer(context)
    moduleFragment.files.forEach { it.accept(transformer, null) }

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

private fun JsIrBackendContext.lower(files: List<IrFile>) {
    LateinitLowering(this, true).lower(files)
    DefaultArgumentStubGenerator(this).runOnFilePostfix(files)
    DefaultParameterInjector(this).runOnFilePostfix(files)
    SharedVariablesLowering(this).runOnFilePostfix(files)
    EnumClassLowering(this).runOnFilePostfix(files)
    EnumUsageLowering(this).lower(files)
    ReturnableBlockLowering(this).lower(files)
    LocalDeclarationsLowering(this).runOnFilePostfix(files)
    InnerClassesLowering(this).runOnFilePostfix(files)
    InnerClassConstructorCallsLowering(this).runOnFilePostfix(files)
    PropertiesLowering().lower(files)
    InitializersLowering(this, JsLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER, false).runOnFilePostfix(files)
    MultipleCatchesLowering(this).lower(files)
    BridgesConstruction(this).runOnFilePostfix(files)
    TypeOperatorLowering(this).lower(files)
    BlockDecomposerLowering(this).runOnFilePostfix(files)
    SecondaryCtorLowering(this).runOnFilePostfix(files)
    CallableReferenceLowering(this).lower(files)
    IntrinsicifyCallsLowering(this).lower(files)
}

private fun FileLoweringPass.lower(files: List<IrFile>) = files.forEach { lower(it) }
private fun DeclarationContainerLoweringPass.runOnFilePostfix(files: List<IrFile>) = files.forEach { runOnFilePostfix(it) }
private fun BodyLoweringPass.runOnFilePostfix(files: List<IrFile>) = files.forEach { runOnFilePostfix(it) }
private fun FunctionLoweringPass.runOnFilePostfix(files: List<IrFile>) = files.forEach { runOnFilePostfix(it) }
private fun ClassLoweringPass.runOnFilePostfix(files: List<IrFile>) = files.forEach { runOnFilePostfix(it) }
