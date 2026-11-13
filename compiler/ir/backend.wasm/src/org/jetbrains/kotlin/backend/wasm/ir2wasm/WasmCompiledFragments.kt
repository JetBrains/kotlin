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

data class MainFunctionWrapper(val fqName: String, val function: IdSignature)

class WasmCompiledLinkerDataFileFragment(
    val globalLiterals: MutableSet<LiteralGlobalSymbol> = mutableSetOf(),
    val globalLiteralsId: MutableMap<String, WasmSymbol<Int>> = mutableMapOf(),
    val stringLiteralId: MutableMap<String, WasmSymbol<Int>> = mutableMapOf(),
    val constantArrayDataSegmentId: MutableMap<Pair<List<Long>, WasmType>, WasmSymbol<Int>> = mutableMapOf(),
    val jsFuns: MutableMap<IdSignature, JsCodeSnippet> = mutableMapOf(),
    val jsModuleImports: MutableMap<IdSignature, String> = mutableMapOf(),
    val jsBuiltinsPolyfills: MutableMap<String, String> = mutableMapOf(),
    val exports: MutableList<WasmExport<*>> = mutableListOf(),
    val mainFunctionWrappers: MutableList<MainFunctionWrapper> = mutableListOf(),
    var testFunctionDeclarators: MutableList<IdSignature> = mutableListOf(),
    val equivalentFunctions: MutableList<Pair<String, IdSignature>> = mutableListOf(),
    val equivalentTypes: MutableList<Pair<String, IdSignature>> = mutableListOf(),
    val jsModuleAndQualifierReferences: MutableSet<JsModuleAndQualifierReference> = mutableSetOf(),
    val classAssociatedObjectsInstanceGetters: MutableList<ClassAssociatedObjects> = mutableListOf(),
    // hot reload: reference keys of the classes this file declares (the
    // linker generates one universal vtable stepper per module from them)
    val hotReloadableClasses: MutableList<IdSignature> = mutableListOf(),
    val objectInstanceFieldInitializers: MutableList<IdSignature> = mutableListOf(),
    val nonConstantFieldInitializers: MutableList<IdSignature> = mutableListOf(),
    val wasmReferencedFunctions: MutableSet<IdSignature> = mutableSetOf(),
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

inline fun <T> List<T>.collectTypeReferences(selector: (T) -> ModuleReferencedTypes): ModuleReferencedTypes {
    val currentDeclarationTypes = ModuleReferencedTypes()
    forEach { fragment ->
        val fragmentTypes = selector(fragment)
        currentDeclarationTypes.gcTypes.addAll(fragmentTypes.gcTypes)
        currentDeclarationTypes.functionTypes.addAll(fragmentTypes.functionTypes)
    }
    return currentDeclarationTypes
}

inline fun <T> List<T>.collectDeclarationReferences(selector: (T) -> ModuleReferencedDeclarations): ModuleReferencedDeclarations {
    val currentDeclarationReferences = ModuleReferencedDeclarations()
    forEach { fragment ->
        val fragmentReferences = selector(fragment)
        currentDeclarationReferences.functions.addAll(fragmentReferences.functions)
        currentDeclarationReferences.globalVTable.addAll(fragmentReferences.globalVTable)
        currentDeclarationReferences.globalClassITable.addAll(fragmentReferences.globalClassITable)
        currentDeclarationReferences.rttiGlobal.addAll(fragmentReferences.rttiGlobal)
    }
    return currentDeclarationReferences
}


fun WasmCompiledTypesFileFragment.makeProjection(onTypes: ModuleReferencedTypes): WasmCompiledTypesFileFragment =
    WasmCompiledTypesFileFragment().also { newFragment ->
        val keptGcTypes = LinkedHashMap<IdSignature, WasmTypeDeclaration>()
        definedGcTypes.filterTo(keptGcTypes) { it.key in onTypes.gcTypes }
        closeOverSuperTypes(keptGcTypes, definedGcTypes)
        newFragment.definedGcTypes.putAll(keptGcTypes)

        val keptVTableTypes = LinkedHashMap<IdSignature, WasmStructDeclaration>()
        definedVTableGcTypes.filterTo(keptVTableTypes) { it.key in onTypes.gcTypes }
        closeOverSuperTypes(keptVTableTypes, definedVTableGcTypes)
        newFragment.definedVTableGcTypes.putAll(keptVTableTypes)

        definedFunctionTypes.filterTo(newFragment.definedFunctionTypes) { it.key in onTypes.functionTypes }
    }

/**
 * Wasm requires a kept type's SUPERTYPE to be declared, and the
 * reference recording only sees direct references — close the
 * projection over supertype chains (payload structs subtype the
 * superclass's payload; a grown vtable subtypes the previous one).
 * LOCAL closure, within one fragment's own definitions — it protects
 * every projection path (including the non-IC link) for intra-file
 * chains; cross-FILE chains need the referenced-set closure in
 * compileArtifactMultimodule, which sees the whole dependency universe.
 */
private fun <T : WasmTypeDeclaration> closeOverSuperTypes(
    kept: LinkedHashMap<IdSignature, T>,
    definitions: Map<IdSignature, T>,
) {
    val pending = ArrayDeque(kept.values.toList())
    while (pending.isNotEmpty()) {
        val superTypeSymbol = (pending.removeLast() as? WasmStructDeclaration)?.superType as? GcHeapTypeSymbol ?: continue
        val superType = superTypeSymbol.type
        if (superType in kept) continue
        val superDeclaration = definitions[superType] ?: continue
        kept[superType] = superDeclaration
        pending.add(superDeclaration)
    }
}

fun WasmCompiledDeclarationsFileFragment.makeProjection(onDeclarations: ModuleReferencedDeclarations): WasmCompiledDeclarationsFileFragment =
    WasmCompiledDeclarationsFileFragment().also { newFragment ->
        definedFunctions.filterTo(newFragment.definedFunctions) { it.key in onDeclarations.functions }
        definedGlobalVTables.filterTo(newFragment.definedGlobalVTables) { it.key in onDeclarations.globalVTable }
        definedGlobalClassITables.filterTo(newFragment.definedGlobalClassITables) { it.key in onDeclarations.globalClassITable }
        definedRttiGlobal.filterTo(newFragment.definedRttiGlobal) { it.key in onDeclarations.rttiGlobal }
    }
