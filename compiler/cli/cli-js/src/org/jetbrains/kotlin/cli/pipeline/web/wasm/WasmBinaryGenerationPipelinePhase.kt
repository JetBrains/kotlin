/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.*
import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment.JsCodeSnippet
import org.jetbrains.kotlin.backend.wasm.lower.JsInteropFunctionsLowering
import org.jetbrains.kotlin.backend.wasm.utils.DwarfGenerator
import org.jetbrains.kotlin.backend.wasm.utils.SourceMapGenerator
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WasmIntermediatePipelineArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.tryAcquireAndRelease
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.js.config.sourceMap
import org.jetbrains.kotlin.js.config.useDebuggerCustomFormatters
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.*
import org.jetbrains.kotlin.wasm.ir.ByteWriterWithOffsetWrite
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.WasmModule
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToBinary
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToText
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationGeneratorImpl

object WasmBinaryGenerationPipelinePhase : PipelinePhase<WasmIntermediatePipelineArtifact, WasmBackendPipelineArtifact>(
    name = "WasmBinaryGenerationPipelinePhase",
    preActions = setOf(PerformanceNotifications.BackendStarted),
    postActions = setOf(PerformanceNotifications.BackendFinished),
) {
    override fun executePhase(input: WasmIntermediatePipelineArtifact): WasmBackendPipelineArtifact {
        (val backendIr, val cacheGuard, val configuration) = input
        val outputDir = configuration.outputDir!!
        return cacheGuard.tryAcquireAndRelease {
            val results = backendIr.map { result ->
                val linkedModule = linkWasmIr(result)
                compileWasmIrToBinary(result, linkedModule)
            }
            WasmBackendPipelineArtifact(results, outputDir, configuration)
        }
    }

    private fun linkWasmIr(moduleConfiguration: WasmIrModuleConfiguration): WasmModule {
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

        val useStackSwitching = configuration.wasmUseStackSwitchingProposal

        return wasmCompiledModuleFragment.linkWasmCompiledFragments(
            multimoduleOptions = multimoduleParameters,
            exceptionTagType = exceptionTagType,
            wasmCommandModuleInitialization = wasmCommandModuleInitialization,
            useStackSwitching = useStackSwitching,
            importWasmMemoryInsteadOfExport = isWasmJsTarget,
        )
    }

    private fun String.normalizeEmptyLines(): String {
        return this.replace(Regex("\n\\s*\n+"), "\n\n")
    }

    private const val wasmInitializeExportName = "_initialize"
    private const val wasmStartExportName = "_start"
    private const val jsBuiltinsModulePrefix = "wasm:"
    private const val importedStringConstants = "'"

    private fun compileWasmIrToBinary(moduleConfiguration: WasmIrModuleConfiguration, linkedModule: WasmModule): WasmCompilerResult {
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
                (fragment as? WasmCompiledCodeFileFragment)?.linkerData?.let { linkerData ->
                    jsModuleImports.addAll(linkerData.jsModuleImports.values.distinct())
                    jsFuns.addAll(linkerData.jsFuns.values)
                    jsModuleAndQualifierReferences.addAll(linkerData.jsModuleAndQualifierReferences)
                    jsPolyfills.addAll(linkerData.jsBuiltinsPolyfills.values)
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
                wasmInitializeFunctionDefined = wasmInitializeFunctionDefined,
                stdlibModule = stdlibModule,
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
            linkedModule = linkedModule,
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

    private fun generateWebAssemblyJsInstanceInitializer(
        jsModuleImports: Set<String>,
        wasmFilePath: String,
        exports: List<WasmExport<*>>,
        useDebuggerCustomFormatters: Boolean,
        baseFileName: String,
        isStdlibModule: Boolean,
        wholeProgramMode: Boolean,
        wasmStartFunctionDefined: Boolean,
        wasmInitializeFunctionDefined: Boolean,
        stdlibModule: WasmModuleDependencyImport?
    ): String {

        val commonStdlibExports = if (isStdlibModule) ", getCachedJsObject, __TAG as wasmTag" else ""

        val stdlibExports = if (isStdlibModule || wholeProgramMode) """
let memoryFirstTimeAccess = true;
const memoryProxy = new Proxy(importObject.intrinsics.memory, {
    get(target, prop, receiver) {
        if (memoryFirstTimeAccess) {
            memoryFirstTimeAccess = false;
            console.error('Accessing `memory` via `wasmExports` is deprecated. Use `kotlin.wasm.unsafe.wasmMemory` or update dependencies. Read more: https://kotl.in/vr3szr');
        }
        return Reflect.get(target, prop);
    }
});
const wasmExports = new Proxy(memoryProxy, {
    get(target, prop, receiver) {
        if (prop == 'memory') {
            return target;
        } else {
            throw new Error('Accessing exports via `wasmExports` is no longer supported. Remove usages or update dependencies. Read more: https://kotl.in/vr3szr');
        }
    }
});
""" else ""
        val mainFunctionCall = if (wasmStartFunctionDefined)
            "exports.$wasmStartExportName();"
        else if (wasmInitializeFunctionDefined)
            "exports.$wasmInitializeExportName();"
        else ""


        val wasmExportsIfNeeded = stdlibModule?.fileName?.let { "import { wasmExports } from './$it.mjs'" } ?: ""

        val staticImports = """
${if (useDebuggerCustomFormatters) "import \"./custom-formatters.js\"" else ""}
import { importObject, setWasmExports$commonStdlibExports } from './${baseFileName}.import-object.mjs'
$wasmExportsIfNeeded
    """.trimIndent()

        val builtinsList = jsModuleImports.filter { it.startsWith(jsBuiltinsModulePrefix) }.map { it.removePrefix(jsBuiltinsModulePrefix) }

        val pathJsStringLiteral = wasmFilePath.toJsStringLiteral()

        return """
$staticImports

let wasmInstance;

const wasmOptions = { builtins: ['${builtinsList.joinToString(", ")}'], importedStringConstants: "$importedStringConstants" }

try {
  if ((typeof process !== 'undefined') && (process.release.name === 'node')) {
    const fs = await import(/* webpackIgnore: true */'node:fs');
    const url = await import(/* webpackIgnore: true */'node:url');
    const filepath = import.meta.resolve($pathJsStringLiteral);
    const wasmBuffer = fs.readFileSync(url.fileURLToPath(filepath));
    const wasmModule = new WebAssembly.Module(wasmBuffer, wasmOptions);
    wasmInstance = new WebAssembly.Instance(wasmModule, importObject);
  } else if (typeof Deno !== 'undefined') {
    const path = await import(/* webpackIgnore: true */'https://deno.land/std/path/mod.ts');
    const binary = Deno.readFileSync(path.fromFileUrl(import.meta.resolve($pathJsStringLiteral)));
    const module = await WebAssembly.compile(binary, wasmOptions);
    wasmInstance = await WebAssembly.instantiate(module, importObject);
  } else if (
    (typeof d8 !== 'undefined' // V8
      || typeof inIon !== 'undefined' // SpiderMonkey
      || typeof jscOptions !== 'undefined' // JavaScriptCore
    )
  ) {
    const filepath = import.meta.url.replace(/\.mjs$/, '.wasm');
    const wasmBuffer = read(filepath, 'binary');
    const wasmModule = new WebAssembly.Module(wasmBuffer, wasmOptions);
    wasmInstance = new WebAssembly.Instance(wasmModule, importObject);
  } else {
    wasmInstance = (await WebAssembly.instantiateStreaming(fetch(new URL($pathJsStringLiteral,import.meta.url).href), importObject, wasmOptions)).instance;
  }
} catch (e) {
  if (e instanceof WebAssembly.CompileError) {
    let text = `Please make sure that your runtime environment supports the latest version of Wasm GC and Exception-Handling proposals.
For more information, see https://kotl.in/wasm-help
`;
    if (typeof console !== "undefined" && console.error !== void 0) {
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


$stdlibExports
${generateExports(exports, wholeProgramMode, isStdlibModule, wasmExportsName = "wasmExports")}

setWasmExports(wasmExports);

$mainFunctionCall
"""
    }

    //language=js
    private fun generateAsyncWasiWrapper(
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
${generateExports(exports, wholeProgramMode = false, isStdlibModule = false, wasmExportsName = "exports")}
"""
    }

    private fun generateImportObject(
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

        val importObject = generateImportObjectBody(stdlibModuleOrWholeProgramMode, jsModuleImports, dependencyModules)

        return """
$imports
$wasmTagInitialization

// Placed here to give access to it from externals (js_code)
let wasmExports;

if (typeof process !== 'undefined' && process.release.name === 'node') {
    function doNotUseRequire() {
        throw new Error("Do not use top-level require. Prefer to use JS import or define your own require instead. Read more: https://kotl.in/r9txlt")
    }

    var require = new Proxy((function() {}), {
        apply(target, thisArg, argumentsList) {
            if (globalThis.require != null) {
                return globalThis.require.apply(thisArg, argumentsList);
            } else {
                doNotUseRequire();
            }
        },
        get(target, prop, receiver) {
            if (globalThis.require != null) {
                return Reflect.get(globalThis.require, prop);
            } else {
                doNotUseRequire();
            }
        },
    });
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

    private fun generateJsImports(
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

    private fun generateImportObjectBody(
        stdlibModuleOrWholeProgramMode: Boolean,
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
        ${if (stdlibModuleOrWholeProgramMode) "memory: new WebAssembly.Memory({ initial: 0 })," else ""}
        tag: wasmTag
    },
    "${importedStringConstants}": StringConstantsProxy,
$imports};
    """.trimIndent()
    }

    private fun generateExports(
        exports: List<WasmExport<*>>,
        wholeProgramMode: Boolean,
        isStdlibModule: Boolean,
        wasmExportsName: String,
    ): String {
        // TODO: necessary to move export check onto common place
        val exportNames = exports
            .filterNot { it.isWasmInternalUsageExport }

        val [validIdentifiers, notValidIdentifiers] = exportNames.partition { it.name.isValidES5Identifier() }
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
            |    $wasmExportsName,
            """.trimMargin()
            else ""

        val exportsStructureSingleModule = """
        |export {
        |    exports as __ALL_EXPORTS,
        |$commonStdlibExports
        |}
    """.trimMargin()

        /*language=js */
        return """
${if (!wholeProgramMode) exportsStructureSingleModule else ""}

const wasmMemory = $wasmExportsName.memory;
export { wasmMemory as memory }

$regularlyExportedVariables
$escapedExportedVariables
"""
    }

    private val WasmExport<*>.isWasmInternalUsageExport
        get() = name.startsWith(JsInteropFunctionsLowering.CALL_FUNCTION) ||
                name == "memory" ||
                WasmServiceImportExportKind.entries.any { name.startsWith(it.prefix) }

    private fun generateShortNameByIndex(index: Int): String {
        val lettersNumber = 26
        val letterName = ('a'.code + index % lettersNumber).toChar()
        val number = index / lettersNumber
        return if (number == 0) letterName.toString() else "$letterName$number"
    }
}
