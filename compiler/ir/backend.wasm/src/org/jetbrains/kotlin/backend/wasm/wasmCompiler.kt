/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.wasm.export.ExportModelGenerator
import org.jetbrains.kotlin.backend.wasm.ir2wasm.JsModuleAndQualifierReference
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment.JsCodeSnippet
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmServiceImportExportKind
import org.jetbrains.kotlin.backend.wasm.ir2wasm.toJsStringLiteral
import org.jetbrains.kotlin.backend.wasm.lower.JsInteropFunctionsLowering
import org.jetbrains.kotlin.backend.wasm.lower.markExportedDeclarations
import org.jetbrains.kotlin.backend.wasm.utils.DwarfGenerator
import org.jetbrains.kotlin.backend.wasm.utils.SourceMapGenerator
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportModelToTsDeclarations
import org.jetbrains.kotlin.ir.backend.js.tsexport.TypeScriptFragment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToBinary
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToText
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationGeneratorImpl
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class WasmCompilerResult(
    val wat: String?,
    val jsUninstantiatedWrapper: String?,
    val jsWrapper: String,
    val wasm: ByteArray,
    val debugInformation: DebugInformation?,
    val dts: String?,
    val useDebuggerCustomFormatters: Boolean,
    val jsBuiltinsPolyfillsWrapper: String?,
    val baseFileName: String,
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
    performanceManager: PerformanceManager?,
    exportedDeclarations: Set<FqName> = emptySet(),
    generateTypeScriptFragment: Boolean,
    propertyLazyInitialization: Boolean,
    disableCrossFileOptimisations: Boolean = false,
): LoweredIrWithExtraArtifacts {
    val (moduleFragment, moduleDependencies, irBuiltIns, symbolTable, irLinker) = irModuleInfo

    val moduleDescriptor = moduleFragment.descriptor
    val context = WasmBackendContext(
        module = moduleDescriptor,
        irBuiltIns = irBuiltIns,
        symbolTable = symbolTable,
        irModuleFragment = moduleFragment,
        propertyLazyInitialization = propertyLazyInitialization,
        configuration = configuration,
    )

    // Create stubs
    ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()

    // Sort dependencies after IR linkage.
    val sortedModuleDependencies = irLinker.moduleDependencyTracker.reverseTopoOrder(moduleDependencies)

    val allModules = when (mainModule) {
        is MainModule.SourceFiles -> sortedModuleDependencies.all + moduleFragment
        is MainModule.Klib -> sortedModuleDependencies.all
    }

    allModules.forEach { it.patchDeclarationParents() }

    irLinker.postProcess(inOrAfterLinkageStep = true)
    irLinker.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")
    irLinker.clear()

    for (module in allModules)
        for (file in module.files)
            markExportedDeclarations(context, file, exportedDeclarations)

    val typeScriptFragment = runIf(generateTypeScriptFragment) {
        val exportModel = ExportModelGenerator(context).generateExport(allModules)
        val exportModelToDtsTranslator = ExportModelToTsDeclarations(ModuleKind.ES)
        val fragment = exportModelToDtsTranslator.generateTypeScriptFragment(exportModel.declarations)
        TypeScriptFragment(exportModelToDtsTranslator.generateTypeScript("", listOf(fragment)))
    }
    performanceManager?.notifyPhaseFinished(PhaseType.TranslationToIr)

    performanceManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
        lowerPreservingTags(
            allModules,
            context,
            context.irFactory.stageController as WholeWorldStageController,
            disableCrossFileOptimisations = disableCrossFileOptimisations,
        )
    }

    return LoweredIrWithExtraArtifacts(allModules, context, typeScriptFragment)
}

fun lowerPreservingTags(
    modules: Iterable<IrModuleFragment>,
    context: WasmBackendContext,
    controller: WholeWorldStageController,
    disableCrossFileOptimisations: Boolean,
) {
    // Lower all the things
    controller.currentStage = 0

    val phaserState = PhaserState()
    val wasmLowerings = getWasmLowerings(context.configuration, disableCrossFileOptimisations)

    wasmLowerings.forEachIndexed { i, lowering ->
        controller.currentStage = i + 1
        modules.forEach { module ->
            lowering.invoke(context.phaseConfig, phaserState, context, module)
        }
    }

    controller.currentStage = wasmLowerings.size + 1
}

data class WasmModuleDependencyImport(val name: String, val fileName: String)

fun compileWasm(
    wasmCompiledFileFragments: List<WasmCompiledFileFragment>,
    moduleName: String,
    configuration: CompilerConfiguration,
    typeScriptFragment: TypeScriptFragment?,
    baseFileName: String,
    emitNameSection: Boolean,
    generateWat: Boolean,
    generateSourceMaps: Boolean,
    useDebuggerCustomFormatters: Boolean,
    generateDwarf: Boolean,
    stdlibModuleNameForImport: String? = null,
    dependencyModules: Set<WasmModuleDependencyImport> = emptySet(),
    initializeUnit: Boolean = true,
    singleModulePreloadJs: String? = null,
): WasmCompilerResult {
    val isWasmJsTarget = configuration.get(WasmConfigurationKeys.WASM_TARGET) != WasmTarget.WASI

    val wasmCompiledModuleFragment = WasmCompiledModuleFragment(
        wasmCompiledFileFragments,
        configuration.getBoolean(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS),
        isWasmJsTarget,
        useSharedObjects = configuration.getBoolean(WasmConfigurationKeys.WASM_USE_SHARED_OBJECTS),
    )

    val linkedModule = wasmCompiledModuleFragment.linkWasmCompiledFragments(stdlibModuleNameForImport, initializeUnit)

    val dwarfGeneratorForBinary = runIf(generateDwarf) {
        DwarfGenerator()
    }
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
            DebugInformationGeneratorImpl.createIfNeeded(
                sourceMapGenerator = sourceMapGeneratorForBinary,
                dwarfGenerator = dwarfGeneratorForBinary,
            ),
            useSharedObjects = configuration.getBoolean(WasmConfigurationKeys.WASM_USE_SHARED_OBJECTS)
        )

    wasmIrToBinary.appendWasmModule()

    val byteArray = os.toByteArray()
    val jsUninstantiatedWrapper: String?
    val jsWrapper: String
    val jsBuiltinsPolyfillsWrapper: String?

    if (isWasmJsTarget) {
        val jsModuleImports = mutableSetOf<String>()
        val jsFuns = mutableSetOf<JsCodeSnippet>()
        val jsModuleAndQualifierReferences = mutableSetOf<JsModuleAndQualifierReference>()
        wasmCompiledFileFragments.forEach { fragment ->
            jsModuleImports.addAll(fragment.jsModuleImports.values.distinct())
            jsFuns.addAll(fragment.jsFuns.values)
            jsModuleAndQualifierReferences.addAll(fragment.jsModuleAndQualifierReferences)
        }

        val useJsTag = !configuration.getBoolean(WasmConfigurationKeys.WASM_NO_JS_TAG)
        val useSharedObjects = configuration.getBoolean(WasmConfigurationKeys.WASM_USE_SHARED_OBJECTS)

        jsUninstantiatedWrapper = generateAsyncJsWrapper(
            jsModuleImports,
            jsFuns,
            dependencyModules,
            "./$baseFileName.wasm",
            jsModuleAndQualifierReferences,
            useJsTag,
            useSharedObjects,
            baseFileName,
            singleModulePreloadJs,
        )
        jsWrapper = generateEsmExportsWrapper(
            "./$baseFileName.uninstantiated.mjs",
            linkedModule.exports,
            useDebuggerCustomFormatters,
        )
        jsBuiltinsPolyfillsWrapper = wasmCompiledFileFragments.flatMap { fragment ->
            fragment.jsBuiltinsPolyfills.values.toList()
        }.joinToString("\n").takeIf { it.isNotEmpty() }
    } else {
        jsUninstantiatedWrapper = null
        jsWrapper =
            wasmCompiledModuleFragment.generateAsyncWasiWrapper("./$baseFileName.wasm", linkedModule.exports, useDebuggerCustomFormatters)
        jsBuiltinsPolyfillsWrapper = null
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
        dts = typeScriptFragment?.raw,
        useDebuggerCustomFormatters = useDebuggerCustomFormatters,
        jsBuiltinsPolyfillsWrapper = jsBuiltinsPolyfillsWrapper,
        baseFileName = baseFileName,
    )
}

//language=js
fun WasmCompiledModuleFragment.generateAsyncWasiWrapper(
    wasmFilePath: String,
    exports: List<WasmExport<*>>,
    useCustomFormatters: Boolean
): String = """
import { WASI } from 'wasi';
import { argv, env } from 'node:process';
${if (useCustomFormatters) "import \"./custom-formatters.js\"" else ""}

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
    dependenciesModules: Set<WasmModuleDependencyImport>,
    wasmFilePath: String,
    jsModuleAndQualifierReferences: Set<JsModuleAndQualifierReference>,
    useJsTag: Boolean,
    useSharedObjects: Boolean,
    baseFileName: String,
    singleModulePreloadJs: String?,
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

    val allModuleImports = jsModuleImports + jsModuleAndQualifierReferences.mapNotNull { it.module }
    val importModuleLoaders = allModuleImports.joinToString("\n") {
        val moduleSpecifier = it.toJsStringLiteral()
        buildString {
            append("    imports[$moduleSpecifier] = imports[$moduleSpecifier] ?? await import(")
            append(if (it.contains("wasm:")) "\'./${baseFileName}.js-builtins.mjs\'" else moduleSpecifier)
            append(");")
        }
    }

    val dependenciesImports = dependenciesModules
        .joinToString("") {
            val moduleSpecifier = it.name.toJsStringLiteral()
            "        $moduleSpecifier: imports[$moduleSpecifier],\n"
        }

    val dependenciesLoaders = dependenciesModules
        .joinToString("") { import ->
            val moduleSpecifier = import.name.toJsStringLiteral()
            "    imports[$moduleSpecifier] = imports[$moduleSpecifier] ?? (await (await import('./${import.fileName}.uninstantiated.mjs')).instantiate(imports, true)).exports;\n"
        }

    val initExternrefTableIfNeeded = if (useSharedObjects) """
    const externrefTable = new WebAssembly.Table({ element: "externref", initial: 1 });"""
    else ""
    val importExternrefTableIfNeeded = if (useSharedObjects) "            externrefTable: externrefTable," else ""

    val referencesToQualifiedAndImportedDeclarations = jsModuleAndQualifierReferences
        .map {
            val module = it.module
            val qualifier = it.qualifier
            buildString {
                append("    const ")
                append(it.jsReference)
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
    val pathJsStringLiteral = wasmFilePath.toJsStringLiteral()

    val builtinsList = jsModuleImports.filter { it.startsWith("wasm:") }.map { "${it.removePrefix("wasm:")}" }
    val options = "{ builtins: ['${builtinsList.joinToString(", ")}'] }"

    return """
let moduleInstanceCounter = 0;

export async function instantiate(imports={}, runInitializer=true) {
    imports['_cachedJsObjects_'] = imports['_cachedJsObjects_'] ?? new WeakMap();
    const cachedJsObjects = imports['_cachedJsObjects_'];

    // ref must be non-null
    function getCachedJsObject(ref, ifNotCached) {
        if (typeof ref !== 'object' && typeof ref !== 'function') return ifNotCached;
        const cached = cachedJsObjects.get(ref);
        if (cached !== void 0) return cached;
        cachedJsObjects.set(ref, ifNotCached);
        return ifNotCached;
    }
${singleModulePreloadJs ?: ""}
$dependenciesLoaders
$importModuleLoaders

$referencesToQualifiedAndImportedDeclarations

    ${
        // Save WebAssembly.JSTag into a local variable to work around [a problem in JavaScriptCore](https://bugs.webkit.org/show_bug.cgi?id=297126), 
        // which doesn't allow us to check if JSTag is used as a tag inside a wasm module.
        ""
    }const wasmJsTag = ${if (useJsTag) "WebAssembly.JSTag" else "void 0"};
    const wasmTag = wasmJsTag ?? new WebAssembly.Tag({ parameters: ['externref'] });

    const moduleInstanceId = moduleInstanceCounter++;

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

    const wasmFilePath = $pathJsStringLiteral;
$initExternrefTableIfNeeded

    const importObject = {
        js_code,
        intrinsics: {
            tag: wasmTag,
            moduleInstanceId: new WebAssembly.Global({ value: "i32", mutable: false }, moduleInstanceId),
$importExternrefTableIfNeeded
        },
$imports
$dependenciesImports
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
        wasmInstance = new WebAssembly.Instance(wasmModule, importObject, $options);
      }
      
      if (isDeno) {
        const path = await import(/* webpackIgnore: true */'https://deno.land/std/path/mod.ts');
        const binary = Deno.readFileSync(path.fromFileUrl(import.meta.resolve(wasmFilePath)));
        const module = await WebAssembly.compile(binary);
        wasmInstance = await WebAssembly.instantiate(module, importObject, $options);
      }
      
      if (isStandaloneJsVM) {
        const wasmBuffer = read(wasmFilePath, 'binary');
        const wasmModule = new WebAssembly.Module(wasmBuffer);
        wasmInstance = new WebAssembly.Instance(wasmModule, importObject, $options);
      }
      
      if (isBrowser) {
        wasmInstance = (await WebAssembly.instantiateStreaming(fetch(new URL($pathJsStringLiteral,import.meta.url).href), importObject, $options)).instance;
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

fun generateEsmExportsWrapper(
    asyncWrapperFileName: String,
    exports: List<WasmExport<*>>,
    useCustomFormatters: Boolean,
): String {
    /*language=js */
    return """
import { instantiate } from ${asyncWrapperFileName.toJsStringLiteral()};
${if (useCustomFormatters) "import \"./custom-formatters.js\"" else ""}

const exports = (await instantiate({
})).exports;
${generateExports(exports)}
"""
}

fun writeCompilationResult(
    result: WasmCompilerResult,
    dir: File,
    fileNameBase: String,
    messageCollector: MessageCollector? = null
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
    if (result.useDebuggerCustomFormatters) {
        val fileName = "custom-formatters.js"
        val classLoader = WasmCompilerResult::class.java.classLoader
        val customFormattersInputStream = classLoader.getResourceAsStream(fileName) ?: run {
            val message = "Custom formatters won't work because a required resource is missing from the compiler: $fileName"
            messageCollector?.report(
                CompilerMessageSeverity.STRONG_WARNING,
                message
            )
            "console.warn(\"$message\");".byteInputStream()
        }

        Files.copy(customFormattersInputStream, Paths.get(dir.path, fileName), StandardCopyOption.REPLACE_EXISTING)
    }

    if (result.dts != null) {
        File(dir, "$fileNameBase.d.mts").writeText(result.dts)
    }

    if (result.jsBuiltinsPolyfillsWrapper != null) {
        File(dir, "${fileNameBase}.js-builtins.mjs").writeText(result.jsBuiltinsPolyfillsWrapper)
    }
}

private val WasmExport<*>.isWasmInternalUsageExport
    get() = name.startsWith(JsInteropFunctionsLowering.CALL_FUNCTION) ||
            WasmServiceImportExportKind.entries.any { name.startsWith(it.prefix) }

fun generateExports(exports: List<WasmExport<*>>): String {
    // TODO: necessary to move export check onto common place
    val exportNames = exports
        .filterNot { it.isWasmInternalUsageExport }

    val (validIdentifiers, notValidIdentifiers) = exportNames.partition { it.name.isValidES5Identifier() }
    val regularlyExportedVariables = validIdentifiers
        .ifNotEmpty {
            """
            |export const {
                |${joinToString(",\n") { it.name }}
            |} = exports
            """.trimMargin()
        }
        .orEmpty()

    val escapedExportedVariables = notValidIdentifiers
        .mapIndexed { index, it ->
            generateShortNameByIndex(index) to it.name.replace("'", "\\'")
        }
        .ifNotEmpty {
            /*language=js */
            """
            |const {
                |${joinToString(",\n") { "'${it.second}': ${it.first}" }}
            |} = exports
            |
            |export {
                |${joinToString(",\n") { "${it.first} as '${it.second}'" }}
            |}
            """.trimMargin()
        }
        .orEmpty()

    /*language=js */
    return """
$regularlyExportedVariables
$escapedExportedVariables
"""
}

private fun generateShortNameByIndex(index: Int): String {
    val lettersNumber = 26
    val letterName = ('a'.code + index % lettersNumber).toChar()
    val number = index / lettersNumber
    return if (number == 0) letterName.toString() else "$letterName$number"
}
