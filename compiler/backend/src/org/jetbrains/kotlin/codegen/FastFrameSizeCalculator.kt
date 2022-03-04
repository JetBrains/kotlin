/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.*

internal val STACK_SIZE_CHANGE_BY_OPCODE = run {
    val s = "EFFFFFFFFGGFFFGGFFFEEFGFGFEEEEEEEEEEEEEEEEEEEEDEDEDDDDD" +
            "CDCDEEEEEEEEEEEEEEEEEEEEBABABBBBDCFFFGGGEDCDCDCDCDCDCDCDCD" +
            "CDCEEEEDDDDDDDCDCDCEFEFDDEEFFDEDEEEBDDBBDDDDDDCCCCCCCCEFED" +
            "DDCDCDEEEEEEEEEEFEEEEEEDDEEDDEE"
    IntArray(s.length) {
        s[it].code - 'E'.code
    }
}

class FastFrameSizeCalculator(private val methodNode: MethodNode) {
    private val insnList = methodNode.instructions
    private val insnArray = insnList.toArray()
    private val nInsns = insnArray.size
    private val stackSize = IntArray(nInsns)

    private val queue = IntArray(nInsns)
    private var top = 0

    private var maxStack = 0

    fun updateMaxLocals() {
        var paramsSize = Type.getArgumentsAndReturnSizes(methodNode.desc) shr 2
        if (methodNode.access and Opcodes.ACC_STATIC != 0) {
            --paramsSize
        }

        var maxLocals = paramsSize

        for (insnNode in insnArray) {
            when (insnNode.opcode) {
                Opcodes.ASTORE, Opcodes.ISTORE, Opcodes.FSTORE -> {
                    val varInsnNode = insnNode as VarInsnNode
                    val varSlot = varInsnNode.`var`
                    if (maxLocals < varSlot) {
                        maxLocals = varSlot
                    }
                }

                Opcodes.DSTORE, Opcodes.LSTORE -> {
                    val varInsnNode = insnNode as VarInsnNode
                    val varSlot = varInsnNode.`var` + 1
                    if (maxLocals < varSlot) {
                        maxLocals = varSlot
                    }
                }
            }
        }

        for (lv in methodNode.localVariables) {
            val lvSize = when (lv.desc[0]) {
                'L', 'D' -> 1
                else -> 0
            }
            val lvSlot = lv.index + lvSize
            if (maxLocals < lvSlot) {
                maxLocals = lvSlot
            }
        }

        methodNode.maxLocals = maxLocals
    }

    fun updateMaxStack() {
        if (nInsns == 0) {
            methodNode.maxStack = 0
            return
        }

        Arrays.fill(stackSize, -1)
        for (tcb in methodNode.tryCatchBlocks) {
            val tcbHandlerIndex = tcb.handler.indexOf()
            stackSize[tcbHandlerIndex] = 1
            queue[top++] = tcbHandlerIndex
        }
        stackSize[0] = 0
        queue[top++] = 0

        while (top > 0) {
            val insn = queue[--top]
            val insnStackSize = stackSize[insn]
            val insnNode = insnArray[insn]

            // TODO -> insnNode.accept(MethodVisitor) ?
            when (insnNode.type) {
                AbstractInsnNode.LABEL,
                AbstractInsnNode.LINE,
                AbstractInsnNode.FRAME,
                AbstractInsnNode.IINC_INSN ->
                    visitEdge(insn, insn + 1, insnStackSize)
                AbstractInsnNode.INSN ->
                    visitInsn(insnNode.opcode, insnStackSize, insn)
                AbstractInsnNode.INT_INSN,
                AbstractInsnNode.VAR_INSN,
                AbstractInsnNode.TYPE_INSN ->
                    visitSimpleInsn(insnNode.opcode, insnStackSize, insn)
                AbstractInsnNode.FIELD_INSN ->
                    visitFieldInsn(insnNode as FieldInsnNode, insnStackSize, insn)
                AbstractInsnNode.METHOD_INSN ->
                    visitMethodInsn(insnNode as MethodInsnNode, insnStackSize, insn)
                AbstractInsnNode.INVOKE_DYNAMIC_INSN ->
                    visitIndyInsn(insnNode as InvokeDynamicInsnNode, insnStackSize, insn)
                AbstractInsnNode.JUMP_INSN ->
                    visitJumpInsn(insnNode as JumpInsnNode, insnStackSize, insn)
                AbstractInsnNode.LDC_INSN ->
                    visitLdcInsn(insnNode as LdcInsnNode, insnStackSize, insn)
                AbstractInsnNode.TABLESWITCH_INSN -> {
                    val switchNode = insnNode as TableSwitchInsnNode
                    visitSwitchInsn(switchNode.dflt, switchNode.labels, insnStackSize, insn)
                }
                AbstractInsnNode.LOOKUPSWITCH_INSN -> {
                    val switchNode = insnNode as LookupSwitchInsnNode
                    visitSwitchInsn(switchNode.dflt, switchNode.labels, insnStackSize, insn)
                }
                AbstractInsnNode.MULTIANEWARRAY_INSN ->
                    visitMultiNewArrayInsn(insnNode as MultiANewArrayInsnNode, insnStackSize, insn)
            }
        }

        methodNode.maxStack = maxStack
    }

    private fun visitMultiNewArrayInsn(insnNode: MultiANewArrayInsnNode, insnStackSize: Int, insn: Int) {
        visitEdge(insn, insn + 1, insnStackSize + insnNode.dims - 1)
    }

    private fun visitSwitchInsn(dflt: LabelNode, labels: List<LabelNode>, insnStackSize: Int, insn: Int) {
        val outputStackSize = insnStackSize - 1
        for (label in labels) {
            visitEdge(insn, label.indexOf(), outputStackSize)
        }
        visitEdge(insn, dflt.indexOf(), outputStackSize)
    }

    private fun visitLdcInsn(insnNode: LdcInsnNode, insnStackSize: Int, insn: Int) {
        val sizeDelta = when (insnNode.cst) {
            is Long, is Double -> 2
            else -> 1
        }
        visitEdge(insn, insn + 1, insnStackSize + sizeDelta)
    }

    private fun visitIndyInsn(insnNode: InvokeDynamicInsnNode, insnStackSize: Int, insn: Int) {
        val argSize = Type.getArgumentsAndReturnSizes(insnNode.desc)
        val sizeDelta = (argSize and 0x03) - (argSize shr 2) + 1
        visitEdge(insn, insn + 1, insnStackSize + sizeDelta)
    }

    private fun visitMethodInsn(insnNode: MethodInsnNode, insnStackSize: Int, insn: Int) {
        val argSize = Type.getArgumentsAndReturnSizes(insnNode.desc)
        val sizeDelta = if (insnNode.opcode == Opcodes.INVOKESTATIC) {
            (argSize and 0x03) - (argSize shr 2) + 1
        } else {
            (argSize and 0x03) - (argSize shr 2)
        }
        visitEdge(insn, insn + 1, insnStackSize + sizeDelta)
    }

    private fun visitFieldInsn(insnNode: FieldInsnNode, insnStackSize: Int, insn: Int) {
        val fieldSize = when (insnNode.desc[0]) {
            'D', 'J' -> 2
            else -> 1
        }
        val sizeDelta = when (insnNode.opcode) {
            Opcodes.GETSTATIC -> fieldSize
            Opcodes.PUTSTATIC -> -fieldSize
            Opcodes.GETFIELD -> fieldSize - 1
            else -> -fieldSize - 1
        }
        visitEdge(insn, insn + 1, insnStackSize + sizeDelta)
    }

    private fun visitSimpleInsn(insnOpcode: Int, insnStackSize: Int, insn: Int) {
        val outputStackSize = insnStackSize + STACK_SIZE_CHANGE_BY_OPCODE[insnOpcode]
        visitEdge(insn, insn + 1, outputStackSize)
    }

    private fun visitInsn(insnOpcode: Int, insnStackSize: Int, insn: Int) {
        if (insnOpcode != Opcodes.ATHROW && insnOpcode !in Opcodes.IRETURN..Opcodes.RETURN) {
            val outputStackSize = insnStackSize + STACK_SIZE_CHANGE_BY_OPCODE[insnOpcode]
            visitEdge(insn, insn + 1, outputStackSize)
        }
    }

    private fun visitJumpInsn(insnNode: JumpInsnNode, insnStackSize: Int, insn: Int) {
        val labelIndex = insnNode.label.indexOf()
        val outputStackSize = insnStackSize + STACK_SIZE_CHANGE_BY_OPCODE[insnNode.opcode]
        if (insnNode.opcode == Opcodes.GOTO) {
            visitEdge(insn, labelIndex, insnStackSize)
        } else {
            // Conditional jump (IFEQ, IFLE, IFLT, etc)
            visitEdge(insn, labelIndex, outputStackSize)
            visitEdge(insn, insn + 1, outputStackSize)
        }
    }

    private fun visitEdge(src: Int, dest: Int, newStackSize: Int) {
        if (newStackSize < 0) {
            throw AssertionError("Stack underflow at instruction #$src")
        }
        val destStackSize = stackSize[dest]
        if (destStackSize < 0) {
            stackSize[dest] = newStackSize
            queue[top++] = dest
        } else {
            if (destStackSize != newStackSize) {
                throw AssertionError("Stack size mismatch at instruction #$dest: $newStackSize != $destStackSize")
            }
        }
        if (maxStack < newStackSize) {
            maxStack = newStackSize
        }
    }

    private fun AbstractInsnNode.indexOf() =
        insnList.indexOf(this)
}
