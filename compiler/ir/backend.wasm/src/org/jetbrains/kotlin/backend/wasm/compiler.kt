/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.wasm.ir2wasm.JsModuleAndQualifierReference
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
import org.jetbrains.kotlin.backend.wasm.ir2wasm.toJsStringLiteral
import org.jetbrains.kotlin.backend.wasm.lower.JsInteropFunctionsLowering
import org.jetbrains.kotlin.backend.wasm.lower.markExportedDeclarations
import org.jetbrains.kotlin.backend.wasm.utils.SourceMapGenerator
import org.jetbrains.kotlin.backend.wasm.export.ExportModelGenerator
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.export.ExportModelToTsDeclarations
import org.jetbrains.kotlin.ir.backend.js.export.ExportedModule
import org.jetbrains.kotlin.ir.backend.js.export.TypeScriptFragment
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.WasmTarget
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToBinary
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToText
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.exp

class WasmCompilerResult(
    val wat: String?,
    val jsUninstantiatedWrapper: String?,
    val jsWrapper: String,
    val wasm: ByteArray,
    val debugInformation: DebugInformation?,
    val dts: String?
)

class DebugInformation(
    val sourceMapForBinary: String?,
    val sourceMapForText: String?,
)

data class LoweredIrWithExtraArtifacts(
    val loweredIr: List<IrModuleFragment>,
    val backendContext: WasmBackendContext,
    val typeScriptFragment: TypeScriptFragment?
)

fun compileToLoweredIr(
    depsDescriptors: ModulesStructure,
    phaseConfig: PhaseConfig,
    irFactory: IrFactory,
    exportedDeclarations: Set<FqName> = emptySet(),
    generateTypeScriptFragment: Boolean,
    propertyLazyInitialization: Boolean,
): LoweredIrWithExtraArtifacts {
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

    irLinker.postProcess(inOrAfterLinkageStep = true)
    irLinker.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")
    irLinker.clear()

    for (module in allModules)
        for (file in module.files)
            markExportedDeclarations(context, file, exportedDeclarations)

    val typeScriptFragment = runIf(generateTypeScriptFragment) {
        val exportModel = ExportModelGenerator(context).generateExport(allModules)
        val exportModelToDtsTranslator = ExportModelToTsDeclarations()
        val fragment = exportModelToDtsTranslator.generateTypeScriptFragment(ModuleKind.ES, exportModel.declarations)
        TypeScriptFragment(exportModelToDtsTranslator.generateTypeScript("", ModuleKind.ES, listOf(fragment)))
    }

    val phaserState = PhaserState<IrModuleFragment>()
    loweringList.forEachIndexed { _, lowering ->
        allModules.forEach { module ->
            lowering.invoke(phaseConfig, phaserState, context, module)
        }
    }

    return LoweredIrWithExtraArtifacts(allModules, context, typeScriptFragment)
}

fun compileWasm(
    allModules: List<IrModuleFragment>,
    backendContext: WasmBackendContext,
    typeScriptFragment: TypeScriptFragment?,
    baseFileName: String,
    emitNameSection: Boolean = false,
    allowIncompleteImplementations: Boolean = false,
    generateWat: Boolean = false,
    generateSourceMaps: Boolean = false,
): WasmCompilerResult {
    val compiledWasmModule = WasmCompiledModuleFragment(
        backendContext.irBuiltIns,
        backendContext.configuration.getBoolean(JSConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS)
    )
    val codeGenerator = WasmModuleFragmentGenerator(backendContext, compiledWasmModule, allowIncompleteImplementations = allowIncompleteImplementations)
    allModules.forEach { codeGenerator.collectInterfaceTables(it) }
    allModules.forEach { codeGenerator.generateModule(it) }

    val sourceMapGeneratorForBinary = runIf(generateSourceMaps) {
        SourceMapGenerator("$baseFileName.wasm", backendContext.configuration)
    }
    val sourceMapGeneratorForText = runIf(generateWat && generateSourceMaps) {
        SourceMapGenerator("$baseFileName.wat", backendContext.configuration)
    }

    val linkedModule = compiledWasmModule.linkWasmCompiledFragments()
    val wat = if (generateWat) {
        val watGenerator = WasmIrToText(sourceMapGeneratorForText)
        watGenerator.appendWasmModule(linkedModule)
        watGenerator.toString()
    } else {
        null
    }

    val os = ByteArrayOutputStream()

    val wasmIrToBinary =
        WasmIrToBinary(
            os,
            linkedModule,
            allModules.last().descriptor.name.asString(),
            emitNameSection,
            sourceMapGeneratorForBinary
        )

    wasmIrToBinary.appendWasmModule()

    val byteArray = os.toByteArray()
    val jsUninstantiatedWrapper: String?
    val jsWrapper: String
    if (backendContext.isWasmJsTarget) {
        jsUninstantiatedWrapper = compiledWasmModule.generateAsyncJsWrapper(
            "./$baseFileName.wasm",
            backendContext.jsModuleAndQualifierReferences
        )
        jsWrapper = compiledWasmModule.generateEsmExportsWrapper(
            "./$baseFileName.uninstantiated.mjs",
            backendContext.jsModuleAndQualifierReferences
        )
    } else {
        jsUninstantiatedWrapper = null
        jsWrapper = compiledWasmModule.generateAsyncWasiWrapper("./$baseFileName.wasm")
    }


    return WasmCompilerResult(
        wat = wat,
        jsUninstantiatedWrapper = jsUninstantiatedWrapper,
        jsWrapper = jsWrapper,
        wasm = byteArray,
        debugInformation = DebugInformation(
            sourceMapGeneratorForBinary?.generate(),
            sourceMapGeneratorForText?.generate(),
        ),
        dts = typeScriptFragment?.raw
    )
}

fun WasmCompiledModuleFragment.generateAsyncWasiWrapper(wasmFilePath: String): String = """
import { WASI } from 'wasi';
import { argv, env } from 'node:process';

const wasi = new WASI({ version: 'preview1', args: argv, env, });

const module = await import(/* webpackIgnore: true */'node:module');
const require = module.default.createRequire(import.meta.url);
const fs = require('fs');
const path = require('path');
const url = require('url');
const filepath = url.fileURLToPath(import.meta.url);
const dirpath = path.dirname(filepath);
const wasmBuffer = fs.readFileSync(path.resolve(dirpath, '$wasmFilePath'));
const wasmModule = new WebAssembly.Module(wasmBuffer);
const wasmInstance = new WebAssembly.Instance(wasmModule, wasi.getImportObject());

wasi.initialize(wasmInstance);

const exports = wasmInstance.exports
${generateExports()}
"""

fun WasmCompiledModuleFragment.generateAsyncJsWrapper(
    wasmFilePath: String,
    jsModuleAndQualifierReferences: Set<JsModuleAndQualifierReference>
): String {

    val jsCodeBody = jsFuns.joinToString(",\n") {
        "${it.importName.toJsStringLiteral()} : ${it.jsCode}"
    }

    val jsCodeBodyIndented = jsCodeBody.prependIndent("        ")

    val imports = jsModuleImports
        .toList()
        .sorted()
        .joinToString("") {
            val moduleSpecifier = it.toJsStringLiteral()
            "        $moduleSpecifier: imports[$moduleSpecifier],\n"
        }

    val referencesToQualifiedAndImportedDeclarations = jsModuleAndQualifierReferences
        .map {
            val module = it.module
            val qualifier = it.qualifier
            buildString {
                append("    const ")
                append(it.jsVariableName)
                append(" = ")
                if (module != null) {
                    append("imports[${module.toJsStringLiteral()}]")
                    if (qualifier != null)
                        append(".")
                }
                if (qualifier != null) {
                    append(qualifier)
                }
                append(";")
            }
        }.sorted()
        .joinToString("\n")

    //language=js
    return """
export async function instantiate(imports={}, runInitializer=true) {
    const externrefBoxes = new WeakMap();
    // ref must be non-null
    function tryGetOrSetExternrefBox(ref, ifNotCached) {
        if (typeof ref !== 'object') return ifNotCached;
        const cachedBox = externrefBoxes.get(ref);
        if (cachedBox !== void 0) return cachedBox;
        externrefBoxes.set(ref, ifNotCached);
        return ifNotCached;
    }

$referencesToQualifiedAndImportedDeclarations
    
    const js_code = {
$jsCodeBodyIndented
    }
    
    // Placed here to give access to it from externals (js_code)
    let wasmInstance;
    let require; 
    let wasmExports;

    const isNodeJs = (typeof process !== 'undefined') && (process.release.name === 'node');
    const isStandaloneJsVM =
        !isNodeJs && (
            typeof d8 !== 'undefined' // V8
            || typeof inIon !== 'undefined' // SpiderMonkey
            || typeof jscOptions !== 'undefined' // JavaScriptCore
        );
    const isBrowser = !isNodeJs && !isStandaloneJsVM && (typeof window !== 'undefined');
    
    if (!isNodeJs && !isStandaloneJsVM && !isBrowser) {
      throw "Supported JS engine not detected";
    }
    
    const wasmFilePath = ${wasmFilePath.toJsStringLiteral()};
    const importObject = {
        js_code,
$imports
    };
    
    try {
      if (isNodeJs) {
        const module = await import(/* webpackIgnore: true */'node:module');
        require = module.default.createRequire(import.meta.url);
        const fs = require('fs');
        const path = require('path');
        const url = require('url');
        const filepath = url.fileURLToPath(import.meta.url);
        const dirpath = path.dirname(filepath);
        const wasmBuffer = fs.readFileSync(path.resolve(dirpath, wasmFilePath));
        const wasmModule = new WebAssembly.Module(wasmBuffer);
        wasmInstance = new WebAssembly.Instance(wasmModule, importObject);
      }
      
      if (isStandaloneJsVM) {
        const wasmBuffer = read(wasmFilePath, 'binary');
        const wasmModule = new WebAssembly.Module(wasmBuffer);
        wasmInstance = new WebAssembly.Instance(wasmModule, importObject);
      }
      
      if (isBrowser) {
        wasmInstance = (await WebAssembly.instantiateStreaming(fetch(wasmFilePath), importObject)).instance;
      }
    } catch (e) {
      if (e instanceof WebAssembly.CompileError) {
        let text = `Please make sure that your runtime environment supports the latest version of Wasm GC and Exception-Handling proposals.
For more information, see https://kotl.in/wasm-help
`;
        if (isBrowser) {
          console.error(text);
        } else {
          const t = "\n" + text;
          if (typeof console !== "undefined" && console.log !== void 0) 
            console.log(t);
          else 
            print(t);
        }
      }
      throw e;
    }
    
    wasmExports = wasmInstance.exports;
    if (runInitializer) {
        wasmExports._initialize();
    }

    return { instance: wasmInstance,  exports: wasmExports };
}
"""
}

fun WasmCompiledModuleFragment.generateEsmExportsWrapper(
    asyncWrapperFileName: String,
    jsModuleAndQualifierReferences: MutableSet<JsModuleAndQualifierReference>
): String {
    val importedModules = jsModuleImports
        .map {
            val moduleSpecifier = it.toJsStringLiteral().toString()
            val importVariableString = JsModuleAndQualifierReference.encode(it)
            moduleSpecifier to importVariableString
        }

    val referencesToImportedDeclarations = jsModuleAndQualifierReferences
        .filter { it.module != null }
        .map {
            val module = it.module!!
            val stringLiteral = module.toJsStringLiteral().toString()
            stringLiteral to if (it.qualifier != null) {
                it.importVariableName
            } else {
                it.jsVariableName
            }
        }

    val allModules = (importedModules + referencesToImportedDeclarations)
        .distinctBy {
            it.first
        }.sortedBy { it.first }

    val importsImportedSection = allModules.joinToString("\n") {
        buildString {
            append("import * as ")
            append(it.second)
            append(" from ")
            append(it.first)
            append(";")
        }
    }

    val imports = allModules.joinToString(",\n") {
        "    ${it.first}: ${it.second}"
    }

    /*language=js */
    return """
$importsImportedSection
import { instantiate } from ${asyncWrapperFileName.toJsStringLiteral()};

const exports = (await instantiate({
$imports
})).exports;
${generateExports()}
"""
}

fun writeCompilationResult(
    result: WasmCompilerResult,
    dir: File,
    fileNameBase: String
) {
    dir.mkdirs()
    if (result.wat != null) {
        File(dir, "$fileNameBase.wat").writeText(result.wat)
    }
    File(dir, "$fileNameBase.wasm").writeBytes(result.wasm)

    if (result.jsUninstantiatedWrapper != null) {
        File(dir, "$fileNameBase.uninstantiated.mjs").writeText(result.jsUninstantiatedWrapper)
    }
    File(dir, "$fileNameBase.mjs").writeText(result.jsWrapper)

    result.debugInformation?.sourceMapForBinary?.let {
        File(dir, "$fileNameBase.wasm.map").writeText(it)
    }
    result.debugInformation?.sourceMapForText?.let {
        File(dir, "$fileNameBase.wat.map").writeText(it)
    }

    if (result.dts != null) {
        File(dir, "$fileNameBase.d.ts").writeText(result.dts)
    }
}

fun WasmCompiledModuleFragment.generateExports(): String {
    // TODO: necessary to move export check onto common place
    val exportNames = exports
        .filterNot { it.name.startsWith(JsInteropFunctionsLowering.CALL_FUNCTION) }
        .ifNotEmpty {
            joinToString(",\n") {
                "    ${it.name}"
            }
        }?.let {
            """
            |const {
                |$it
            |}
        """.trimMargin()
        }

    /*language=js */
    return """
export default new Proxy(exports, {
    _shownError: false,
    get(target, prop) {
        if (!this._shownError) {
            this._shownError = true;
            if (typeof console !== "undefined") {
                console.error("Do not use default import. Use corresponding named import instead.")
            }
        }
        return target[prop];
    }
});
${exportNames?.let { "export $it = exports;" }}
"""
}
