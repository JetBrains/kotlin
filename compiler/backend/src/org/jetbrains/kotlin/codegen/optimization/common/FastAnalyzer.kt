/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Value

abstract class FastAnalyzer<V : Value, I : Interpreter<V>, F: Frame<V>>(
    protected val owner: String,
    protected val method: MethodNode,
    protected val interpreter: I,
) {
    protected val nInsns = method.instructions.size()
    protected val handlers: Array<MutableList<TryCatchBlockNode>?> = arrayOfNulls(nInsns)

    protected fun visitMeaningfulInstruction(insnNode: AbstractInsnNode, insnType: Int, insnOpcode: Int, current: F, insn: Int) {
        when {
            insnType == AbstractInsnNode.JUMP_INSN ->
                visitJumpInsnNode(insnNode as JumpInsnNode, current, insn, insnOpcode)
            insnType == AbstractInsnNode.LOOKUPSWITCH_INSN ->
                visitLookupSwitchInsnNode(insnNode as LookupSwitchInsnNode, current)
            insnType == AbstractInsnNode.TABLESWITCH_INSN ->
                visitTableSwitchInsnNode(insnNode as TableSwitchInsnNode, current)
            insnOpcode != Opcodes.ATHROW && (insnOpcode < Opcodes.IRETURN || insnOpcode > Opcodes.RETURN) ->
                visitOpInsn(insnNode, current, insn)
            else -> {
            }
        }
    }

    protected abstract fun visitJumpInsnNode(insnNode: JumpInsnNode, current: F, insn: Int, insnOpcode: Int)
    protected abstract fun visitLookupSwitchInsnNode(insnNode: LookupSwitchInsnNode, current: F)
    protected abstract fun visitTableSwitchInsnNode(insnNode: TableSwitchInsnNode, current: F)
    protected abstract fun visitOpInsn(insnNode: AbstractInsnNode, current: F, insn: Int)

    protected fun checkAssertions() {
        if (method.instructions.any { it.opcode == Opcodes.JSR || it.opcode == Opcodes.RET })
            throw AssertionError("Subroutines are deprecated since Java 6")
    }

    protected fun AbstractInsnNode.indexOf() =
        method.instructions.indexOf(this)

    protected fun computeExceptionHandlersForEachInsn(m: MethodNode) {
        for (tcb in m.tryCatchBlocks) {
            var current: AbstractInsnNode = tcb.start
            val end = tcb.end

            while (current != end) {
                if (current.isMeaningful) {
                    val currentIndex = current.indexOf()
                    var insnHandlers: MutableList<TryCatchBlockNode>? = handlers[currentIndex]
                    if (insnHandlers == null) {
                        insnHandlers = SmartList()
                        handlers[currentIndex] = insnHandlers
                    }
                    insnHandlers.add(tcb)
                }
                current = current.next
            }
        }
    }

    protected fun F.dump(): String {
        return buildString {
            append("{\n")
            append("  locals: [\n")
            for (i in 0 until method.maxLocals) {
                append("    #$i: ${this@dump.getLocal(i)}\n")
            }
            append("  ]\n")
            val stackSize = this@dump.stackSize
            append("  stack: size=")
            append(stackSize)
            if (stackSize == 0) {
                append(" []\n")
            } else {
                append(" [\n")
                for (i in 0 until stackSize) {
                    append("    #$i: ${this@dump.getStack(i)}\n")
                }
                append("  ]\n")
            }
            append("}\n")
        }
    }
}