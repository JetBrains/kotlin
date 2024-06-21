/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.wasm.export.ExportModelGenerator
import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment.JsCodeSnippet
import org.jetbrains.kotlin.backend.wasm.lower.JsInteropFunctionsLowering
import org.jetbrains.kotlin.backend.wasm.lower.markExportedDeclarations
import org.jetbrains.kotlin.backend.wasm.utils.SourceMapGenerator
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.IrModuleInfo
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.export.ExportModelToTsDeclarations
import org.jetbrains.kotlin.ir.backend.js.export.TypeScriptFragment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToBinary
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToText
import java.io.ByteArrayOutputStream
import java.io.File

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
    irModuleInfo: IrModuleInfo,
    mainModule: MainModule,
    configuration: CompilerConfiguration,
    performanceManager: CommonCompilerPerformanceManager?,
    phaseConfig: PhaseConfig,
    exportedDeclarations: Set<FqName> = emptySet(),
    generateTypeScriptFragment: Boolean,
    propertyLazyInitialization: Boolean,
): LoweredIrWithExtraArtifacts {
    val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, irLinker) = irModuleInfo

    val allModules = when (mainModule) {
        is MainModule.SourceFiles -> dependencyModules + listOf(moduleFragment)
        is MainModule.Klib -> dependencyModules
    }

    val moduleDescriptor = moduleFragment.descriptor
    val context = WasmBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, propertyLazyInitialization, configuration)

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
    performanceManager?.notifyIRTranslationFinished()

    performanceManager?.notifyGenerationStarted()
    performanceManager?.notifyIRLoweringStarted()

    lowerPreservingTags(allModules, context, phaseConfig, context.irFactory.stageController as WholeWorldStageController)

    performanceManager?.notifyIRLoweringFinished()

    return LoweredIrWithExtraArtifacts(allModules, context, typeScriptFragment)
}

fun lowerPreservingTags(
    modules: Iterable<IrModuleFragment>,
    context: WasmBackendContext,
    phaseConfig: PhaseConfig,
    controller: WholeWorldStageController
) {
    // Lower all the things
    controller.currentStage = 0

    val phaserState = PhaserState<IrModuleFragment>()

    loweringList.forEachIndexed { i, lowering ->
        controller.currentStage = i + 1
        modules.forEach { module ->
            lowering.invoke(phaseConfig, phaserState, context, module)
        }
    }

    controller.currentStage = loweringList.size + 1
}

fun compileWasm(
    wasmCompiledFileFragments: List<WasmCompiledFileFragment>,
    moduleName: String,
    configuration: CompilerConfiguration,
    typeScriptFragment: TypeScriptFragment?,
    baseFileName: String,
    emitNameSection: Boolean = false,
    generateWat: Boolean = false,
    generateSourceMaps: Boolean = false,
): WasmCompilerResult {
    val wasmCompiledModuleFragment = WasmCompiledModuleFragment(
        wasmCompiledFileFragments,
        configuration.getBoolean(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS)
    )

    wasmCompiledModuleFragment.createInterfaceTablesAndLinkTableSymbols()

    val linkedModule = wasmCompiledModuleFragment.linkWasmCompiledFragments()

    val sourceMapGeneratorForBinary = runIf(generateSourceMaps) {
        SourceMapGenerator("$baseFileName.wasm", configuration)
    }
    val sourceMapGeneratorForText = runIf(generateWat && generateSourceMaps) {
        SourceMapGenerator("$baseFileName.wat", configuration)
    }

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
            moduleName,
            emitNameSection,
            sourceMapGeneratorForBinary
        )

    wasmIrToBinary.appendWasmModule()

    val byteArray = os.toByteArray()
    val jsUninstantiatedWrapper: String?
    val jsWrapper: String
    if (configuration.get(WasmConfigurationKeys.WASM_TARGET) != WasmTarget.WASI) {
        val jsModuleImports = mutableSetOf<String>()
        val jsFuns = mutableSetOf<JsCodeSnippet>()
        val jsModuleAndQualifierReferences = mutableSetOf<JsModuleAndQualifierReference>()
        wasmCompiledFileFragments.forEach { fragment ->
            jsModuleImports.addAll(fragment.jsModuleImports)
            jsFuns.addAll(fragment.jsFuns)
            jsModuleAndQualifierReferences.addAll(fragment.jsModuleAndQualifierReferences)
        }

        jsUninstantiatedWrapper = generateAsyncJsWrapper(
            jsModuleImports,
            jsFuns,
            "./$baseFileName.wasm",
            jsModuleAndQualifierReferences,
        )
        jsWrapper = wasmCompiledModuleFragment.generateEsmExportsWrapper(
            jsModuleImports,
            "./$baseFileName.uninstantiated.mjs",
            jsModuleAndQualifierReferences,
            linkedModule.exports,
        )
    } else {
        jsUninstantiatedWrapper = null
        jsWrapper = wasmCompiledModuleFragment.generateAsyncWasiWrapper("./$baseFileName.wasm", linkedModule.exports)
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

//language=js
fun WasmCompiledModuleFragment.generateAsyncWasiWrapper(wasmFilePath: String, exports: List<WasmExport<*>>): String = """
import { WASI } from 'wasi';
import { argv, env } from 'node:process';

const wasi = new WASI({ version: 'preview1', args: argv, env, });

const fs = await import('node:fs');
const url = await import('node:url');
const wasmBuffer = fs.readFileSync(url.fileURLToPath(import.meta.resolve('$wasmFilePath')));
const wasmModule = new WebAssembly.Module(wasmBuffer);
const wasmInstance = new WebAssembly.Instance(wasmModule, wasi.getImportObject());

wasi.initialize(wasmInstance);

const exports = wasmInstance.exports
${generateExports(exports)}
"""

fun generateAsyncJsWrapper(
    jsModuleImports: Set<String>,
    jsFuns: Set<JsCodeSnippet>,
    wasmFilePath: String,
    jsModuleAndQualifierReferences: Set<JsModuleAndQualifierReference>
): String {

    val jsCodeBody = jsFuns.joinToString(",\n") {
        "${it.importName.owner.toJsStringLiteral()} : ${it.jsCode}"
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
    const cachedJsObjects = new WeakMap();
    // ref must be non-null
    function getCachedJsObject(ref, ifNotCached) {
        if (typeof ref !== 'object' && typeof ref !== 'function') return ifNotCached;
        const cached = cachedJsObjects.get(ref);
        if (cached !== void 0) return cached;
        cachedJsObjects.set(ref, ifNotCached);
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
    const isDeno = !isNodeJs && (typeof Deno !== 'undefined')
    const isStandaloneJsVM =
        !isDeno && !isNodeJs && (
            typeof d8 !== 'undefined' // V8
            || typeof inIon !== 'undefined' // SpiderMonkey
            || typeof jscOptions !== 'undefined' // JavaScriptCore
        );
    const isBrowser = !isNodeJs && !isDeno && !isStandaloneJsVM && (typeof window !== 'undefined' || typeof self !== 'undefined');
    
    if (!isNodeJs && !isDeno && !isStandaloneJsVM && !isBrowser) {
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
        const importMeta = import.meta;
        require = module.default.createRequire(importMeta.url);
        const fs = require('fs');
        const url = require('url');
        const filepath = import.meta.resolve(wasmFilePath);
        const wasmBuffer = fs.readFileSync(url.fileURLToPath(filepath));
        const wasmModule = new WebAssembly.Module(wasmBuffer);
        wasmInstance = new WebAssembly.Instance(wasmModule, importObject);
      }
      
      if (isDeno) {
        const path = await import(/* webpackIgnore: true */'https://deno.land/std/path/mod.ts');
        const binary = Deno.readFileSync(path.fromFileUrl(import.meta.resolve(wasmFilePath)));
        const module = await WebAssembly.compile(binary);
        wasmInstance = await WebAssembly.instantiate(module, importObject);
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
    jsModuleImports: Set<String>,
    asyncWrapperFileName: String,
    jsModuleAndQualifierReferences: MutableSet<JsModuleAndQualifierReference>,
    exports: List<WasmExport<*>>
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
${generateExports(exports)}
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

fun WasmCompiledModuleFragment.generateExports(exports: List<WasmExport<*>>): String {
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
            throw new Error("Do not use default import. Use the corresponding named import instead.")
        }
    }
});
${exportNames?.let { "export $it = exports;" }}
"""
}
