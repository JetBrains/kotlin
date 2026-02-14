/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.wasm.export.ExportModelGenerator
import org.jetbrains.kotlin.backend.wasm.ic.overrideBuiltInsSignatures
import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment.JsCodeSnippet
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
import org.jetbrains.kotlin.js.config.generateDts
import org.jetbrains.kotlin.js.config.sourceMap
import org.jetbrains.kotlin.js.config.useDebuggerCustomFormatters
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.config.wasmDebug
import org.jetbrains.kotlin.wasm.config.wasmGenerateDwarf
import org.jetbrains.kotlin.wasm.config.wasmGenerateWat
import org.jetbrains.kotlin.wasm.config.wasmNoJsTag
import org.jetbrains.kotlin.wasm.ir.ByteWriterWithOffsetWrite
import org.jetbrains.kotlin.wasm.ir.WasmBinaryData
import org.jetbrains.kotlin.wasm.ir.WasmBinaryData.Companion.writeTo
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.WasmModule
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToBinary
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToText
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationGeneratorImpl
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

data class DynamicJsModule(
    val name: String,
    val content: String,
)

class WasmCompilerResult(
    val wat: String?,
    val jsWrapper: String,
    val wasm: WasmBinaryData,
    val debugInformation: DebugInformation?,
    val dts: String?,
    val useDebuggerCustomFormatters: Boolean,
    val dynamicJsModules: List<DynamicJsModule>,
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
    exportedDeclarations: Set<FqName> = emptySet(),
): LoweredIrWithExtraArtifacts {
    val (moduleFragment, moduleDependencies, irBuiltIns, symbolTable, irLinker) = irModuleInfo

    val moduleDescriptor = moduleFragment.descriptor
    val context = WasmBackendContext(
        module = moduleDescriptor,
        irBuiltIns = irBuiltIns,
        symbolTable = symbolTable,
        irModuleFragment = moduleFragment,
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

    val typeScriptFragment = runIf(configuration.generateDts) {
        val exportModel = ExportModelGenerator(context).generateExport(allModules)
        val exportModelToDtsTranslator = ExportModelToTsDeclarations(ModuleKind.ES)
        val fragment = exportModelToDtsTranslator.generateTypeScriptFragment(exportModel.declarations)
        TypeScriptFragment(exportModelToDtsTranslator.generateTypeScript("", listOf(fragment)))
    }

    overrideBuiltInsSignatures(context)

    lowerPreservingTags(
        allModules,
        context,
        context.irFactory.stageController as WholeWorldStageController,
    )

    return LoweredIrWithExtraArtifacts(allModules, context, typeScriptFragment)
}

fun lowerPreservingTags(
    modules: Iterable<IrModuleFragment>,
    context: WasmBackendContext,
    controller: WholeWorldStageController,
) {
    // Lower all the things
    controller.currentStage = 0

    val phaserState = PhaserState()
    val wasmLowerings = wasmLowerings

    wasmLowerings.forEachIndexed { i, lowering ->
        controller.currentStage = i + 1
        modules.forEach { module ->
            lowering.invoke(context.phaseConfig, phaserState, context, module)
        }
    }

    controller.currentStage = wasmLowerings.size + 1
}

data class WasmModuleDependencyImport(val name: String, val fileName: String)

internal const val wasmInitializeExportName = "_initialize"
internal const val wasmStartExportName = "_start"
internal const val jsBuiltinsModulePrefix = "wasm:"
internal const val importedStringConstants = "'"

private fun String.normalizeEmptyLines(): String {
    return this.replace(Regex("\n\\s*\n+"), "\n\n")
}

class MultimoduleCompileOptions(
    val stdlibModuleNameForImport: String?,
    val dependencyModules: Set<WasmModuleDependencyImport>,
    val initializeUnit: Boolean,
)

class WasmIrModuleConfiguration(
    val wasmCompiledFileFragments: List<WasmCompiledFileFragment>,
    val configuration: CompilerConfiguration,
    val moduleName: String,
    val baseFileName: String,
    val typeScriptFragment: TypeScriptFragment?,
    val multimoduleOptions: MultimoduleCompileOptions?,
)

fun linkWasmIr(moduleConfiguration: WasmIrModuleConfiguration): WasmModule {
    val wasmCompiledFileFragments = moduleConfiguration.wasmCompiledFileFragments

    val configuration = moduleConfiguration.configuration

    val isWasmJsTarget = configuration.get(WasmConfigurationKeys.WASM_TARGET) != WasmTarget.WASI

    val exceptionTagType: ExceptionTagType = when {
        configuration.getBoolean(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS) ->
            ExceptionTagType.TRAP
        isWasmJsTarget -> ExceptionTagType.JS_TAG
        else -> ExceptionTagType.WASM_TAG
    }

    val multimoduleParameters = moduleConfiguration.multimoduleOptions

    val wasmCompiledModuleFragment = WasmCompiledModuleFragment(
        wasmCompiledFileFragments = wasmCompiledFileFragments,
        isWasmJsTarget = isWasmJsTarget
    )

    val wasmCommandModuleInitialization = configuration.get(WasmConfigurationKeys.WASM_COMMAND_MODULE) ?: false

    return wasmCompiledModuleFragment.linkWasmCompiledFragments(
        multimoduleOptions = multimoduleParameters,
        exceptionTagType = exceptionTagType,
        wasmCommandModuleInitialization = wasmCommandModuleInitialization,
    )
}

fun compileWasmIrToBinary(moduleConfiguration: WasmIrModuleConfiguration, linkedModule: WasmModule): WasmCompilerResult {
    val baseFileName = moduleConfiguration.baseFileName
    val configuration = moduleConfiguration.configuration
    val wasmCompiledFileFragments = moduleConfiguration.wasmCompiledFileFragments
    val multimoduleParameters = moduleConfiguration.multimoduleOptions
    val isWasmJsTarget = configuration.get(WasmConfigurationKeys.WASM_TARGET) != WasmTarget.WASI

    val wasmStartFunctionDefined = linkedModule.exports.any { it.name == wasmStartExportName }
    val wasmInitializeFunctionDefined = linkedModule.exports.any { it.name == wasmInitializeExportName }

    val dwarfGeneratorForBinary = runIf(configuration.wasmGenerateDwarf) {
        DwarfGenerator()
    }
    val sourceMapGeneratorForBinary = runIf(configuration.sourceMap) {
        SourceMapGenerator("$baseFileName.wasm", configuration)
    }

    val sourceMapGeneratorForText = runIf(configuration.wasmGenerateWat && configuration.sourceMap) {
        SourceMapGenerator("$baseFileName.wat", configuration)
    }

    val wat = if (configuration.wasmGenerateWat) {
        val watGenerator = WasmIrToText(linkedModule, sourceMapGeneratorForText)
        watGenerator.appendWasmModule()
        watGenerator.toString()
    } else {
        null
    }

    val writer = ByteWriterWithOffsetWrite()

    val wasmIrToBinary =
        WasmIrToBinary(
            writer,
            linkedModule,
            moduleConfiguration.moduleName,
            configuration.wasmDebug,
            DebugInformationGeneratorImpl.createIfNeeded(
                sourceMapGenerator = sourceMapGeneratorForBinary,
                dwarfGenerator = dwarfGeneratorForBinary,
            )
        )

    wasmIrToBinary.appendWasmModule()

    val jsWrapper: String
    val dynamicJsModules = mutableListOf<DynamicJsModule>()

    if (isWasmJsTarget) {
        val jsModuleImports = mutableSetOf<String>()
        val jsFuns = mutableSetOf<JsCodeSnippet>()
        val jsModuleAndQualifierReferences = mutableSetOf<JsModuleAndQualifierReference>()
        val jsPolyfills = mutableListOf<String>()
        wasmCompiledFileFragments.forEach { fragment ->
            (fragment as? WasmCompiledCodeFileFragment)?.serviceData?.let { serviceData ->
                jsModuleImports.addAll(serviceData.jsModuleImports.values.distinct())
                jsFuns.addAll(serviceData.jsFuns.values)
                jsModuleAndQualifierReferences.addAll(serviceData.jsModuleAndQualifierReferences)
                jsPolyfills.addAll(serviceData.jsBuiltinsPolyfills.values)
            }
        }

        if (jsPolyfills.isNotEmpty()) {
            dynamicJsModules.add(
                DynamicJsModule(
                    name = "js-builtins",
                    content = jsPolyfills.joinToString("\n")
                )
            )
        }

        val wholeProgramMode: Boolean
        val isStdlibModule: Boolean
        val stdlibModule: WasmModuleDependencyImport?
        if (multimoduleParameters != null) {
            wholeProgramMode = false
            val stdlibModuleNameForImport = multimoduleParameters.stdlibModuleNameForImport
            isStdlibModule = stdlibModuleNameForImport == null
            stdlibModule = multimoduleParameters.dependencyModules.find { it.name == stdlibModuleNameForImport }
        } else {
            wholeProgramMode = true
            isStdlibModule = false
            stdlibModule = null
        }

        val importObject = generateImportObject(
            jsModuleImports = jsModuleImports,
            jsModuleAndQualifierReferences = jsModuleAndQualifierReferences,
            dependencyModules = multimoduleParameters?.dependencyModules ?: emptySet(),
            baseFileName = baseFileName,
            jsFuns = jsFuns,
            stdlibModule = stdlibModule,
            isStdlibModule = isStdlibModule,
            useJsTag = !configuration.wasmNoJsTag,
            wholeProgramMode = wholeProgramMode
        )

        if (importObject.isNotEmpty()) {
            dynamicJsModules.add(
                DynamicJsModule(
                    name = "import-object",
                    content = importObject
                )
            )
        }

        jsWrapper = generateWebAssemblyJsInstanceInitializer(
            jsModuleImports = jsModuleImports,
            wasmFilePath = "./$baseFileName.wasm",
            exports = linkedModule.exports,
            useDebuggerCustomFormatters = configuration.useDebuggerCustomFormatters,
            baseFileName = baseFileName,
            isStdlibModule = isStdlibModule,
            wholeProgramMode = wholeProgramMode,
            wasmStartFunctionDefined = wasmStartFunctionDefined,
            wasmInitializeFunctionDefined = wasmInitializeFunctionDefined
        )

    } else {
        jsWrapper =
            generateAsyncWasiWrapper(
                wasmFilePath = "./$baseFileName.wasm",
                exports = linkedModule.exports,
                useDebuggerCustomFormatters = configuration.useDebuggerCustomFormatters,
                wasmStartFunctionDefined = wasmStartFunctionDefined,
                wasmInitializeFunctionDefined = wasmInitializeFunctionDefined
            )
    }

    return WasmCompilerResult(
        wat = wat,
        jsWrapper = jsWrapper.normalizeEmptyLines(),
        wasm = writer.getBinaryData(),
        debugInformation = DebugInformation(
            sourceMapGeneratorForBinary?.generate(),
            sourceMapGeneratorForText?.generate(),
        ),
        dts = moduleConfiguration.typeScriptFragment?.raw,
        useDebuggerCustomFormatters = configuration.useDebuggerCustomFormatters,
        dynamicJsModules = dynamicJsModules.map { it.copy(content = it.content.normalizeEmptyLines()) },
        baseFileName = baseFileName,
    )
}

//language=js
fun generateAsyncWasiWrapper(
    wasmFilePath: String,
    exports: List<WasmExport<*>>,
    useDebuggerCustomFormatters: Boolean,
    wasmStartFunctionDefined: Boolean,
    wasmInitializeFunctionDefined: Boolean
): String {
    val mainFunctionCall = if (wasmInitializeFunctionDefined)
        "wasi.initialize(wasmInstance);"
    else if (wasmStartFunctionDefined)
        "wasi.start(wasmInstance);"
    else
        "wasi.finalizeBindings(wasmInstance);"

    return """
import { WASI } from 'wasi';
import { argv, env } from 'node:process';
${if (useDebuggerCustomFormatters) "import \"./custom-formatters.js\"" else ""}

const wasi = new WASI({ version: 'preview1', args: argv, env, });

const fs = await import('node:fs');
const url = await import('node:url');
const wasmBuffer = fs.readFileSync(url.fileURLToPath(import.meta.resolve('$wasmFilePath')));
const wasmModule = new WebAssembly.Module(wasmBuffer);
const wasmInstance = new WebAssembly.Instance(wasmModule, wasi.getImportObject());

$mainFunctionCall

const exports = wasmInstance.exports
${generateExports(exports, wholeProgramMode = false, isStdlibModule = false)}
"""
}

fun generateImportObject(
    jsModuleImports: Set<String>,
    jsModuleAndQualifierReferences: MutableSet<JsModuleAndQualifierReference>,
    dependencyModules: Set<WasmModuleDependencyImport>,
    baseFileName: String,
    stdlibModule: WasmModuleDependencyImport?,
    jsFuns: Set<JsCodeSnippet>,
    isStdlibModule: Boolean,
    useJsTag: Boolean,
    wholeProgramMode: Boolean
): String {
    val stdlibModuleOrWholeProgramMode = isStdlibModule || wholeProgramMode

    val imports = generateJsImports(
        jsModuleImports,
        jsModuleAndQualifierReferences,
        dependencyModules,
        baseFileName,
        stdlibModuleOrWholeProgramMode,
        stdlibModule,
    )

    val jsCodeBody = jsFuns.joinToString(",\n") {
        "${it.importName.owner.toJsStringLiteral()} : ${it.jsCode}"
    }
    val jsCodeBodyIndented = jsCodeBody.prependIndent("    ")

    val referencesToQualifiedAndImportedDeclarations = jsModuleAndQualifierReferences
        .map {
            val module = it.module
            val qualifier = it.qualifier
            buildString {
                append("const ")
                append(it.jsReference)
                append(" = ")
                if (module != null) {
                    append(JsModuleAndQualifierReference.encode(module))
                    if (qualifier != null) {
                        append(".")
                    }
                }
                if (qualifier != null) {
                    append(qualifier)
                }
                append(";")
            }
        }.sorted()
        .joinToString("\n")

    val wasmTagInitialization =
        if (stdlibModuleOrWholeProgramMode)
            """
            const wasmJsTag = ${if (useJsTag) "WebAssembly.JSTag" else "void 0"};
            const wasmTag = wasmJsTag ?? new WebAssembly.Tag({ parameters: ['externref'] });
        """.trimIndent()
        else
            ""
    val getCachedJsObject =
        if (stdlibModuleOrWholeProgramMode)
            """
    const cachedJsObjects = new WeakMap();
    ${if (isStdlibModule) "export " else ""}function getCachedJsObject(ref, ifNotCached) {
        if (typeof ref !== 'object' && typeof ref !== 'function') return ifNotCached;
        const cached = cachedJsObjects.get(ref);
        if (cached !== void 0) return cached;
        cachedJsObjects.set(ref, ifNotCached);
        return ifNotCached;
    }
            """.trimIndent()
        else
            ""

    val importObject = generateImportObjectBody(jsModuleImports, dependencyModules)

    return """
$imports
$wasmTagInitialization

// Placed here to give access to it from externals (js_code)
let wasmExports;
let require;

if (typeof process !== 'undefined' && process.release.name === 'node') {
    const module = await import(/* webpackIgnore: true */'node:module');
    const importMeta = import.meta;
    require = module.default.createRequire(importMeta.url);
}

export function setWasmExports(exports) {
    wasmExports = exports;
}

$referencesToQualifiedAndImportedDeclarations

$getCachedJsObject

const js_code = {
$jsCodeBodyIndented
}

const StringConstantsProxy = new Proxy({}, {
  get(_, prop) { return prop; }
});

${if (stdlibModuleOrWholeProgramMode) "export { wasmTag as __TAG };" else ""}

$importObject
    """
}

fun generateJsImports(
    jsModuleImports: Set<String>,
    jsModuleAndQualifierReferences: MutableSet<JsModuleAndQualifierReference>,
    dependencyModules: Set<WasmModuleDependencyImport>,
    baseFileName: String,
    stdlibModuleOrWholeProgramMode: Boolean,
    stdlibModule: WasmModuleDependencyImport?,
): String {
    val dependenciesImports = dependencyModules
        .map {
            val importVariableString = JsModuleAndQualifierReference.encode(it.name)
            "\'./${it.fileName}.mjs\'" to importVariableString
        }

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
            stringLiteral to JsModuleAndQualifierReference.encode(module)
        }

    val allModules = (importedModules + referencesToImportedDeclarations + dependenciesImports)
        .distinctBy {
            it.first
        }.sortedBy { it.first }

    val importsImportedSection = allModules.joinToString("\n") {
        buildString {
            append("import * as ")
            append(it.second)
            append(" from ")
            append(if (it.first.contains(jsBuiltinsModulePrefix)) "\'./${baseFileName}.js-builtins.mjs\'" else it.first)
            append(";")
        }
    }

    val orderedDependenciesImports =
        dependenciesImports.distinctBy { it.first }.sortedBy { it.first }

    val importsDependenciesExportsSection = orderedDependenciesImports.joinToString("\n") {
        "import { __ALL_EXPORTS as ${it.second}__ALL_EXPORTS } from ${it.first};"
    }

    /*language=js */
    return """
$importsImportedSection
$importsDependenciesExportsSection
${if (!stdlibModuleOrWholeProgramMode) "import { __TAG as wasmTag, getCachedJsObject } from \'./${stdlibModule!!.fileName}.mjs\'" else ""}
""".trimIndent()
}

fun generateImportObjectBody(
    jsModuleImports: Set<String>,
    dependencyModules: Set<WasmModuleDependencyImport>,
): String {

    val dependencyImports = dependencyModules
        .map {
            val moduleSpecifier = it.name.toJsStringLiteral().toString()
            val importVariableString = JsModuleAndQualifierReference.encode(it.name)
            moduleSpecifier to importVariableString
        }.joinToString("") {
            "    ${it.first}: ${it.second}__ALL_EXPORTS,\n"
        }

    val jsImports = jsModuleImports
        .map {
            val moduleSpecifier = it.toJsStringLiteral().toString()
            val importVariableString = JsModuleAndQualifierReference.encode(it)
            moduleSpecifier to importVariableString
        }.joinToString("") {
            "    ${it.first}: ${it.second},\n"
        }

    val imports = dependencyImports + jsImports

    return """
export const importObject = {
    js_code,
    intrinsics: {
        tag: wasmTag
    },
    "$importedStringConstants": StringConstantsProxy,
$imports};
    """.trimIndent()
}

fun generateWebAssemblyJsInstanceInitializer(
    jsModuleImports: Set<String>,
    wasmFilePath: String,
    exports: List<WasmExport<*>>,
    useDebuggerCustomFormatters: Boolean,
    baseFileName: String,
    isStdlibModule: Boolean,
    wholeProgramMode: Boolean,
    wasmStartFunctionDefined: Boolean,
    wasmInitializeFunctionDefined: Boolean
): String {

    val commonStdlibExports = if (isStdlibModule) ", getCachedJsObject, __TAG as wasmTag" else ""

    val mainFunctionCall = if (wasmStartFunctionDefined)
        "exports.$wasmStartExportName();"
    else if (wasmInitializeFunctionDefined)
        "exports.$wasmInitializeExportName();"
    else
        ""

    val staticImports = """
${if (useDebuggerCustomFormatters) "import \"./custom-formatters.js\"" else ""}
import { importObject, setWasmExports$commonStdlibExports } from './${baseFileName}.import-object.mjs'
    """.trimIndent()

    val builtinsList = jsModuleImports.filter { it.startsWith(jsBuiltinsModulePrefix) }.map { it.removePrefix(jsBuiltinsModulePrefix) }

    val pathJsStringLiteral = wasmFilePath.toJsStringLiteral()

    return """
$staticImports

let wasmInstance;

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
const wasmOptions = { builtins: ['${builtinsList.joinToString(", ")}'], importedStringConstants: "$importedStringConstants" }

try {
  if (isNodeJs) {
    const module = await import(/* webpackIgnore: true */'node:module');
    const importMeta = import.meta;
    const require = module.default.createRequire(importMeta.url);
    const fs = require('fs');
    const url = require('url');
    const filepath = import.meta.resolve(wasmFilePath);
    const wasmBuffer = fs.readFileSync(url.fileURLToPath(filepath));
    const wasmModule = new WebAssembly.Module(wasmBuffer, wasmOptions);
    wasmInstance = new WebAssembly.Instance(wasmModule, importObject);
  }

  if (isDeno) {
    const path = await import(/* webpackIgnore: true */'https://deno.land/std/path/mod.ts');
    const binary = Deno.readFileSync(path.fromFileUrl(import.meta.resolve(wasmFilePath)));
    const module = await WebAssembly.compile(binary, wasmOptions);
    wasmInstance = await WebAssembly.instantiate(module, importObject);
  }

  if (isStandaloneJsVM) {
    const importMeta = import.meta;
    const filepath = importMeta.url.replace(/\.mjs$/, '.wasm');
    const wasmBuffer = read(filepath, 'binary');
    const wasmModule = new WebAssembly.Module(wasmBuffer, wasmOptions);
    wasmInstance = new WebAssembly.Instance(wasmModule, importObject);
  }

  if (isBrowser) {
    wasmInstance = (await WebAssembly.instantiateStreaming(fetch(new URL($pathJsStringLiteral,import.meta.url).href), importObject, wasmOptions)).instance;
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

const exports = wasmInstance.exports
setWasmExports(exports);
$mainFunctionCall

${generateExports(exports, wholeProgramMode, isStdlibModule)}
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
    result.wasm.writeTo(File(dir, "$fileNameBase.wasm"))

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

    for (dynamicJsModule in result.dynamicJsModules) {
        File(dir, "${fileNameBase}.${dynamicJsModule.name}.mjs").writeText(dynamicJsModule.content)
    }
}

private val WasmExport<*>.isWasmInternalUsageExport
    get() = name.startsWith(JsInteropFunctionsLowering.CALL_FUNCTION) ||
            WasmServiceImportExportKind.entries.any { name.startsWith(it.prefix) }

fun generateExports(
    exports: List<WasmExport<*>>,
    wholeProgramMode: Boolean,
    isStdlibModule: Boolean,
): String {
    // TODO: necessary to move export check onto common place
    val exportNames = exports
        .filterNot { it.isWasmInternalUsageExport }

    val (validIdentifiers, notValidIdentifiers) = exportNames.partition { it.name.isValidES5Identifier() }
    val regularlyExportedVariables = validIdentifiers
        .ifNotEmpty {
            """
            |export const {
            |${joinToString(",\n") { it.name }.prependIndent("    ")}
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
            |${joinToString(",\n") { "'${it.second}': ${it.first}" }.prependIndent("    ")}
            |} = exports
            |
            |export {
            |${joinToString(",\n") { "${it.first} as '${it.second}'" }.prependIndent("    ")}
            |}
            """.trimMargin()
        }
        .orEmpty()

    val commonStdlibExports =
        if (isStdlibModule)
            """
            |    wasmTag as __TAG,
            |    getCachedJsObject,
            """.trimMargin()
        else
            ""

    val exportsStructureSingleModule = """
        |export {
        |    exports as __ALL_EXPORTS,
        |$commonStdlibExports
        |}
    """.trimMargin()

    /*language=js */
    return """
${if (!wholeProgramMode) exportsStructureSingleModule else ""}

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
