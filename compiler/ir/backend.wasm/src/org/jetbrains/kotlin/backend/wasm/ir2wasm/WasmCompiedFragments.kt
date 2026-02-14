/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment.JsCodeSnippet
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragment
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.WasmFunction
import org.jetbrains.kotlin.wasm.ir.WasmFunctionType
import org.jetbrains.kotlin.wasm.ir.WasmGlobal
import org.jetbrains.kotlin.wasm.ir.WasmStructDeclaration
import org.jetbrains.kotlin.wasm.ir.WasmSymbol
import org.jetbrains.kotlin.wasm.ir.WasmType
import org.jetbrains.kotlin.wasm.ir.WasmTypeDeclaration

class WasmCompiledTypesFileFragment(
    val definedGcTypes: MutableMap<IdSignature, WasmTypeDeclaration> = mutableMapOf(),
    val definedVTableGcTypes: MutableMap<IdSignature, WasmStructDeclaration> = mutableMapOf(),
    val definedFunctionTypes: MutableMap<IdSignature, WasmFunctionType> = mutableMapOf(),
)

val WasmCompiledDeclarationsFileFragment.hasDeclarations: Boolean
    get() = definedFunctions.isNotEmpty() || definedGlobalVTables.isNotEmpty() || definedGlobalClassITables.isNotEmpty() || definedRttiGlobal.isNotEmpty()

class WasmCompiledDeclarationsFileFragment(
    val definedFunctions: MutableMap<IdSignature, WasmFunction> = mutableMapOf(),
    val definedGlobalFields: MutableMap<IdSignature, WasmGlobal> = mutableMapOf(),
    val definedGlobalVTables: MutableMap<IdSignature, WasmGlobal> = mutableMapOf(),
    val definedGlobalClassITables: MutableMap<IdSignature, WasmGlobal> = mutableMapOf(),
    val definedRttiGlobal: MutableMap<IdSignature, WasmGlobal> = mutableMapOf(),
    val definedRttiSuperType: MutableMap<IdSignature, IdSignature?> = mutableMapOf(),
//    var builtinIdSignatures: BuiltinIdSignatures? = null,
)

class WasmCompiledServiceFileFragment(
    val globalLiterals: MutableSet<LiteralGlobalSymbol> = mutableSetOf(),
    val globalLiteralsId: MutableMap<String, WasmSymbol<Int>> = mutableMapOf(),
    val stringLiteralId: MutableMap<String, WasmSymbol<Int>> = mutableMapOf(),
    val constantArrayDataSegmentId: MutableMap<Pair<List<Long>, WasmType>, WasmSymbol<Int>> = mutableMapOf(),
    val jsFuns: MutableMap<IdSignature, JsCodeSnippet> = mutableMapOf(),
    val jsModuleImports: MutableMap<IdSignature, String> = mutableMapOf(),
    val jsBuiltinsPolyfills: MutableMap<String, String> = mutableMapOf(),
    val exports: MutableList<WasmExport<*>> = mutableListOf(),
    val mainFunctionWrappers: MutableList<IdSignature> = mutableListOf(),
    var testFunctionDeclarators: MutableList<IdSignature> = mutableListOf(),
    val equivalentFunctions: MutableList<Pair<String, IdSignature>> = mutableListOf(),
    val jsModuleAndQualifierReferences: MutableSet<JsModuleAndQualifierReference> = mutableSetOf(),
    val classAssociatedObjectsInstanceGetters: MutableList<ClassAssociatedObjects> = mutableListOf(),
    val objectInstanceFieldInitializers: MutableList<IdSignature> = mutableListOf(),
    val nonConstantFieldInitializers: MutableList<IdSignature> = mutableListOf(),
)

abstract class WasmCompiledFileFragment(
    val definedTypes: WasmCompiledTypesFileFragment,
    val definedDeclarations: WasmCompiledDeclarationsFileFragment,
) : IrICProgramFragment()

class WasmCompiledDependencyFileFragment(
    definedTypes: WasmCompiledTypesFileFragment = WasmCompiledTypesFileFragment(),
    definedDeclarations: WasmCompiledDeclarationsFileFragment = WasmCompiledDeclarationsFileFragment(),
) : WasmCompiledFileFragment(definedTypes, definedDeclarations) {

    fun makeProjection(onTypes: ModuleReferencedTypes?, onDeclarations: ModuleReferencedDeclarations): WasmCompiledDependencyFileFragment {
        val types = onTypes?.let { oldTypes ->
            WasmCompiledTypesFileFragment().also { newTypes ->
                definedTypes.definedGcTypes.filterTo(newTypes.definedGcTypes) { it.key in oldTypes.gcTypes }.toMutableMap()
                definedTypes.definedVTableGcTypes.filterTo(newTypes.definedVTableGcTypes) { it.key in oldTypes.gcTypes }.toMutableMap()
                definedTypes.definedFunctionTypes.filterTo(newTypes.definedFunctionTypes) { it.key in oldTypes.functionTypes }.toMutableMap()
            }
        } ?: definedTypes

        val declarations = WasmCompiledDeclarationsFileFragment()
        definedDeclarations.definedFunctions.filterTo(declarations.definedFunctions) { it.key in onDeclarations.functions }
        definedDeclarations.definedGlobalVTables.filterTo(declarations.definedGlobalVTables) { it.key in onDeclarations.globalVTable }
        definedDeclarations.definedGlobalClassITables.filterTo(declarations.definedGlobalClassITables) { it.key in onDeclarations.globalClassITable }
        definedDeclarations.definedRttiGlobal.filterTo(declarations.definedRttiGlobal) { it.key in onDeclarations.rttiGlobal }
        return WasmCompiledDependencyFileFragment(types, declarations)
    }
}

class WasmCompiledCodeFileFragment(
    definedTypes: WasmCompiledTypesFileFragment = WasmCompiledTypesFileFragment(),
    definedDeclarations: WasmCompiledDeclarationsFileFragment = WasmCompiledDeclarationsFileFragment(),
    val serviceData: WasmCompiledServiceFileFragment = WasmCompiledServiceFileFragment(),
) : WasmCompiledFileFragment(definedTypes, definedDeclarations)
