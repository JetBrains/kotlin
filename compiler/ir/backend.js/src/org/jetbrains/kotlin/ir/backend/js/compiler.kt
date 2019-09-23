/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.Kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

class CompilerResult(
    val jsCode: String,
    val tsDefinitions: String? = null
)

fun compile(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    phaseConfig: PhaseConfig,
    resolvedLibraries: KotlinLibraryResolveResult,
    allDependencies: List<KotlinLibrary>,
    friendDependencies: List<KotlinLibrary>,
    mainArguments: List<String>?,
    exportedDeclarations: Set<FqName> = emptySet()
): CompilerResult {
    val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
        loadIr(project, files, configuration, resolvedLibraries, allDependencies, friendDependencies)

    val moduleDescriptor = moduleFragment.descriptor

    val mainFunction = JsMainFunctionDetector.getMainFunctionOrNull(moduleFragment)

    val context = JsIrBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, exportedDeclarations, configuration)

    // Load declarations referenced during `context` initialization
    dependencyModules.forEach {
        ExternalDependenciesGenerator(
            it.descriptor,
            symbolTable,
            irBuiltIns,
            deserializer = deserializer
        ).generateUnboundSymbolsAsDependencies()
    }

    val irFiles = dependencyModules.flatMap { it.files } + moduleFragment.files

    moduleFragment.files.clear()
    moduleFragment.files += irFiles

    // Create stubs
    ExternalDependenciesGenerator(
        moduleDescriptor = moduleDescriptor,
        symbolTable = symbolTable,
        irBuiltIns = irBuiltIns
    ).generateUnboundSymbolsAsDependencies()
    moduleFragment.patchDeclarationParents()

    moveBodilessDeclarationsToSeparatePlace(context, moduleFragment)

    jsPhases.invokeToplevel(phaseConfig, context, moduleFragment)

    val transformer = IrModuleToJsTransformer(context, mainFunction, mainArguments)
    return transformer.generateModule(moduleFragment)
}