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
)

class WasmCompiledLinkerDataFileFragment(
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
) : WasmCompiledFileFragment(definedTypes, definedDeclarations)

class WasmCompiledCodeFileFragment(
    definedTypes: WasmCompiledTypesFileFragment = WasmCompiledTypesFileFragment(),
    definedDeclarations: WasmCompiledDeclarationsFileFragment = WasmCompiledDeclarationsFileFragment(),
    val linkerData: WasmCompiledLinkerDataFileFragment = WasmCompiledLinkerDataFileFragment(),
) : WasmCompiledFileFragment(definedTypes, definedDeclarations)

fun WasmCompiledTypesFileFragment.makeProjection(onTypes: ModuleReferencedTypes): WasmCompiledTypesFileFragment =
    WasmCompiledTypesFileFragment().also { newFragment ->
        definedGcTypes.filterTo(newFragment.definedGcTypes) { it.key in onTypes.gcTypes }
        definedVTableGcTypes.filterTo(newFragment.definedVTableGcTypes) { it.key in onTypes.gcTypes }
        definedFunctionTypes.filterTo(newFragment.definedFunctionTypes) { it.key in onTypes.functionTypes }
    }

fun WasmCompiledDeclarationsFileFragment.makeProjection(onDeclarations: ModuleReferencedDeclarations): WasmCompiledDeclarationsFileFragment =
    WasmCompiledDeclarationsFileFragment().also { newFragment ->
        definedFunctions.filterTo(newFragment.definedFunctions) { it.key in onDeclarations.functions }
        definedGlobalVTables.filterTo(newFragment.definedGlobalVTables) { it.key in onDeclarations.globalVTable }
        definedGlobalClassITables.filterTo(newFragment.definedGlobalClassITables) { it.key in onDeclarations.globalClassITable }
        definedRttiGlobal.filterTo(newFragment.definedRttiGlobal) { it.key in onDeclarations.rttiGlobal }
    }

