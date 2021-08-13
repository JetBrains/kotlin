/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.temporaryVals

import org.jetbrains.kotlin.codegen.inline.INLINE_MARKER_CLASS_NAME
import org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.isLoadOperation
import org.jetbrains.kotlin.codegen.optimization.common.isStoreOperation
import org.jetbrains.kotlin.codegen.optimization.fixStack.BasicTypeInterpreter
import org.jetbrains.kotlin.codegen.optimization.removeNodeGetNext
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Value


private const val TEMPORARY_VAL_INIT_MARKER = "<TEMPORARY-VAL-INIT>"

fun InstructionAdapter.addTemporaryValInitMarker() {
    invokestatic(INLINE_MARKER_CLASS_NAME, TEMPORARY_VAL_INIT_MARKER, "()V", false)
}

fun AbstractInsnNode.isTemporaryValInitMarker(): Boolean {
    if (opcode != Opcodes.INVOKESTATIC) return false
    val methodInsn = this as MethodInsnNode
    return methodInsn.owner == INLINE_MARKER_CLASS_NAME && methodInsn.name == TEMPORARY_VAL_INIT_MARKER
}

fun MethodNode.stripTemporaryValInitMarkers() {
    var insn = this.instructions.first
    while (insn != null) {
        insn = if (insn.isTemporaryValInitMarker()) {
            this.instructions.removeNodeGetNext(insn)
        } else {
            insn.next
        }
    }
}

// A temporary val is a local variables that is:
//  - initialized once (with some xSTORE instruction)
//  - is not written by any other instruction (xSTORE or IINC)
//  - not "observed" by any LVT entry
class TemporaryVal(
    val index: Int,
    val storeInsn: VarInsnNode,
    val loadInsns: List<VarInsnNode>
)

class TemporaryValsAnalyzer {

    fun analyze(internalClassName: String, methodNode: MethodNode): List<TemporaryVal> {
        val temporaryValStores = methodNode.instructions.filter { it.isStoreOperation() && it.previous.isTemporaryValInitMarker() }
        if (temporaryValStores.isEmpty()) return emptyList()

        val storeInsnToStoreData = temporaryValStores.associateWith { StoreData(it) }
        FastMethodAnalyzer(internalClassName, methodNode, StoreTrackingInterpreter(storeInsnToStoreData)).analyze()

        return storeInsnToStoreData.values
            .filterNot { it.isDirty }
            .map {
                val storeInsn = it.storeInsn as VarInsnNode
                val loadInsns = it.loads.map { load -> load as VarInsnNode }
                TemporaryVal(storeInsn.`var`, storeInsn, loadInsns)
            }
            .sortedBy { methodNode.instructions.indexOf(it.storeInsn) }
    }

    private class StoreData(val storeInsn: AbstractInsnNode) {
        var isDirty = false

        val storeOpcode = storeInsn.opcode

        val value =
            StoredValue.Store(
                this,
                when (storeOpcode) {
                    Opcodes.LSTORE, Opcodes.DSTORE -> 2
                    else -> 1
                }
            )

        val loads = LinkedHashSet<AbstractInsnNode>()
    }

    private sealed class StoredValue(private val _size: Int) : Value {
        override fun getSize(): Int = _size

        object Small : StoredValue(1)

        object Big : StoredValue(2)

        class Store(val temporaryVal: StoreData, size: Int) : StoredValue(size) {
            override fun equals(other: Any?): Boolean =
                other is Store && other.temporaryVal === temporaryVal

            override fun hashCode(): Int =
                temporaryVal.hashCode()
        }

        class DirtyStore(val temporaryVals: Collection<StoreData>, size: Int) : StoredValue(size) {
            override fun equals(other: Any?): Boolean =
                other is DirtyStore && other.temporaryVals == temporaryVals

            override fun hashCode(): Int =
                temporaryVals.hashCode()
        }
    }

    private class StoreTrackingInterpreter(
        private val storeInsnToStoreData: Map<AbstractInsnNode, StoreData>
    ) : BasicTypeInterpreter<StoredValue>() {

        override fun uninitializedValue(): StoredValue = StoredValue.Small
        override fun booleanValue(): StoredValue = StoredValue.Small
        override fun charValue(): StoredValue = StoredValue.Small
        override fun byteValue(): StoredValue = StoredValue.Small
        override fun shortValue(): StoredValue = StoredValue.Small
        override fun intValue(): StoredValue = StoredValue.Small
        override fun longValue(): StoredValue = StoredValue.Big
        override fun floatValue(): StoredValue = StoredValue.Small
        override fun doubleValue(): StoredValue = StoredValue.Big
        override fun nullValue(): StoredValue = StoredValue.Small
        override fun objectValue(type: Type): StoredValue = StoredValue.Small
        override fun arrayValue(type: Type): StoredValue = StoredValue.Small
        override fun methodValue(type: Type): StoredValue = StoredValue.Small
        override fun handleValue(handle: Handle): StoredValue = StoredValue.Small
        override fun typeConstValue(typeConst: Type): StoredValue = StoredValue.Small
        override fun aaLoadValue(arrayValue: StoredValue): StoredValue = StoredValue.Small

        override fun copyOperation(insn: AbstractInsnNode, value: StoredValue): StoredValue {
            if (insn.isStoreOperation()) {
                val temporaryValData = storeInsnToStoreData[insn]
                if (temporaryValData != null) {
                    return temporaryValData.value
                }
            } else if (insn.isLoadOperation()) {
                if (value is StoredValue.DirtyStore) {
                    // If we load a dirty value, invalidate all related temporary vals.
                    for (temporaryValData in value.temporaryVals) {
                        temporaryValData.isDirty = true
                    }
                } else if (value is StoredValue.Store) {
                    // Keep track of a load instruction
                    value.temporaryVal.loads.add(insn)
                }
            }
            return value
        }

        override fun merge(a: StoredValue, b: StoredValue): StoredValue {
            return when {
                a === b ->
                    a
                a is StoredValue.Store || a is StoredValue.DirtyStore || b is StoredValue.Store || b is StoredValue.DirtyStore -> {
                    // 'StoreValue.Store' are unique for each 'StoreData', so if we are here, we are going to merge value stored
                    // by a xSTORE instruction with some other value (maybe produced by another xSTORE instruction).
                    // Loading such value invalidates all related temporary vals.
                    val dirtySet = SmartSet.create(a.temporaryVals())
                    dirtySet.addAll(b.temporaryVals())
                    StoredValue.DirtyStore(dirtySet, a.size)
                }
                else ->
                    StoredValue.Small
            }
        }

        private fun StoredValue.temporaryVals(): Collection<StoreData> =
            when (this) {
                is StoredValue.Store -> setOf(this.temporaryVal)
                is StoredValue.DirtyStore -> this.temporaryVals
                else -> emptySet()
            }
    }
}