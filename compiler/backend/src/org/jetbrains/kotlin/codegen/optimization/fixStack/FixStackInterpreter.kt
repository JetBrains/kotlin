/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.fixStack

import org.jetbrains.kotlin.codegen.inline.insnOpcodeText
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter

open class FixStackInterpreter : Interpreter<FixStackValue>(API_VERSION) {

    override fun newValue(type: Type?): FixStackValue? =
        type?.toFixStackValue()

    override fun newOperation(insn: AbstractInsnNode): FixStackValue? =
        when (insn.opcode) {
            ACONST_NULL ->
                FixStackValue.OBJECT
            ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 ->
                FixStackValue.INT
            LCONST_0, LCONST_1 ->
                FixStackValue.LONG
            FCONST_0, FCONST_1, FCONST_2 ->
                FixStackValue.FLOAT
            DCONST_0, DCONST_1 ->
                FixStackValue.DOUBLE
            BIPUSH, SIPUSH ->
                FixStackValue.INT
            LDC -> {
                when (val cst = (insn as LdcInsnNode).cst) {
                    is Int ->
                        FixStackValue.INT
                    is Float ->
                        FixStackValue.FLOAT
                    is Long ->
                        FixStackValue.LONG
                    is Double ->
                        FixStackValue.DOUBLE
                    is String, is Handle ->
                        FixStackValue.OBJECT
                    is Type -> {
                        val sort = cst.sort
                        if (sort == Type.OBJECT || sort == Type.ARRAY || sort == Type.METHOD)
                            FixStackValue.OBJECT
                        else
                            throw IllegalArgumentException("Illegal LDC constant $cst")
                    }
                    else ->
                        throw IllegalArgumentException("Illegal LDC constant $cst")
                }
            }
            GETSTATIC ->
                newValue(Type.getType((insn as FieldInsnNode).desc))
            NEW ->
                newValue(Type.getObjectType((insn as TypeInsnNode).desc))
            else ->
                throw IllegalArgumentException("Unexpected instruction: " + insn.insnOpcodeText)
        }

    override fun copyOperation(insn: AbstractInsnNode, value: FixStackValue?): FixStackValue =
        when (insn.opcode) {
            ILOAD -> FixStackValue.INT
            LLOAD -> FixStackValue.LONG
            FLOAD -> FixStackValue.FLOAT
            DLOAD -> FixStackValue.DOUBLE
            ALOAD -> FixStackValue.OBJECT
            else -> value!!
        }

    override fun binaryOperation(insn: AbstractInsnNode, value1: FixStackValue?, value2: FixStackValue?): FixStackValue? =
        when (insn.opcode) {
            IALOAD, BALOAD, CALOAD, SALOAD, IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR ->
                FixStackValue.INT
            FALOAD, FADD, FSUB, FMUL, FDIV, FREM ->
                FixStackValue.FLOAT
            LALOAD, LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR ->
                FixStackValue.LONG
            DALOAD, DADD, DSUB, DMUL, DDIV, DREM ->
                FixStackValue.DOUBLE
            AALOAD ->
                FixStackValue.OBJECT
            LCMP, FCMPL, FCMPG, DCMPL, DCMPG ->
                FixStackValue.INT
            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, PUTFIELD ->
                null
            else ->
                throw IllegalArgumentException("Unexpected instruction: " + insn.insnOpcodeText)
        }

    override fun ternaryOperation(
        insn: AbstractInsnNode,
        value1: FixStackValue?,
        value2: FixStackValue?,
        value3: FixStackValue?
    ): FixStackValue? =
        null

    override fun naryOperation(insn: AbstractInsnNode, values: List<FixStackValue?>): FixStackValue? =
        when (insn.opcode) {
            MULTIANEWARRAY ->
                newValue(Type.getType((insn as MultiANewArrayInsnNode).desc))
            INVOKEDYNAMIC ->
                newValue(Type.getReturnType((insn as InvokeDynamicInsnNode).desc))
            else ->
                newValue(Type.getReturnType((insn as MethodInsnNode).desc))
        }

    override fun returnOperation(insn: AbstractInsnNode?, value: FixStackValue?, expected: FixStackValue?) {
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: FixStackValue?): FixStackValue? =
        when (insn.opcode) {
            INEG, IINC, L2I, F2I, D2I, I2B, I2C, I2S ->
                FixStackValue.INT
            FNEG, I2F, L2F, D2F ->
                FixStackValue.FLOAT
            LNEG, I2L, F2L, D2L ->
                FixStackValue.LONG
            DNEG, I2D, L2D, F2D ->
                FixStackValue.DOUBLE
            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, TABLESWITCH, LOOKUPSWITCH, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, PUTSTATIC ->
                null
            GETFIELD ->
                newValue(Type.getType((insn as FieldInsnNode).desc))
            NEWARRAY ->
                FixStackValue.OBJECT
            ANEWARRAY -> {
                FixStackValue.OBJECT
            }
            ARRAYLENGTH ->
                FixStackValue.INT
            ATHROW ->
                null
            CHECKCAST ->
                FixStackValue.OBJECT
            INSTANCEOF ->
                FixStackValue.INT
            MONITORENTER, MONITOREXIT, IFNULL, IFNONNULL ->
                null
            else ->
                throw IllegalArgumentException("Unexpected instruction: " + insn.insnOpcodeText)
        }

    override fun merge(v: FixStackValue?, w: FixStackValue?): FixStackValue? =
        when {
            v == w -> v
            v == null -> w
            w == null -> v
            else -> throw AssertionError("Mismatching value kinds: $v != $w")
        }
}
