/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.DeclarationResolver
import org.jetbrains.kotlin.wasm.ir.WasmFunction
import org.jetbrains.kotlin.wasm.ir.WasmFunctionType
import org.jetbrains.kotlin.wasm.ir.WasmGlobal
import org.jetbrains.kotlin.wasm.ir.WasmHeapType.Type
import org.jetbrains.kotlin.wasm.ir.WasmImmediate
import org.jetbrains.kotlin.wasm.ir.WasmStructDeclaration
import org.jetbrains.kotlin.wasm.ir.WasmTypeDeclaration

internal class DefinedDeclarationsResolver(
    val functions: MutableMap<IdSignature, WasmFunction> = mutableMapOf(),
    val globalFields: MutableMap<IdSignature, WasmGlobal> = mutableMapOf(),
    val globalVTables: MutableMap<IdSignature, WasmGlobal> = mutableMapOf(),
    val globalClassITables: MutableMap<IdSignature, WasmGlobal> = mutableMapOf(),
    val globalRTTI: MutableMap<IdSignature, WasmGlobal> = mutableMapOf(),
    val gcTypes: MutableMap<IdSignature, WasmTypeDeclaration> = mutableMapOf(),
    val vTableGcTypes: MutableMap<IdSignature, WasmStructDeclaration> = mutableMapOf(),
    val functionTypes: MutableMap<IdSignature, WasmFunctionType> = mutableMapOf(),
) : DeclarationResolver() {

    val globalLiteralGlobals: MutableMap<String, WasmGlobal> = mutableMapOf()

    override fun resolve(type: Type): WasmTypeDeclaration = when (type) {
        is GcHeapTypeSymbol -> gcTypes.getValue(type.type)
        is VTableHeapTypeSymbol -> vTableGcTypes.getValue(type.type)
        is FunctionHeapTypeSymbol -> functionTypes.getValue(type.type)
        else -> error("Unsupported Type type: ${type::class.simpleName}")
    }

    override fun resolve(type: WasmImmediate.TypeIdx): WasmTypeDeclaration = when (type) {
        is GcTypeSymbol -> gcTypes.getValue(type.value)
        is VTableTypeSymbol -> vTableGcTypes.getValue(type.value)
        is FunctionTypeSymbol -> functionTypes.getValue(type.value)
        else -> error("Unsupported TypeSymbol type: ${type::class.simpleName}")
    }

    override fun resolve(global: WasmImmediate.GlobalIdx): WasmGlobal = when (global) {
        is FieldGlobalSymbol -> globalFields.getValue(global.value)
        is VTableGlobalSymbol -> globalVTables.getValue(global.value)
        is ClassITableGlobalSymbol -> globalClassITables.getValue(global.value)
        is RttiGlobalSymbol -> globalRTTI.getValue(global.value)
        is LiteralGlobalSymbol -> globalLiteralGlobals.getValue(global.value)
        else -> error("Unsupported GlobalSymbol type: ${global::class.simpleName}")
    }

    override fun resolve(function: WasmImmediate.FuncIdx): WasmFunction = when(function) {
        is FuncSymbol -> functions.getValue(function.value)
        else -> error("Unsupported FuncSymbolBase type: ${function::class.simpleName}")
    }
}