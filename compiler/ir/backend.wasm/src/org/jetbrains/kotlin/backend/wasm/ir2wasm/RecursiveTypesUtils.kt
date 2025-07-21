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

private fun WasmType.toTypeDeclaration(): WasmTypeDeclaration? {
    val heapType = when (val type = this) {
        is WasmRefType -> type.heapType
        is WasmRefNullType -> type.heapType
        else -> null
    }
    return (heapType as? WasmHeapType.Type)?.type?.owner
}

private fun dependencyTypes(type: WasmTypeDeclaration): Sequence<WasmTypeDeclaration> = sequence {
    when (type) {
        is WasmStructDeclaration -> {
            yieldIfNotNull(type.superType?.owner)
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

private fun wasmTypeDeclarationOrderKey(declaration: WasmTypeDeclaration): Int {
    return when (declaration) {
        is WasmArrayDeclaration -> 0
        is WasmFunctionType -> 0
        is WasmStructDeclaration ->
            // Subtype depth
            declaration.superType?.let { wasmTypeDeclarationOrderKey(it.owner) + 1 } ?: 0
    }
}


fun createRecursiveTypeGroups(types: Sequence<WasmTypeDeclaration>): MutableList<RecursiveTypeGroup> {
    val componentFinder = StronglyConnectedComponents(::dependencyTypes)
    types.forEach(componentFinder::visit)
    return componentFinder.findComponents()
}

private fun typeFingerprint(type: WasmType, currentHash: Hash128Bits, visited: MutableSet<WasmTypeDeclaration>): Hash128Bits {
    val heapType = when (type) {
        is WasmRefType -> type.getHeapType()
        is WasmRefNullType -> type.getHeapType()
        else -> return currentHash.combineWith(Hash128Bits(type.code.toULong()))
    }
    return when (heapType) {
        is WasmHeapType.Type -> wasmDeclarationFingerprint(heapType.type.owner, currentHash, visited)
        is WasmHeapType.Simple -> currentHash.combineWith(Hash128Bits(heapType.code.toULong()))
    }
}

private val structHash = Hash128Bits(1U, 1U)
private val functionHash = Hash128Bits(2U, 2U)
private val arrayHash = Hash128Bits(3U, 3U)

private fun wasmDeclarationFingerprint(
    declaration: WasmTypeDeclaration,
    currentHash: Hash128Bits,
    visited: MutableSet<WasmTypeDeclaration>
): Hash128Bits {
    if (!visited.add(declaration)) return currentHash
    return when (declaration) {
        is WasmStructDeclaration -> {
            val structHash = currentHash.combineWith(structHash)
            val fields = declaration.fields.fold(structHash) { acc, field ->
                typeFingerprint(field.type, acc, visited)
            }
            declaration.superType?.owner?.let {
                wasmDeclarationFingerprint(it, fields, visited)
            } ?: fields
        }
        is WasmFunctionType -> {
            val functionHash = currentHash.combineWith(functionHash)
            val parametersHash = declaration.parameterTypes.fold(functionHash) { acc, parameter ->
                typeFingerprint(parameter, acc, visited)
            }
            declaration.resultTypes.fold(parametersHash) { acc, parameter ->
                typeFingerprint(parameter, acc, visited)
            }
        }
        is WasmArrayDeclaration -> {
            val arrayHash = currentHash.combineWith(arrayHash)
            typeFingerprint(declaration.field.type, arrayHash, visited)
        }
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
    WasmStructRef,
)

internal fun encodeIndex(index: UInt): List<WasmStructFieldDeclaration> {
    var current = index
    val result = mutableListOf<WasmStructFieldDeclaration>()
    //i31 type is not used by kotlin/wasm, so mixin index would never clash with regular signature
    result.add(WasmStructFieldDeclaration("", WasmI31Ref, false))
    while (current != 0U) {
        result.add(WasmStructFieldDeclaration("", indexes[(current % 10U).toInt()], false))
        current /= 10U
    }
    return result
}

internal fun canonicalSort(group: RecursiveTypeGroup, stableSort: Boolean) {
    if (group.size == 1) return
    if (stableSort) {
        group.sortWith(WasmTypeDeclaratorByFingerprint())
    }
    group.sortBy(::wasmTypeDeclarationOrderKey)
}

private class WasmTypeDeclaratorByFingerprint : Comparator<WasmTypeDeclaration> {
    private val fingerprintCache = mutableMapOf<WasmTypeDeclaration, Hash128Bits>()

    private fun getFingerprint(type: WasmTypeDeclaration) = fingerprintCache.getOrPut(type) {
        wasmDeclarationFingerprint(type, Hash128Bits(), visited = mutableSetOf())
    }

    private fun diff(x: ULong, y: ULong): Int {
        if (x == y) return 0
        return if (x > y) {
            val diff = x - y
            if (diff > Int.MAX_VALUE.toUInt()) {
                Int.MAX_VALUE
            } else {
                (x - y).toInt()
            }
        } else {
            val diff = y - x
            if (diff > Int.MAX_VALUE.toUInt()) {
                Int.MIN_VALUE
            } else {
                -diff.toInt()
            }
        }
    }

    override fun compare(
        o1: WasmTypeDeclaration,
        o2: WasmTypeDeclaration,
    ): Int {
        val o1Hash = getFingerprint(o1)
        val o2Hash = getFingerprint(o2)
        return if (o1Hash.highBytes == o2Hash.highBytes) {
            diff(o1Hash.lowBytes, o2Hash.lowBytes)
        } else {
            diff(o1Hash.highBytes, o2Hash.highBytes)
        }
    }
}