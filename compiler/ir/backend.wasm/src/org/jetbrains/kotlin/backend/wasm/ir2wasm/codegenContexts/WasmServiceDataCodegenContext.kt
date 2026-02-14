/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.WasmSymbol
import org.jetbrains.kotlin.wasm.ir.WasmType

open class WasmServiceDataCodegenContext(
    private val wasmFileFragment: WasmCompiledServiceFileFragment,
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
        wasmFileFragment.mainFunctionWrappers.add(mainFunctionWrapper.getReferenceKey())
    }

    open fun addTestFunDeclarator(testFunctionDeclarator: IrFunctionSymbol) {
        wasmFileFragment.testFunctionDeclarators.add(testFunctionDeclarator.getReferenceKey())
    }

    open fun addEquivalentFunction(key: String, function: IrFunctionSymbol) {
        wasmFileFragment.equivalentFunctions.add(key to function.getReferenceKey())
    }

    open fun addClassAssociatedObjects(klass: IrClassSymbol, associatedObjectsGetters: List<AssociatedObjectBySymbols>) {
        val classAssociatedObjects = ClassAssociatedObjects(
            referenceTypeId(klass),
            associatedObjectsGetters.map { (obj, getter, isExternal) ->
                AssociatedObject(referenceTypeId(obj), getter.getReferenceKey(), isExternal)
            }
        )
        wasmFileFragment.classAssociatedObjectsInstanceGetters.add(classAssociatedObjects)
    }

    open fun addJsModuleAndQualifierReferences(reference: JsModuleAndQualifierReference) {
        wasmFileFragment.jsModuleAndQualifierReferences.add(reference)
    }
}