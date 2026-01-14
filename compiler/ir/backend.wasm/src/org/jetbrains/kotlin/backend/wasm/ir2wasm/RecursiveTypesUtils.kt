/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.backend.wasm.utils.StronglyConnectedComponents
import org.jetbrains.kotlin.utils.yieldIfNotNull
import org.jetbrains.kotlin.wasm.ir.*

typealias RecursiveTypeGroup = MutableList<WasmTypeDeclaration>

internal class RecursiveGroupBuilder(private val resolver: (WasmHeapType.Type) -> WasmTypeDeclaration) {
    private val componentFinder = StronglyConnectedComponents(::dependencyTypes)

    private fun WasmType.toTypeDeclaration(): WasmTypeDeclaration? {
        val heapType = when (val type = this) {
            is WasmRefType -> type.heapType
            is WasmRefNullType -> type.heapType
            else -> null
        }
        return (heapType as? WasmHeapType.Type)?.let(resolver)
    }

    private fun dependencyTypes(type: WasmTypeDeclaration): Sequence<WasmTypeDeclaration> = sequence {
        when (type) {
            is WasmStructDeclaration -> {
                yieldIfNotNull(type.superType?.let(resolver))
                for (field in type.fields) {
                    yieldIfNotNull(field.type.toTypeDeclaration())
                }
            }
            is WasmArrayDeclaration -> {
                yieldIfNotNull(type.field.type.toTypeDeclaration())
            }
            is WasmFunctionType -> {
                for (parameter in type.parameterTypes) {
                    yieldIfNotNull(parameter.toTypeDeclaration())
                }
                for (parameter in type.resultTypes) {
                    yieldIfNotNull(parameter.toTypeDeclaration())
                }
            }
        }
    }

    fun addTypes(types: Iterable<WasmTypeDeclaration>) {
        types.forEach(componentFinder::visit)
    }

    fun build(): MutableList<RecursiveTypeGroup> {
        return componentFinder.findComponents()
    }
}

private val indexes = arrayOf(
    WasmI32,
    WasmI64,
    WasmF32,
    WasmF64,
    WasmV128,
    WasmI8,
    WasmI16,
    WasmFuncRef,
    WasmExternRef,
    WasmAnyRef,
    WasmEqRef,
    WasmRefNullrefType,
    WasmRefNullExternrefType,
    WasmI31Ref,
    WasmStructRef,
    WasmArrayRef,
)

internal fun encodeIndex(index: ULong): List<WasmStructFieldDeclaration> {
    var current = index
    val result = mutableListOf<WasmStructFieldDeclaration>()
    val indexesSize = indexes.size.toUInt()

    var wasI31 = false

    while (current != 0UL) {
        val fieldType = indexes[(current % indexesSize).toInt()]
        result.add(WasmStructFieldDeclaration("", fieldType, false))
        wasI31 = wasI31 || fieldType == WasmI31Ref

        current /= indexesSize
    }

    //i31 type is not used by kotlin/wasm, so mixin index would never clash with regular signature
    if (!wasI31) {
        result.add(WasmStructFieldDeclaration("", WasmI31Ref, false))
    }

    return result
}

private fun wasmTypeDeclarationOrderKey(
    declaration: WasmTypeDeclaration,
    resolver: (WasmHeapType.Type) -> WasmTypeDeclaration,
): Int {
    return when (declaration) {
        is WasmArrayDeclaration -> 0
        is WasmFunctionType -> 0
        is WasmStructDeclaration ->
            // Subtype depth
            declaration.superType?.let { wasmTypeDeclarationOrderKey(resolver(it), resolver) + 1 } ?: 0
    }
}

internal fun canonicalStableSort(
    group: RecursiveTypeGroup,
    resolver: (WasmHeapType.Type) -> WasmTypeDeclaration,
    getStableId: (WasmStructDeclaration) -> Hash128Bits,
) {
    if (group.size == 1) return

    group.sortWith(WasmTypeDeclaratorByFingerprint(resolver, getStableId))

    val sortMethod = fun(declaration: WasmTypeDeclaration): Int = wasmTypeDeclarationOrderKey(declaration, resolver)
    group.sortBy(sortMethod)
}

private class WasmTypeDeclaratorByFingerprint(
    private val resolver: (WasmHeapType.Type) -> WasmTypeDeclaration,
    private val getStableId: (WasmStructDeclaration) -> Hash128Bits,
) : Comparator<WasmTypeDeclaration> {

    companion object {
        private val k0 = 0xc3a5c85c97cb3127U
        private val k1 = 0xb492b66fbe98f273U
        private val k2 = 0x9ae16a3b2f90404fU
        private val k3 = 0xc949d7c7509e6557U

        private val structHash = Hash128Bits(k0, k1)
        private val function1Hash = Hash128Bits(k1, k2)
        private val function2Hash = Hash128Bits(k2, k3)
        private val arrayHash = Hash128Bits(k3, k0)
    }

    private fun combine(hash: Hash128Bits, type: WasmType): Hash128Bits {
        val heapType = when (type) {
            is WasmRefType -> type.getHeapType()
            is WasmRefNullType -> type.getHeapType()
            else -> return hash.combineWith(Hash128Bits(type.code.toULong()))
        }

        return when (heapType) {
            is WasmHeapType.Type -> combine(hash, resolver(heapType))
            is WasmHeapType.Simple -> hash.combineWith(Hash128Bits(heapType.code.toULong()))
        }
    }

    private fun combine(hash: Hash128Bits, declaration: WasmTypeDeclaration): Hash128Bits = when (declaration) {
        is WasmStructDeclaration ->
            hash.combineWith(structHash).combineWith(getStableId(declaration))
        is WasmFunctionType -> {
            val functionHash = hash.combineWith(function1Hash)
            val parametersHash = declaration.parameterTypes.fold(functionHash, ::combine)
            val returnValuesHash = parametersHash.combineWith(function2Hash)
            declaration.resultTypes.fold(returnValuesHash, ::combine)
        }
        is WasmArrayDeclaration -> {
            val arrayHash = hash.combineWith(arrayHash)
            combine(arrayHash, declaration.field.type)
        }
    }

    private fun getFingerprint(type: WasmTypeDeclaration) =
        combine(Hash128Bits(0UL, 0UL), type)

    override fun compare(
        o1: WasmTypeDeclaration,
        o2: WasmTypeDeclaration,
    ): Int {
        val o1Hash = getFingerprint(o1)
        val o2Hash = getFingerprint(o2)
        return if (o1Hash.highBytes == o2Hash.highBytes) {
            o1Hash.lowBytes.compareTo(o2Hash.lowBytes)
        } else {
            o1Hash.highBytes.compareTo(o2Hash.highBytes)
        }
    }
}