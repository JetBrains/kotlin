/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
import org.jetbrains.kotlin.backend.wasm.lower.markExportedDeclarations
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToBinary
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToText
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping
import java.io.ByteArrayOutputStream
import java.io.File

class WasmCompilerResult(
    val wat: String?,
    val js: String,
    val wasm: ByteArray,
    val sourceMap: String?
)

fun compileToLoweredIr(
    depsDescriptors: ModulesStructure,
    phaseConfig: PhaseConfig,
    irFactory: IrFactory,
    exportedDeclarations: Set<FqName> = emptySet(),
    propertyLazyInitialization: Boolean,
): Pair<List<IrModuleFragment>, WasmBackendContext> {
    val mainModule = depsDescriptors.mainModule
    val configuration = depsDescriptors.compilerConfiguration
    val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, irLinker) = loadIr(
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
        ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
    }

    // Create stubs
    ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
    allModules.forEach { it.patchDeclarationParents() }

    irLinker.postProcess()
    irLinker.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")

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
    allowIncompleteImplementations: Boolean = false,
    generateWat: Boolean = false,
    sourceMapFileName: String? = null,
): WasmCompilerResult {
    val compiledWasmModule = WasmCompiledModuleFragment(backendContext.irBuiltIns)
    val codeGenerator = WasmModuleFragmentGenerator(backendContext, compiledWasmModule, allowIncompleteImplementations = allowIncompleteImplementations)
    allModules.forEach { codeGenerator.collectInterfaceTables(it) }
    allModules.forEach { codeGenerator.generateModule(it) }

    val linkedModule = compiledWasmModule.linkWasmCompiledFragments()
    val wat = if (generateWat) {
        val watGenerator = WasmIrToText()
        watGenerator.appendWasmModule(linkedModule)
        watGenerator.toString()
    } else {
        null
    }

    val js = compiledWasmModule.generateJs()

    val os = ByteArrayOutputStream()

    val sourceLocationMappings =
        if (sourceMapFileName != null) mutableListOf<SourceLocationMapping>() else null

    val wasmIrToBinary =
        WasmIrToBinary(
            os,
            linkedModule,
            allModules.last().descriptor.name.asString(),
            emitNameSection,
            sourceMapFileName,
            sourceLocationMappings
        )

    wasmIrToBinary.appendWasmModule()

    val byteArray = os.toByteArray()

    return WasmCompilerResult(
        wat = wat,
        js = js,
        wasm = byteArray,
        sourceMap = generateSourceMap(backendContext.configuration, sourceLocationMappings)
    )
}

private fun generateSourceMap(
    configuration: CompilerConfiguration,
    sourceLocationMappings: MutableList<SourceLocationMapping>?
): String? {
    if (sourceLocationMappings == null) return null

    val sourceMapsInfo = SourceMapsInfo.from(configuration) ?: return null

    val sourceMapBuilder =
        SourceMap3Builder(null, { error("This should not be called for Kotlin/Wasm") }, sourceMapsInfo.sourceMapPrefix)

    val pathResolver =
        SourceFilePathResolver.create(sourceMapsInfo.sourceRoots, sourceMapsInfo.sourceMapPrefix, sourceMapsInfo.outputDir)

    var prev: SourceLocation? = null

    for (mapping in sourceLocationMappings) {
        val location = mapping.sourceLocation as? SourceLocation.Location ?: continue

        if (location == prev) continue

        prev = location

        location.apply {
            // TODO resulting path goes too deep since temporary directory we compiled first is deeper than final destination.   
            val relativePath = pathResolver.getPathRelativeToSourceRoots(File(file)).substring(3)
            sourceMapBuilder.addMapping(relativePath, null, { null }, line, column, null, mapping.offset)
        }
    }

    return sourceMapBuilder.build()
}

fun WasmCompiledModuleFragment.generateJs(): String {
    //language=js
    val runtime = """
    const externrefBoxes = new WeakMap();
    // ref must be non-null
    function tryGetOrSetExternrefBox(ref, ifNotCached) {
        if (typeof ref !== 'object') return ifNotCached;
        const cachedBox = externrefBoxes.get(ref);
        if (cachedBox !== void 0) return cachedBox;
        externrefBoxes.set(ref, ifNotCached);
        return ifNotCached;
    }    """.trimIndent()

    val jsCodeBody = jsFuns.joinToString(",\n") { "\"" + it.importName + "\" : " + it.jsCode }
    val jsCodeBodyIndented = jsCodeBody.prependIndent("    ")
    val jsCode =
        "\nconst js_code = {\n$jsCodeBodyIndented\n};\n"

    return runtime + jsCode
}

fun generateJsWasmLoader(wasmFilePath: String, externalJs: String): String =
    externalJs + """
    
    const isNodeJs = (typeof process !== 'undefined') && (process.release.name === 'node');
    const isD8 = !isNodeJs && (typeof d8 !== 'undefined');
    const isBrowser = !isNodeJs && !isD8 && (typeof window !== 'undefined');
    
    if (!isNodeJs && !isD8 && !isBrowser) {
      throw "Supported JS engine not detected";
    }
    
    let wasmInstance;
    let require; // Placed here to give access to it from externals (js_code)
    if (isNodeJs) {
      const module = await import(/* webpackIgnore: true */'node:module');
      require = module.default.createRequire(import.meta.url);
      const fs = require('fs');
      const path = require('path');
      const url = require('url');
      const filepath = url.fileURLToPath(import.meta.url);
      const dirpath = path.dirname(filepath);
      const wasmBuffer = fs.readFileSync(path.resolve(dirpath, '$wasmFilePath'));
      const wasmModule = new WebAssembly.Module(wasmBuffer);
      wasmInstance = new WebAssembly.Instance(wasmModule, { js_code });
    }
    
    if (isD8) {
      const wasmBuffer = read('$wasmFilePath', 'binary');
      const wasmModule = new WebAssembly.Module(wasmBuffer);
      wasmInstance = new WebAssembly.Instance(wasmModule, { js_code });
    }
    
    if (isBrowser) {
      wasmInstance = (await WebAssembly.instantiateStreaming(fetch('$wasmFilePath'), { js_code })).instance;
    }
    
    const wasmExports = wasmInstance.exports;
    wasmExports.__init();
    export default wasmExports;
    """.trimIndent()

fun writeCompilationResult(
    result: WasmCompilerResult,
    dir: File,
    fileNameBase: String,
    sourceMapFileName: String?
) {
    dir.mkdirs()
    if (result.wat != null) {
        File(dir, "$fileNameBase.wat").writeText(result.wat)
    }
    File(dir, "$fileNameBase.wasm").writeBytes(result.wasm)

    val jsWithLoader = generateJsWasmLoader("./$fileNameBase.wasm", result.js)
    File(dir, "$fileNameBase.mjs").writeText(jsWithLoader)

    if (sourceMapFileName != null) {
        File(dir, sourceMapFileName).writeText(result.sourceMap!!)
    }
}
