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
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*

val AbstractInsnNode.isMeaningful: Boolean
    get() =
        when (this.nodeType) {
            AbstractInsnNode.LABEL, AbstractInsnNode.LINE, AbstractInsnNode.FRAME -> false
            else -> true
        }

val AbstractInsnNode.isBranchOrCall: Boolean
    get() =
        when (this.nodeType) {
            AbstractInsnNode.JUMP_INSN,
            AbstractInsnNode.TABLESWITCH_INSN,
            AbstractInsnNode.LOOKUPSWITCH_INSN,
            AbstractInsnNode.METHOD_INSN -> true
            else -> false
        }

private val opcodeToNodeType = IntArray(IFNONNULL + 1) {
    when (it) {
        in NOP..DCONST_1, in IALOAD..SALOAD, in IASTORE..LXOR, in I2L..DCMPG, in IRETURN..RETURN,
        ARRAYLENGTH, ATHROW, MONITORENTER, MONITOREXIT -> AbstractInsnNode.INSN
        in BIPUSH..SIPUSH, NEWARRAY -> AbstractInsnNode.INT_INSN
        in ILOAD..ALOAD, in ISTORE..ASTORE, RET -> AbstractInsnNode.VAR_INSN
        NEW, ANEWARRAY, CHECKCAST, INSTANCEOF -> AbstractInsnNode.TYPE_INSN
        in GETSTATIC..PUTFIELD -> AbstractInsnNode.FIELD_INSN
        in INVOKEVIRTUAL..INVOKEINTERFACE -> AbstractInsnNode.METHOD_INSN
        INVOKEDYNAMIC -> AbstractInsnNode.INVOKE_DYNAMIC_INSN
        in IFEQ..JSR, IFNULL, IFNONNULL -> AbstractInsnNode.JUMP_INSN
        LDC -> AbstractInsnNode.LDC_INSN
        IINC -> AbstractInsnNode.IINC_INSN
        TABLESWITCH -> AbstractInsnNode.TABLESWITCH_INSN
        LOOKUPSWITCH -> AbstractInsnNode.LOOKUPSWITCH_INSN
        MULTIANEWARRAY -> AbstractInsnNode.MULTIANEWARRAY_INSN
        else -> -1
    }
}

// Faster version of `AbstractInsnNode.getType`
val AbstractInsnNode.nodeType: Int
    get() {
        return when (val opcode = this.opcode) {
            -1 -> when (this) {
                is LabelNode -> AbstractInsnNode.LABEL
                is FrameNode -> AbstractInsnNode.FRAME
                is LineNumberNode -> AbstractInsnNode.LINE
                else -> -1
            }
            else -> opcodeToNodeType.getOrElse(opcode) { -1 }
        }
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

fun InsnList.asSequence(): Sequence<AbstractInsnNode> = if (size() == 0) emptySequence() else InsnSequence(this)

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

        if (current.nodeType == AbstractInsnNode.LINE) {
            instructions.remove(current)
        }

        current = prev
    }
    updateMaxStack()
}

fun MethodNode.updateMaxStack() {
    maxStack = -1
    accept(
        MaxStackFrameSizeAndLocalsCalculator(
            API_VERSION, access, desc,
            object : MethodVisitor(API_VERSION) {
                override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                    this@updateMaxStack.maxStack = maxStack
                }
            })
    )
}

fun MethodNode.stripOptimizationMarkers() {
    var insn = instructions.first
    while (insn != null) {
        insn = if (isOptimizationMarker(insn)) {
            instructions.removeNodeGetNext(insn)
        } else {
            insn.next
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

    // Arguments are always used whether or not they are in the local variable table
    // or used by instructions.
    var argumentIndex = 0
    val isStatic = (access and ACC_STATIC) != 0
    if (!isStatic) {
        used[argumentIndex++] = true
    }
    for (argumentType in Type.getArgumentTypes(desc)) {
        for (i in 0 until argumentType.size) {
            used[argumentIndex++] = true
        }
    }

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
    for (insn in instructions) {
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

fun AbstractInsnNode.isStoreOperation(): Boolean = opcode in ISTORE..ASTORE
fun AbstractInsnNode.isLoadOperation(): Boolean = opcode in ILOAD..ALOAD

val AbstractInsnNode?.debugText
    get() =
        if (this == null) "<null>" else "${this::class.java.simpleName}: $insnText"

internal inline fun <reified T : AbstractInsnNode> AbstractInsnNode.isInsn(opcode: Int, condition: T.() -> Boolean): Boolean =
    takeInsnIf(opcode, condition) != null

internal inline fun <reified T : AbstractInsnNode> AbstractInsnNode.takeInsnIf(opcode: Int, condition: T.() -> Boolean): T? =
    (takeIf { it.opcode == opcode } as? T)?.takeIf { it.condition() }

fun InsnList.removeAll(nodes: Collection<AbstractInsnNode>) {
    for (node in nodes) remove(node)
}
