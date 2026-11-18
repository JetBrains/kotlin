/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.WasmSymbol
import org.jetbrains.kotlin.wasm.ir.WasmType

open class WasmLinkerDataCodegenContext(
    private val wasmFileFragment: WasmCompiledLinkerDataFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
) : WasmCodegenContext(idSignatureRetriever) {
    fun referenceConstantArray(resource: Pair<List<Long>, WasmType>): WasmSymbol<Int> =
        wasmFileFragment.constantArrayDataSegmentId.getOrPut(resource) { WasmSymbol() }

    fun addExport(wasmExport: WasmExport<*>) {
        wasmFileFragment.exports += wasmExport
    }

    fun referenceGlobalStringGlobal(value: String): LiteralGlobalSymbol {
        return LiteralGlobalSymbol(value).also {
            wasmFileFragment.globalLiterals.add(it)
        }
    }

    fun referenceGlobalStringId(referenceValue: String): WasmSymbol<Int> =
        wasmFileFragment.globalLiteralsId.getOrPut(referenceValue) { WasmSymbol() }

    fun referenceStringLiteralId(string: String): WasmSymbol<Int> =
        wasmFileFragment.stringLiteralId.getOrPut(string) { WasmSymbol() }

    fun referenceTypeId(irClass: IrClassSymbol): Long =
        cityHash64(irClass.getReferenceKey().toString().encodeToByteArray()).toLong()

    fun addUsedAsWasmRawFunctionReference(irFunction: IrFunctionSymbol) {
        wasmFileFragment.wasmReferencedFunctions.add(irFunction.getReferenceKey())
    }

    /** Hot-reload support: declare a synthesized function (registered under a
     *  synthetic key) as used via ref.func, so the linker emits the required
     *  declarative element segment. */
    fun addUsedAsWasmRawFunctionReference(key: IdSignature) {
        wasmFileFragment.wasmReferencedFunctions.add(key)
    }

    /** The fdefn era: record this file's binding for the link step —
     *  the exact name (the image table's key), the impl signature (the
     *  init installs its ref.func into the cell), and the cell global's
     *  synthetic key (the init resolves the binding by name at load). */
    fun addHotReloadCellBinding(name: String, impl: IdSignature, cellKey: IdSignature) {
        wasmFileFragment.hotReloadCellBindings.add(Triple(name, impl, cellKey))
    }

    fun addJsFun(irFunction: IrFunctionSymbol, importName: WasmSymbol<String>, jsCode: String) {
        wasmFileFragment.jsFuns[irFunction.getReferenceKey()] =
            WasmCompiledModuleFragment.JsCodeSnippet(importName = importName, jsCode = jsCode)
    }

    fun addJsModuleImport(irFunction: IrFunctionSymbol, module: String) {
        wasmFileFragment.jsModuleImports[irFunction.getReferenceKey()] = module
    }

    fun addJsBuiltin(declarationName: String, polyfillImpl: String) {
        wasmFileFragment.jsBuiltinsPolyfills[declarationName] = polyfillImpl
    }

    open fun addObjectInstanceFieldInitializer(initializer: IrFunctionSymbol) {
        wasmFileFragment.objectInstanceFieldInitializers.add(initializer.getReferenceKey())
    }

    open fun addNonConstantFieldInitializers(initializer: IrFunctionSymbol) {
        wasmFileFragment.nonConstantFieldInitializers.add(initializer.getReferenceKey())
    }

    open fun addMainFunctionWrapper(mainFunctionWrapper: IrFunctionSymbol) {
        val fqName = mainFunctionWrapper.owner.getPackageFragment().packageFqName.asString()
        val reference = mainFunctionWrapper.getReferenceKey()
        wasmFileFragment.mainFunctionWrappers.add(MainFunctionWrapper(fqName, reference))
    }

    open fun addTestFunDeclarator(testFunctionDeclarator: IrFunctionSymbol) {
        wasmFileFragment.testFunctionDeclarators.add(testFunctionDeclarator.getReferenceKey())
    }

    open fun addEquivalentFunction(key: String, function: IrFunctionSymbol) {
        wasmFileFragment.equivalentFunctions.add(key to function.getReferenceKey())
    }

    open fun addEquivalentType(key: String, klass: IrClassSymbol) {
        wasmFileFragment.equivalentTypes.add(key to klass.getReferenceKey())
    }

    open fun addClassAssociatedObjects(klass: IrClassSymbol, associatedObjectsGetters: List<AssociatedObjectBySymbols>) {
        val classAssociatedObjects = ClassAssociatedObjects(
            referenceTypeId(klass),
            associatedObjectsGetters.map { (val obj = klass, val getter, val isExternal) ->
                AssociatedObject(referenceTypeId(obj), getter.getReferenceKey(), isExternal)
            }
        )
        wasmFileFragment.classAssociatedObjectsInstanceGetters.add(classAssociatedObjects)
    }

    open fun addJsModuleAndQualifierReferences(reference: JsModuleAndQualifierReference) {
        wasmFileFragment.jsModuleAndQualifierReferences.add(reference)
    }

    /** Hot reload: record the class's registration into the image's
     *  CLASS REGISTRY — the class's reference key rides along (the
     *  init reads the class's vtable/itable globals by it). */
    fun addHotLayoutBinding(irClass: IrClassSymbol, binding: HotLayoutBinding) {
        binding.classKey = irClass.getReferenceKey()
        wasmFileFragment.hotLayoutBindings.add(binding)
    }
}
