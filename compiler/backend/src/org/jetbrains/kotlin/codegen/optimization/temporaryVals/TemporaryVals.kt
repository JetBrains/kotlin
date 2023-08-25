/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.temporaryVals

import org.jetbrains.kotlin.codegen.optimization.OptimizationMethodVisitor
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.common.isStoreOperation
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode


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
        val insnList = methodNode.instructions
        val insnArray = insnList.toArray()

        val potentiallyTemporaryStores = insnList.filterIsInstance<VarInsnNode>().filterTo(LinkedHashSet()) { it.isStoreOperation() }

        for (lv in methodNode.localVariables) {
            // Exclude stores within LVT entry liveness ranges.
            for (i in insnList.indexOf(lv.start) until insnList.indexOf(lv.end)) {
                val insn = insnArray[i]
                if (insn.isStoreOperation() && (insn as VarInsnNode).`var` == lv.index) {
                    potentiallyTemporaryStores.remove(insn)
                }
            }

            // Remove 1st store that would definitely be observed at local variable liveness start.
            var p = lv.start.previous
            while (p != null) {
                if (p.isStoreOperation() && (p as VarInsnNode).`var` == lv.index) {
                    potentiallyTemporaryStores.remove(p)
                    break
                } else if (
                    p.type == AbstractInsnNode.LABEL ||
                    p.opcode in Opcodes.IRETURN..Opcodes.RETURN ||
                    p.opcode == Opcodes.GOTO ||
                    p.opcode == Opcodes.ATHROW
                ) {
                    // Label might be a jump target;
                    // return/goto/throw instructions don't pass control to the next instruction.
                    break
                } else {
                    p = p.previous
                }
            }
        }

        for (tcb in methodNode.tryCatchBlocks) {
            // Some coroutine transformations require exception handler to start from an ASTORE instruction.
            var handlerFirstInsn: AbstractInsnNode? = tcb.handler
            while (handlerFirstInsn != null && !handlerFirstInsn.isMeaningful) {
                handlerFirstInsn = handlerFirstInsn.next
            }
            if (handlerFirstInsn != null && handlerFirstInsn.opcode == Opcodes.ASTORE) {
                potentiallyTemporaryStores.remove(handlerFirstInsn)
            }
            // Don't touch stack spilling at TCB start
            var insn = tcb.start.previous
            while (insn != null && insn.isStoreOperation()) {
                potentiallyTemporaryStores.remove(insn)
                insn = insn.previous
            }
        }

        // Don't run analysis if we have no potential temporary val stores.
        if (potentiallyTemporaryStores.isEmpty())
            return emptyList()

        // If the method is big, and we couldn't eliminate enough temporary variable store candidates,
        // bail out, treat all variables as non-temporary.
        // Here we estimate memory required to store all relevant information as O(N * M * K),
        //  N = number of method instructions
        //  M = number of local variables
        //  K = number of potential temporary variables so far
        val memoryComplexity = methodNode.instructions.size().toLong() *
                methodNode.localVariables.size *
                potentiallyTemporaryStores.size /
                (1024 * 1024)
        if (memoryComplexity > OptimizationMethodVisitor.MEMORY_LIMIT_BY_METHOD_MB)
            return emptyList()

        val storeInsnToStoreData = potentiallyTemporaryStores.associateWith { StoreData(it) }

        val frames = FastStoreLoadAnalyzer(internalClassName, methodNode, StoreTrackingInterpreter(storeInsnToStoreData)).analyze()

        // Exclude stores observed at LVT liveness range start using information from bytecode analysis.
        for (lv in methodNode.localVariables) {
            val frameAtStart = frames[insnList.indexOf(lv.start)] ?: continue
            when (val valueAtStart = frameAtStart.getLocal(lv.index)) {
                is StoredValue.Store ->
                    valueAtStart.temporaryVal.isDirty = true
                is StoredValue.DirtyStore ->
                    valueAtStart.temporaryVals.forEach { it.isDirty = true }
                StoredValue.Unknown -> {}
            }
        }

        return storeInsnToStoreData.values
            .filterNot { it.isDirty }
            .map { TemporaryVal(it.storeInsn.`var`, it.storeInsn, it.loads.toList()) }
            .sortedBy { insnList.indexOf(it.storeInsn) }
    }

    private class StoreData(val storeInsn: VarInsnNode) {
        var isDirty = false

        val value = StoredValue.Store(this)

        val loads = LinkedHashSet<VarInsnNode>()
    }

    private sealed class StoredValue : StoreLoadValue {
        // `StoredValue` represent some abstract value that doesn't really have a definition of size.
        // `getSize` can return either 1 or 2, so we use 1 here.
        override fun getSize(): Int = 1

        object Unknown : StoredValue()

        class Store(val temporaryVal: StoreData) : StoredValue() {
            override fun equals(other: Any?): Boolean =
                other is Store && other.temporaryVal === temporaryVal

            override fun hashCode(): Int =
                temporaryVal.hashCode()
        }

        class DirtyStore(val temporaryVals: Collection<StoreData>) : StoredValue() {
            override fun equals(other: Any?): Boolean =
                other is DirtyStore && other.temporaryVals == temporaryVals

            override fun hashCode(): Int =
                temporaryVals.hashCode()
        }
    }

    private class StoreTrackingInterpreter(
        private val storeInsnToStoreData: Map<VarInsnNode, StoreData>
    ) : StoreLoadInterpreter<StoredValue>() {

        override fun newEmptyValue(local: Int): StoredValue = StoredValue.Unknown

        override fun newValue(type: Type?): StoredValue = StoredValue.Unknown

        override fun copyOperation(insn: AbstractInsnNode, value: StoredValue?): StoredValue {
            if (value == null) {
                val temporaryValData = storeInsnToStoreData[insn]
                if (temporaryValData != null) {
                    return temporaryValData.value
                }
            } else if (value is StoredValue.DirtyStore) {
                // If we load a dirty value, invalidate all related temporary vals.
                value.temporaryVals.forEach { it.isDirty = true }
            } else if (value is StoredValue.Store) {
                // Keep track of a load instruction
                value.temporaryVal.loads.add(insn as VarInsnNode)
            }
            return StoredValue.Unknown
        }

        override fun unaryOperation(insn: AbstractInsnNode, value: StoredValue): StoredValue {
            if (insn.opcode != Opcodes.IINC) return value
            when (value) {
                is StoredValue.Store ->
                    value.temporaryVal.isDirty = true
                is StoredValue.DirtyStore ->
                    value.temporaryVals.forEach { it.isDirty = true }
                else -> {
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
                    StoredValue.DirtyStore(dirtySet)
                }
                else ->
                    StoredValue.Unknown
            }
        }

        private fun StoredValue.temporaryVals(): Collection<StoreData> =
            when (this) {
                is StoredValue.Store -> setOf(this.temporaryVal)
                is StoredValue.DirtyStore -> this.temporaryVals
                else -> emptySet()
            }

        override fun newOperation(insn: AbstractInsnNode?): StoredValue {
            error("Should not be called")
        }

        override fun binaryOperation(insn: AbstractInsnNode?, value1: StoredValue?, value2: StoredValue?): StoredValue {
            error("Should not be called")
        }

        override fun ternaryOperation(
            insn: AbstractInsnNode?, value1: StoredValue?, value2: StoredValue?, value3: StoredValue?
        ): StoredValue {
            error("Should not be called")
        }

        override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out StoredValue>?): StoredValue {
            error("Should not be called")
        }

        override fun returnOperation(insn: AbstractInsnNode?, value: StoredValue?, expected: StoredValue?) {
            error("Should not be called")
        }
    }
}
