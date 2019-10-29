/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.kotlin.codegen.inline.MaxStackFrameSizeAndLocalsCalculator
import org.jetbrains.kotlin.codegen.inline.insnText
import org.jetbrains.kotlin.codegen.optimization.removeNodeGetNext
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsn
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*

val AbstractInsnNode.isMeaningful: Boolean
    get() =
        when (this.type) {
            AbstractInsnNode.LABEL, AbstractInsnNode.LINE, AbstractInsnNode.FRAME -> false
            else -> true
        }

val AbstractInsnNode.isBranchOrCall: Boolean
    get() =
        when(this.type) {
            AbstractInsnNode.JUMP_INSN,
            AbstractInsnNode.TABLESWITCH_INSN,
            AbstractInsnNode.LOOKUPSWITCH_INSN,
            AbstractInsnNode.METHOD_INSN -> true
            else -> false
        }

class InsnSequence(val from: AbstractInsnNode, val to: AbstractInsnNode?) : Sequence<AbstractInsnNode> {
    constructor(insnList: InsnList) : this(insnList.first, null)

    override fun iterator(): Iterator<AbstractInsnNode> {
        return object : Iterator<AbstractInsnNode> {
            var current: AbstractInsnNode? = from
            override fun next(): AbstractInsnNode {
                val result = current
                current = current!!.next
                return result!!
            }

            override fun hasNext() = current != to
        }
    }
}

fun InsnList.asSequence() = InsnSequence(this)

fun MethodNode.prepareForEmitting() {
    stripOptimizationMarkers()

    removeEmptyCatchBlocks()

    // local variables with live ranges starting after last meaningful instruction lead to VerifyError
    localVariables = localVariables.filter { lv ->
        InsnSequence(lv.start, lv.end).any(AbstractInsnNode::isMeaningful)
    }

    // We should remove linenumbers after last meaningful instruction
    // because they point to index of non-existing instruction and it leads to VerifyError
    var current = instructions.last
    while (!current.isMeaningful) {
        val prev = current.previous

        if (current.type == AbstractInsnNode.LINE) {
            instructions.remove(current)
        }

        current = prev
    }
    maxStack = -1
    accept(
        MaxStackFrameSizeAndLocalsCalculator(
            Opcodes.API_VERSION, access, desc,
            object : MethodVisitor(Opcodes.API_VERSION) {
                override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                    this@prepareForEmitting.maxStack = maxStack
                }
            })
    )
}

fun MethodNode.stripOptimizationMarkers() {
    var insn = instructions.first
    while (insn != null) {
        if (isOptimizationMarker(insn)) {
            insn = instructions.removeNodeGetNext(insn)
        } else {
            insn = insn.next
        }
    }
}

private fun isOptimizationMarker(insn: AbstractInsnNode) =
    PseudoInsn.STORE_NOT_NULL.isa(insn)

fun MethodNode.removeEmptyCatchBlocks() {
    tryCatchBlocks = tryCatchBlocks.filter { tcb ->
        InsnSequence(tcb.start, tcb.end).any(AbstractInsnNode::isMeaningful)
    }
}

fun MethodNode.removeUnusedLocalVariables() {
    val used = BooleanArray(maxLocals) { false }
    for (insn in instructions) {
        when (insn) {
            is VarInsnNode -> {
                val varIndex = insn.`var`
                used[varIndex] = true
                if (insn.isSize2LoadStoreOperation()) {
                    used[varIndex + 1] = true
                }
            }
            is IincInsnNode ->
                used[insn.`var`] = true
        }
    }
    for (localVar in localVariables) {
        val varIndex = localVar.index
        used[varIndex] = true
        val type = Type.getType(localVar.desc)
        if (type.size == 2) {
            used[varIndex + 1] = true
        }
    }

    if (used.all { it }) return

    val remapping = IntArray(maxLocals) { 0 }
    var lastUnused = 0
    for (i in remapping.indices) {
        remapping[i] = lastUnused
        if (used[i]) {
            lastUnused++
        }
    }

    remapLocalVariables(remapping)
}

private fun VarInsnNode.isSize2LoadStoreOperation() =
    opcode == LLOAD || opcode == DLOAD || opcode == LSTORE || opcode == DSTORE

fun MethodNode.remapLocalVariables(remapping: IntArray) {
    for (insn in instructions.toArray()) {
        when (insn) {
            is VarInsnNode ->
                insn.`var` = remapping[insn.`var`]
            is IincInsnNode ->
                insn.`var` = remapping[insn.`var`]
        }
    }

    for (localVariableNode in localVariables) {
        localVariableNode.index = remapping[localVariableNode.index]
    }
}

inline fun AbstractInsnNode.findNextOrNull(predicate: (AbstractInsnNode) -> Boolean): AbstractInsnNode? {
    var finger = this.next
    while (finger != null && !predicate(finger)) {
        finger = finger.next
    }
    return finger
}

inline fun AbstractInsnNode.findPreviousOrNull(predicate: (AbstractInsnNode) -> Boolean): AbstractInsnNode? {
    var finger = this.previous
    while (finger != null && !predicate(finger)) {
        finger = finger.previous
    }
    return finger
}

fun AbstractInsnNode.hasOpcode(): Boolean =
    opcode >= 0

//   See InstructionAdapter
//
//   public void iconst(final int cst) {
//       if (cst >= -1 && cst <= 5) {
//           mv.visitInsn(Opcodes.ICONST_0 + cst);
//       } else if (cst >= Byte.MIN_VALUE && cst <= Byte.MAX_VALUE) {
//           mv.visitIntInsn(Opcodes.BIPUSH, cst);
//       } else if (cst >= Short.MIN_VALUE && cst <= Short.MAX_VALUE) {
//           mv.visitIntInsn(Opcodes.SIPUSH, cst);
//       } else {
//           mv.visitLdcInsn(new Integer(cst));
//       }
//   }
val AbstractInsnNode.intConstant: Int?
    get() =
        when (opcode) {
            in ICONST_M1..ICONST_5 -> opcode - ICONST_0
            BIPUSH, SIPUSH -> (this as IntInsnNode).operand
            LDC -> (this as LdcInsnNode).cst as? Int
            else -> null
        }

fun insnListOf(vararg insns: AbstractInsnNode) = InsnList().apply { insns.forEach { add(it) } }

fun AbstractInsnNode.isStoreOperation(): Boolean = opcode in Opcodes.ISTORE..Opcodes.ASTORE
fun AbstractInsnNode.isLoadOperation(): Boolean = opcode in Opcodes.ILOAD..Opcodes.ALOAD

val AbstractInsnNode?.debugText
    get() =
        if (this == null) "<null>" else "${this::class.java.simpleName}: $insnText"

internal inline fun <reified T : AbstractInsnNode> AbstractInsnNode.isInsn(opcode: Int, condition: T.() -> Boolean): Boolean =
    takeInsnIf(opcode, condition) != null

internal inline fun <reified T : AbstractInsnNode> AbstractInsnNode.takeInsnIf(opcode: Int, condition: T.() -> Boolean): T? =
    takeIf { it.opcode == opcode }?.safeAs<T>()?.takeIf { it.condition() }

fun InsnList.removeAll(nodes: Collection<AbstractInsnNode>) {
    for (node in nodes) remove(node)
}
