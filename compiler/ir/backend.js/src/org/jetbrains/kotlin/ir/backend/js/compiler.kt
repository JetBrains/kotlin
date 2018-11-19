/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.InlineClassLowering
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.calls.CallsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.CoroutineIntrinsicLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.SuspendFunctionsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.FunctionInlining
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineFunctionsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.ReturnableBlockLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.replaceUnboundSymbols
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator

data class Result(val moduleDescriptor: ModuleDescriptor, val generatedCode: String, val moduleFragment: IrModuleFragment)

fun compile(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    export: FqName? = null,
    dependencies: List<ModuleDescriptor> = listOf(),
    irDependencyModules: List<IrModuleFragment> = listOf()
): Result {
    val analysisResult =
        TopDownAnalyzerFacadeForJS.analyzeFiles(files, project, configuration, dependencies.mapNotNull { it as? ModuleDescriptorImpl }, emptyList())

    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    TopDownAnalyzerFacadeForJS.checkForErrors(files, analysisResult.bindingContext)

    val psi2IrTranslator = Psi2IrTranslator(configuration.languageVersionSettings)
    val psi2IrContext = psi2IrTranslator.createGeneratorContext(analysisResult.moduleDescriptor, analysisResult.bindingContext)

    irDependencyModules.forEach { psi2IrContext.symbolTable.loadModule(it)}

    val moduleFragment = psi2IrTranslator.generateModuleFragment(psi2IrContext, files)

    val context = JsIrBackendContext(
        analysisResult.moduleDescriptor,
        psi2IrContext.irBuiltIns,
        psi2IrContext.symbolTable,
        moduleFragment,
        configuration
    )

    ExternalDependenciesGenerator(psi2IrContext.moduleDescriptor, psi2IrContext.symbolTable, psi2IrContext.irBuiltIns)
        .generateUnboundSymbolsAsDependencies(moduleFragment)

    val extensions = IrGenerationExtension.getInstances(project)
    extensions.forEach { extension ->
        moduleFragment.files.forEach { irFile -> extension.generate(irFile, context, psi2IrContext.bindingContext) }
    }

    MoveExternalDeclarationsToSeparatePlace().lower(moduleFragment)
    ExpectDeclarationsRemoving(context).lower(moduleFragment)
    CoroutineIntrinsicLowering(context).lower(moduleFragment)
    ArrayInlineConstructorLowering(context).lower(moduleFragment)
    LateinitLowering(context, true).lower(moduleFragment)

    val moduleFragmentCopy = moduleFragment.deepCopyWithSymbols()

    context.performInlining(moduleFragment)

    context.lower(moduleFragment, irDependencyModules)

    val program = moduleFragment.accept(IrModuleToJsTransformer(context), null)

    return Result(analysisResult.moduleDescriptor, program.toString(), moduleFragmentCopy)
}

private fun JsIrBackendContext.performInlining(moduleFragment: IrModuleFragment) {
    FunctionInlining(this).inline(moduleFragment)

    moduleFragment.replaceUnboundSymbols(this)
    moduleFragment.patchDeclarationParents()

    RemoveInlineFunctionsWithReifiedTypeParametersLowering.runOnFilesPostfix(moduleFragment)
}

private fun JsIrBackendContext.lower(moduleFragment: IrModuleFragment, dependencies: List<IrModuleFragment>) {
    val validateIr = {
        val visitor = IrValidator(this, validatorConfig)
        moduleFragment.acceptVoid(visitor)
    }
    validateIr()
    ThrowableSuccessorsLowering(this).lower(moduleFragment)
    TailrecLowering(this).runOnFilesPostfix(moduleFragment)
    UnitMaterializationLowering(this).lower(moduleFragment)
    EnumClassLowering(this).runOnFilesPostfix(moduleFragment)
    EnumUsageLowering(this).lower(moduleFragment)
    LateinitLowering(this, true).lower(moduleFragment)
    SharedVariablesLowering(this).runOnFilesPostfix(moduleFragment)
    ReturnableBlockLowering(this).lower(moduleFragment)
    LocalDelegatedPropertiesLowering().lower(moduleFragment)
    LocalDeclarationsLowering(this).runOnFilesPostfix(moduleFragment)
    InnerClassesLowering(this).runOnFilesPostfix(moduleFragment)
    InnerClassConstructorCallsLowering(this).runOnFilesPostfix(moduleFragment)
    validateIr()
    SuspendFunctionsLowering(this).lower(moduleFragment)
    CallableReferenceLowering(this).lower(moduleFragment)
    DefaultArgumentStubGenerator(this).runOnFilesPostfix(moduleFragment)
    DefaultParameterInjector(this).runOnFilesPostfix(moduleFragment)
    DefaultParameterCleaner(this).runOnFilesPostfix(moduleFragment)
    VarargLowering(this).lower(moduleFragment)
    PropertiesLowering().lower(moduleFragment)
    InitializersLowering(this, JsLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER, false).runOnFilesPostfix(moduleFragment)
    MultipleCatchesLowering(this).lower(moduleFragment)
    BridgesConstruction(this).runOnFilesPostfix(moduleFragment)
    TypeOperatorLowering(this).lower(moduleFragment)

    SecondaryCtorLowering(this).apply {
        constructorProcessorLowering.runOnFilesPostfix(moduleFragment.files + dependencies.flatMap { it.files })
        constructorRedirectorLowering.runOnFilesPostfix(moduleFragment)
    }

    InlineClassLowering(this).apply {
        inlineClassDeclarationLowering.runOnFilesPostfix(moduleFragment)
        inlineClassUsageLowering.lower(moduleFragment)
    }
    validateIr()
    AutoboxingTransformer(this).lower(moduleFragment)
    BlockDecomposerLowering(this).runOnFilesPostfix(moduleFragment)

    ClassReferenceLowering(this).lower(moduleFragment)
    PrimitiveCompanionLowering(this).lower(moduleFragment)
    ConstLowering(this).lower(moduleFragment)
    validateIr()
    CallsLowering(this).lower(moduleFragment)
}

val validatorConfig = IrValidatorConfig(
    abortOnError = true,
    ensureAllNodesAreDifferent = true,
    checkTypes = false,
    checkDescriptors = false
)

private fun FileLoweringPass.lower(moduleFragment: IrModuleFragment) = moduleFragment.files.forEach { lower(it) }

private fun DeclarationContainerLoweringPass.runOnFilesPostfix(moduleFragment: IrModuleFragment) =
    moduleFragment.files.forEach { runOnFilePostfix(it) }

private fun DeclarationContainerLoweringPass.runOnFilesPostfix(files: Iterable<IrFile>) =
    files.forEach { runOnFilePostfix(it) }

private fun BodyLoweringPass.runOnFilesPostfix(moduleFragment: IrModuleFragment) =
    moduleFragment.files.forEach { runOnFilePostfix(it) }

private fun FunctionLoweringPass.runOnFilesPostfix(moduleFragment: IrModuleFragment) =
    moduleFragment.files.forEach { runOnFilePostfix(it) }

private fun ClassLoweringPass.runOnFilesPostfix(moduleFragment: IrModuleFragment) =
    moduleFragment.files.forEach { runOnFilePostfix(it) }
