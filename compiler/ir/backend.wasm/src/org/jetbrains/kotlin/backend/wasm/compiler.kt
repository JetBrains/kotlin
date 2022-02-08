/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
import org.jetbrains.kotlin.backend.wasm.lower.markExportedDeclarations
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToBinary
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToText
import java.io.ByteArrayOutputStream

class WasmCompilerResult(val wat: String, val js: String, val wasm: ByteArray)

fun compileToLoweredIr(
    depsDescriptors: ModulesStructure,
    phaseConfig: PhaseConfig,
    irFactory: IrFactory,
    exportedDeclarations: Set<FqName> = emptySet(),
    propertyLazyInitialization: Boolean,
): Pair<List<IrModuleFragment>, WasmBackendContext> {
    val mainModule = depsDescriptors.mainModule
    val configuration = depsDescriptors.compilerConfiguration
    val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) = loadIr(
        depsDescriptors,
        irFactory,
        verifySignatures = false,
        loadFunctionInterfacesIntoStdlib = true,
    )

    val allModules = when (mainModule) {
        is MainModule.SourceFiles -> dependencyModules + listOf(moduleFragment)
        is MainModule.Klib -> dependencyModules
    }

    val moduleDescriptor = moduleFragment.descriptor
    val context = WasmBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, propertyLazyInitialization, configuration)

    // Load declarations referenced during `context` initialization
    allModules.forEach {
        ExternalDependenciesGenerator(symbolTable, listOf(deserializer)).generateUnboundSymbolsAsDependencies()
    }

    // Create stubs
    ExternalDependenciesGenerator(symbolTable, listOf(deserializer)).generateUnboundSymbolsAsDependencies()
    allModules.forEach { it.patchDeclarationParents() }

    deserializer.postProcess()
    symbolTable.noUnboundLeft("Unbound symbols at the end of linker")

    for (module in allModules)
        for (file in module.files)
            markExportedDeclarations(context, file, exportedDeclarations)

    wasmPhases.invokeToplevel(phaseConfig, context, allModules)

    return Pair(allModules, context)
}

fun compileWasm(
    allModules: List<IrModuleFragment>,
    backendContext: WasmBackendContext,
    emitNameSection: Boolean = false,
    dceEnabled: Boolean = false,
): WasmCompilerResult {

    if (dceEnabled) {
        eliminateDeadDeclarations(allModules, backendContext)
    }

    val compiledWasmModule = WasmCompiledModuleFragment(backendContext.irBuiltIns)
    val codeGenerator = WasmModuleFragmentGenerator(backendContext, compiledWasmModule, allowIncompleteImplementations = dceEnabled)
    allModules.forEach { codeGenerator.generateModule(it) }

    val linkedModule = compiledWasmModule.linkWasmCompiledFragments()
    val watGenerator = WasmIrToText()
    watGenerator.appendWasmModule(linkedModule)
    val wat = watGenerator.toString()

    val js = compiledWasmModule.generateJs()

    val os = ByteArrayOutputStream()
    WasmIrToBinary(os, linkedModule, allModules.last().descriptor.name.asString(), emitNameSection).appendWasmModule()
    val byteArray = os.toByteArray()

    return WasmCompilerResult(
        wat = wat,
        js = js,
        wasm = byteArray
    )
}

fun WasmCompiledModuleFragment.generateJs(): String {
    //language=js
    val runtime = """
    var wasmInstance = null;
    
    const externrefBoxes = new WeakMap();
    """.trimIndent()

    val jsFuns = jsFuns.joinToString(",\n") { "\"" + it.importName + "\" : " + it.jsCode }
    val jsCode = "\nconst js_code = {$jsFuns};"

    return runtime + jsCode
}
