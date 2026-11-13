/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.backend.wasm.utils.redefinitionError
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.wasm.ir.WasmFunctionType
import org.jetbrains.kotlin.wasm.ir.WasmStructDeclaration
import org.jetbrains.kotlin.wasm.ir.WasmTypeDeclaration

private const val ENCODE_BYTE_COUNT = 9
private const val MASK_7 = 0x7FUL

/**
 * Encodes the low 63 bits of [value] into 9 UTF-8-safe bytes.
 * Each output byte is in range 0..127, so the result is valid UTF-8.
 */
private fun encode63BitsToUtf8String(value: ULong): String {
    var current = value
    val result = ByteArray(ENCODE_BYTE_COUNT) {
        val codePoint = (current and MASK_7).toByte()
        current = current shr 7
        codePoint
    }
    return String(result, Charsets.UTF_8)
}

open class WasmTypeCodegenContext(
    private val wasmFileFragment: WasmCompiledTypesFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
) : WasmCodegenContext(idSignatureRetriever) {

    fun getDeclarationTag(declaration: IrDeclaration): String {
        val referenceString = declaration.symbol.getReferenceKey().toString()
        return encode63BitsToUtf8String(cityHash64(referenceString.toByteArray()))
    }

    fun defineGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        if (wasmFileFragment.definedGcTypes.put(irClass.getReferenceKey(), wasmType) != null) {
            redefinitionError(irClass.getReferenceKey(), "GcTypes")
        }
    }

    /** Cells: each class gets a *payload* struct holding its fields
     * (incl. Any's `_hashCode`); the cell struct — vtable, itable, rtti,
     * payload — never changes shape. Versioned payload keys live in the
     * same definedGcTypes map under synthetic signatures (same trick as
     * the slot globals); v1 keeps this unversioned key. */
    fun payloadSignature(irClass: IrClassSymbol): IdSignature =
        IdSignature.CommonSignature("hot.reload.payload", "payload<\$" + irClass.getReferenceKey().toString() + ">", null, 0, null)

    fun classReferenceKey(irClass: IrClassSymbol): String = irClass.getReferenceKey().toString()

    fun stepperSignature(irClass: IrClassSymbol, version: Int): IdSignature =
        HotReloadPayloads.stepperSignature(classReferenceKey(irClass), version)

    fun definePayloadVersionGcType(irClass: IrClassSymbol, version: Int, wasmType: WasmTypeDeclaration) {
        val key = HotReloadPayloads.payloadVersionSignature(classReferenceKey(irClass), version)
        if (wasmFileFragment.definedGcTypes.put(key, wasmType) != null) {
            redefinitionError(key, "PayloadGcTypes")
        }
    }

    open fun referencePayloadVersionGcType(irClass: IrClassSymbol, version: Int): GcTypeSymbol =
        GcTypeSymbol(HotReloadPayloads.payloadVersionSignature(classReferenceKey(irClass), version))

    open fun referencePayloadVersionHeapType(irClass: IrClassSymbol, version: Int): GcHeapTypeSymbol =
        GcHeapTypeSymbol(HotReloadPayloads.payloadVersionSignature(classReferenceKey(irClass), version))

    open fun referencePayloadHeapType(irClass: IrClassSymbol): GcHeapTypeSymbol = GcHeapTypeSymbol(payloadSignature(irClass))

    fun defineVTableGcType(irClass: IrClassSymbol, wasmType: WasmStructDeclaration) {
        if (wasmFileFragment.definedVTableGcTypes.put(irClass.getReferenceKey(), wasmType) != null) {
            redefinitionError(irClass.getReferenceKey(), "VTableGcTypes")
        }
    }

    fun defineFunctionType(irFunction: IrFunctionSymbol, wasmFunctionType: WasmFunctionType) {
        if (wasmFileFragment.definedFunctionTypes.put(irFunction.getReferenceKey(), wasmFunctionType) != null) {
            redefinitionError(irFunction.getReferenceKey(), "FunctionTypes")
        }
    }

    /** The registered wasm function type of [irFunction] (types are generated
     *  before function bodies), or null if not registered. Used by the
     *  hot-reload codegen to synthesize slot/entry functions. */
    fun getWasmFunctionType(irFunction: IrFunctionSymbol): WasmFunctionType? =
        wasmFileFragment.definedFunctionTypes[irFunction.getReferenceKey()]

    open fun referenceWasmFunctionType(wasmFunctionType: WasmFunctionType): FunctionTypeSymbol {
        val signature = getFunctionTypeSignature(wasmFunctionType)
        wasmFileFragment.definedFunctionTypes.putIfAbsent(signature, wasmFunctionType)
        return FunctionTypeSymbol(signature)
    }

    open fun referenceWasmFunctionHeapType(wasmFunctionType: WasmFunctionType): FunctionHeapTypeSymbol {
        val signature = getFunctionTypeSignature(wasmFunctionType)
        wasmFileFragment.definedFunctionTypes.putIfAbsent(signature, wasmFunctionType)
        return FunctionHeapTypeSymbol(signature)
    }

    open fun referenceGcType(irClass: IrClassSymbol): GcTypeSymbol =
        GcTypeSymbol(irClass.getReferenceKey())

    open fun referenceHeapType(irClass: IrClassSymbol): GcHeapTypeSymbol =
        GcHeapTypeSymbol(irClass.getReferenceKey())

    open fun referenceVTableGcType(irClass: IrClassSymbol): VTableTypeSymbol =
        VTableTypeSymbol(irClass.getReferenceKey())

    open fun referenceVTableHeapType(irClass: IrClassSymbol): VTableHeapTypeSymbol =
        VTableHeapTypeSymbol(irClass.getReferenceKey())

    open fun referenceFunctionType(irClass: IrFunctionSymbol): FunctionTypeSymbol =
        FunctionTypeSymbol(irClass.getReferenceKey())

    open fun referenceFunctionHeapType(irClass: IrFunctionSymbol): FunctionHeapTypeSymbol =
        FunctionHeapTypeSymbol(irClass.getReferenceKey())
}
