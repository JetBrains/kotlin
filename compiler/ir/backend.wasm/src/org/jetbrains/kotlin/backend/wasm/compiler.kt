/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCodeGenerator
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.generateStringLiteralsSupport
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.generateTypicalIrProviderList
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.wasm.ir.convertors.WasmBinaryBuilder
import org.jetbrains.kotlin.wasm.ir.convertors.WatBuilder
import java.io.ByteArrayOutputStream

class WasmCompilerResult(val wat: String, val js: String, val wasm: ByteArray)

fun compileWasm(
    project: Project,
    files: List<KtFile>,
    analyzer: AbstractAnalyzerWithCompilerReport,
    configuration: CompilerConfiguration,
    phaseConfig: PhaseConfig,
    allDependencies: KotlinLibraryResolveResult,
    friendDependencies: List<KotlinLibrary>,
    exportedDeclarations: Set<FqName> = emptySet()
): WasmCompilerResult {
    val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
        loadIr(
            project, MainModule.SourceFiles(files), analyzer, configuration, allDependencies, friendDependencies,
            PersistentIrFactory
        )

    val moduleDescriptor = moduleFragment.descriptor
    val context = WasmBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, exportedDeclarations, configuration)

    // Load declarations referenced during `context` initialization
    dependencyModules.forEach {
        val irProviders = generateTypicalIrProviderList(it.descriptor, irBuiltIns, symbolTable, deserializer)
        ExternalDependenciesGenerator(symbolTable, irProviders, configuration.languageVersionSettings)
            .generateUnboundSymbolsAsDependencies()
    }

    val irFiles = dependencyModules.flatMap { it.files } + moduleFragment.files

    moduleFragment.files.clear()
    moduleFragment.files += irFiles

    // Create stubs
    val irProviders = generateTypicalIrProviderList(moduleDescriptor, irBuiltIns, symbolTable, deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders, configuration.languageVersionSettings).generateUnboundSymbolsAsDependencies()
    moduleFragment.patchDeclarationParents()

    wasmPhases.invokeToplevel(phaseConfig, context, moduleFragment)

    val compiledWasmModule = WasmCompiledModuleFragment()
    val codeGenerator = WasmCodeGenerator(context, compiledWasmModule)
    codeGenerator.generateModule(moduleFragment)

    val linkedModule = compiledWasmModule.linkWasmCompiledFragments()
    val watGenerator = WatBuilder()
    watGenerator.appendWasmModule(linkedModule)
    val wat = watGenerator.toString()

    val os = ByteArrayOutputStream()
    WasmBinaryBuilder(os, linkedModule).appendWasmModule()
    val byteArray = os.toByteArray()

    return WasmCompilerResult(
        wat,
        generateStringLiteralsSupport(compiledWasmModule.stringLiterals),
        wasm = byteArray
    )
}
