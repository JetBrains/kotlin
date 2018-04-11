/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.lower.IntrinsicifyBuiltinOperationsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.SecondaryCtorLowering
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator

fun compile(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    export: FqName
): String {
    val analysisResult = TopDownAnalyzerFacadeForJS.analyzeFiles(files, project, configuration, emptyList(), emptyList())
    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    TopDownAnalyzerFacadeForJS.checkForErrors(files, analysisResult.bindingContext)

    val psi2IrTranslator = Psi2IrTranslator()
    val psi2IrContext = psi2IrTranslator.createGeneratorContext(analysisResult.moduleDescriptor, analysisResult.bindingContext)

    val moduleFragment = psi2IrTranslator.generateModuleFragment(psi2IrContext, files)

    val context = JsIrBackendContext(
        analysisResult.moduleDescriptor,
        psi2IrContext.irBuiltIns,
        moduleFragment,
        psi2IrContext.symbolTable
    )

    ExternalDependenciesGenerator(psi2IrContext.symbolTable, psi2IrContext.irBuiltIns).generateUnboundSymbolsAsDependencies(moduleFragment)

    moduleFragment.files.forEach { context.lower(it) }
    val transformer = SecondaryCtorLowering.CallsiteRedirectionTransformer(context)
    moduleFragment.files.forEach { it.accept(transformer, null) }

    val program = moduleFragment.accept(IrModuleToJsTransformer(context), null)

    return program.toString()
}

fun JsIrBackendContext.lower(file: IrFile) {
    LateinitLowering(this, true).lower(file)
    LocalFunctionsLowering(this).lower(file)
    DefaultArgumentStubGenerator(this).runOnFilePostfix(file)
    PropertiesLowering().lower(file)
    InitializersLowering(this, JsLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER, false).runOnFilePostfix(file)
    SecondaryCtorLowering(this).lower(file)

    IntrinsicifyBuiltinOperationsLowering(this).lower(file)
}