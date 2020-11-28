/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
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
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToBinary
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToText
import java.io.ByteArrayOutputStream

class WasmCompilerResult(val wat: String, val js: String, val wasm: ByteArray)

fun compileWasm(
    project: Project,
    mainModule: MainModule,
    analyzer: AbstractAnalyzerWithCompilerReport,
    configuration: CompilerConfiguration,
    phaseConfig: PhaseConfig,
    allDependencies: KotlinLibraryResolveResult,
    friendDependencies: List<KotlinLibrary>,
    exportedDeclarations: Set<FqName> = emptySet()
): WasmCompilerResult {
    val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
        loadIr(
            project, mainModule, analyzer, configuration, allDependencies, friendDependencies,
            PersistentIrFactory
        )

    val allModules = when (mainModule) {
        is MainModule.SourceFiles -> dependencyModules + listOf(moduleFragment)
        is MainModule.Klib -> dependencyModules
    }

    val moduleDescriptor = moduleFragment.descriptor
    val context = WasmBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, exportedDeclarations, configuration)

    // Load declarations referenced during `context` initialization
    allModules.forEach {
        val irProviders = generateTypicalIrProviderList(it.descriptor, irBuiltIns, symbolTable, deserializer)
        ExternalDependenciesGenerator(symbolTable, irProviders, configuration.languageVersionSettings)
            .generateUnboundSymbolsAsDependencies()
    }

    val irFiles = allModules.flatMap { it.files }

    moduleFragment.files.clear()
    moduleFragment.files += irFiles

    // Create stubs
    val irProviders = generateTypicalIrProviderList(moduleDescriptor, irBuiltIns, symbolTable, deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders, configuration.languageVersionSettings).generateUnboundSymbolsAsDependencies()
    moduleFragment.patchDeclarationParents()

    wasmPhases.invokeToplevel(phaseConfig, context, moduleFragment)

    val compiledWasmModule = WasmCompiledModuleFragment()
    val codeGenerator = WasmModuleFragmentGenerator(context, compiledWasmModule)
    codeGenerator.generateModule(moduleFragment)

    val linkedModule = compiledWasmModule.linkWasmCompiledFragments()
    val watGenerator = WasmIrToText()
    watGenerator.appendWasmModule(linkedModule)
    val wat = watGenerator.toString()

    val js = compiledWasmModule.generateJs()

    val os = ByteArrayOutputStream()
    WasmIrToBinary(os, linkedModule).appendWasmModule()
    val byteArray = os.toByteArray()

    return WasmCompilerResult(
        wat = wat,
        js = js,
        wasm = byteArray
    )
}


fun WasmCompiledModuleFragment.generateJs(): String {
    val runtime = """
    const runtime = {
        String_getChar(str, index) {
            return str.charCodeAt(index);
        },

        String_compareTo(str1, str2) {
            if (str1 > str2) return 1;
            if (str1 < str2) return -1;
            return 0;
        },

        String_equals(str, other) {
            return str === other;
        },

        String_subsequence(str, startIndex, endIndex) {
            return str.substring(startIndex, endIndex);
        },

        String_getLiteral(index) {
            return runtime.stringLiterals[index];
        },

        coerceToString(value) {
            return String(value);
        },

        Char_toString(char) {
            return String.fromCharCode(char)
        },

        JsArray_new(size) {
            return new Array(size);
        },

        JsArray_get(array, index) {
            return array[index];
        },

        JsArray_set(array, index, value) {
            array[index] = value;
        },

        JsArray_getSize(array) {
            return array.length;
        },

        identity(x) {
            return x;
        },

        println(value) {
            console.log(">>>  " + value)
        }
    };
    """.trimIndent()

    val jsCode =
        "\nconst js_code = {${jsFuns.joinToString(",\n") { "\"" + it.importName + "\" : " + it.jsCode }}};"

    return runtime + generateStringLiteralsSupport(stringLiterals) + jsCode
}
