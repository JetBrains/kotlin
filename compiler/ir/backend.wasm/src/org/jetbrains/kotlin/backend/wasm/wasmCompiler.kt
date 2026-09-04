/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.serialization.IrModuleDependencyTrackerImpl
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.backend.wasm.WasmBackendErrors.WASM_BACKEND_MISSING_CUSTOM_FORMATTERS
import org.jetbrains.kotlin.backend.wasm.export.ExportModelGenerator
import org.jetbrains.kotlin.backend.wasm.ic.overrideBuiltInsSignatures
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmServiceImportExportKind
import org.jetbrains.kotlin.backend.wasm.lower.JsInteropFunctionsLowering
import org.jetbrains.kotlin.backend.wasm.lower.markFunctionToExport
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportModelToTsDeclarations
import org.jetbrains.kotlin.ir.backend.js.tsexport.TypeScriptFragment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.config.generateDts
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.wasmTestBoxFunctionToExport
import org.jetbrains.kotlin.wasm.ir.WasmBinaryData
import org.jetbrains.kotlin.wasm.ir.WasmBinaryData.Companion.writeTo
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.WasmModule
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

data class DynamicJsModule(
    val name: String,
    val content: String,
)

class WasmCompilerResult(
    /**
     * The resulting Wasm module. This is not `null` only when running compiler tests, so that we could inspect it.
     * In regular use, this is always `null` for memory optimization purposes.
     */
    val linkedModule: WasmModule?,
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
    val typeScriptFragment: TypeScriptFragment?,
    val moduleDependencies: (IrModuleFragment) -> Set<IrModuleFragment>,
)

fun compileToLoweredIr(
    configuration: CompilerConfiguration,
    irLinker: KotlinIrLinker,
    allModules: List<IrModuleFragment>,
    context: WasmBackendContext,
): LoweredIrWithExtraArtifacts {
    // Ad-hoc export for box functions in compiler tests.
    configuration.wasmTestBoxFunctionToExport?.let { testBoxFunToExport ->
        val boxPackage = testBoxFunToExport.parent()
        val boxName = testBoxFunToExport.shortName()
        for (module in allModules) {
            for (file in module.files) {
                if (file.packageFqName != boxPackage) continue
                markFunctionToExport(context, file) {
                    // The majority of tests use `fun box(): String` as entry point.
                    // But some parts of stepping tests may have `box` fun returning `Unit`.
                    name == boxName && parameters.isEmpty() && (returnType.isString() || returnType.isUnit())
                }
            }
        }
    }

    val typeScriptFragment = runIf(configuration.generateDts) {
        val exportModel = ExportModelGenerator(context).generateExport(allModules)
        val exportModelToDtsTranslator = ExportModelToTsDeclarations(ModuleKind.ES)
        val fragment = exportModelToDtsTranslator.generateTypeScriptFragment(exportModel.declarations)
        TypeScriptFragment(exportModelToDtsTranslator.generateTypeScript("", listOf(fragment)))
    }

    lowerPreservingTags(
        allModules,
        context,
        context.irFactory.stageController as WholeWorldStageController,
    )

    overrideBuiltInsSignatures(context)

    val dependencyTracker = irLinker.moduleDependencyTracker as IrModuleDependencyTrackerImpl
    val stdlibFragment = allModules.first { it.kotlinLibrary?.isWasmStdlib == true }
    allModules.forEach { module ->
        dependencyTracker.trackDependency(module, stdlibFragment)
    }

    return LoweredIrWithExtraArtifacts(
        loweredIr = allModules,
        backendContext = context,
        typeScriptFragment = typeScriptFragment,
        moduleDependencies = dependencyTracker::getAllDependencies
    )
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

const val wasmInitializeExportName = "_initialize"
const val wasmStartExportName = "_start"
const val jsBuiltinsModulePrefix = "wasm:"
const val importedStringConstants = "'"

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

fun writeCompilationResult(
    result: WasmCompilerResult,
    dir: File,
    fileNameBase: String,
    configuration: CompilerConfiguration? = null
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
            configuration?.report(WASM_BACKEND_MISSING_CUSTOM_FORMATTERS, message)
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
            name == "memory" ||
            WasmServiceImportExportKind.entries.any { name.startsWith(it.prefix) }

fun generateExports(
    exports: List<WasmExport<*>>,
    wholeProgramMode: Boolean,
    isStdlibModule: Boolean,
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
const exports = wasmInstance.exports;
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
