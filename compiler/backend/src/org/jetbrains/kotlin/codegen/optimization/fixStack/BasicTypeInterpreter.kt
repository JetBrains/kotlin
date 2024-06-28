/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.fixStack

import org.jetbrains.kotlin.codegen.inline.insnOpcodeText
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Value

abstract class BasicTypeInterpreter<V : Value> : Interpreter<V>(API_VERSION) {

    protected abstract fun uninitializedValue(): V
    protected abstract fun booleanValue(): V
    protected abstract fun charValue(): V
    protected abstract fun byteValue(): V
    protected abstract fun shortValue(): V
    protected abstract fun intValue(): V
    protected abstract fun longValue(): V
    protected abstract fun floatValue(): V
    protected abstract fun doubleValue(): V
    protected abstract fun nullValue(): V
    protected abstract fun objectValue(type: Type): V
    protected abstract fun arrayValue(type: Type): V
    protected abstract fun methodValue(type: Type): V
    protected abstract fun handleValue(handle: Handle): V
    protected abstract fun typeConstValue(typeConst: Type): V
    protected abstract fun aaLoadValue(arrayValue: V): V
    
    override fun newValue(type: Type?): V? =
        if (type == null)
            uninitializedValue()
        else when (type.sort) {
            Type.VOID -> null
            Type.BOOLEAN -> booleanValue()
            Type.CHAR -> charValue()
            Type.BYTE -> byteValue()
            Type.SHORT -> shortValue()
            Type.INT -> intValue()
            Type.FLOAT -> floatValue()
            Type.LONG -> longValue()
            Type.DOUBLE -> doubleValue()
            Type.ARRAY -> arrayValue(type)
            Type.OBJECT -> objectValue(type)
            Type.METHOD -> methodValue(type)
            else -> throw AssertionError("Unexpected type: $type")
        }

    override fun newEmptyValue(local: Int): V {
        return uninitializedValue()
    }

    override fun newOperation(insn: AbstractInsnNode): V? =
        when (insn.opcode) {
            ACONST_NULL ->
                nullValue()
            ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 ->
                intValue()
            LCONST_0, LCONST_1 ->
                longValue()
            FCONST_0, FCONST_1, FCONST_2 ->
                floatValue()
            DCONST_0, DCONST_1 ->
                doubleValue()
            BIPUSH, SIPUSH ->
                intValue()
            LDC -> {
                when (val cst = (insn as LdcInsnNode).cst) {
                    is Int -> intValue()
                    is Float -> floatValue()
                    is Long -> longValue()
                    is Double -> doubleValue()
                    is String -> objectValue(AsmTypes.JAVA_STRING_TYPE)
                    is Handle -> handleValue(cst)
                    is Type -> typeConstValue(cst)
                    else -> throw IllegalArgumentException("Illegal LDC constant $cst")
                }
            }
            GETSTATIC ->
                newValue(Type.getType((insn as FieldInsnNode).desc))
            NEW ->
                newValue(Type.getObjectType((insn as TypeInsnNode).desc))
            else ->
                throw IllegalArgumentException("Unexpected instruction: " + insn.insnOpcodeText)
        }

    override fun binaryOperation(insn: AbstractInsnNode, value1: V, value2: V): V? =
        when (insn.opcode) {
            IALOAD, BALOAD, CALOAD, SALOAD, IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR ->
                intValue()
            FALOAD, FADD, FSUB, FMUL, FDIV, FREM ->
                floatValue()
            LALOAD, LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR ->
                longValue()
            DALOAD, DADD, DSUB, DMUL, DDIV, DREM ->
                doubleValue()
            AALOAD ->
                aaLoadValue(value1)
            LCMP, FCMPL, FCMPG, DCMPL, DCMPG ->
                intValue()
            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, PUTFIELD ->
                null
            else ->
                throw IllegalArgumentException("Unexpected instruction: " + insn.insnOpcodeText)
        }

    override fun ternaryOperation(insn: AbstractInsnNode, value1: V, value2: V, value3: V): V? =
        null

    override fun naryOperation(insn: AbstractInsnNode, values: List<V>): V? =
        when (insn.opcode) {
            MULTIANEWARRAY ->
                newValue(Type.getType((insn as MultiANewArrayInsnNode).desc))
            INVOKEDYNAMIC ->
                newValue(Type.getReturnType((insn as InvokeDynamicInsnNode).desc))
            else ->
                newValue(Type.getReturnType((insn as MethodInsnNode).desc))
        }

    override fun returnOperation(insn: AbstractInsnNode, value: V?, expected: V?) {
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: V): V? =
        when (insn.opcode) {
            INEG, IINC, L2I, F2I, D2I, I2B, I2C, I2S ->
                intValue()
            FNEG, I2F, L2F, D2F ->
                floatValue()
            LNEG, I2L, F2L, D2L ->
                longValue()
            DNEG, I2D, L2D, F2D ->
                doubleValue()
            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, TABLESWITCH, LOOKUPSWITCH, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, PUTSTATIC ->
                null
            GETFIELD ->
                newValue(Type.getType((insn as FieldInsnNode).desc))
            NEWARRAY ->
                when ((insn as IntInsnNode).operand) {
                    T_BOOLEAN -> newValue(Type.getType("[Z"))
                    T_CHAR -> newValue(Type.getType("[C"))
                    T_BYTE -> newValue(Type.getType("[B"))
                    T_SHORT -> newValue(Type.getType("[S"))
                    T_INT -> newValue(Type.getType("[I"))
                    T_FLOAT -> newValue(Type.getType("[F"))
                    T_DOUBLE -> newValue(Type.getType("[D"))
                    T_LONG -> newValue(Type.getType("[J"))
                    else -> throw AnalyzerException(insn, "Invalid array type")
                }
            ANEWARRAY ->
                newValue(Type.getType("[" + Type.getObjectType((insn as TypeInsnNode).desc)))
            ARRAYLENGTH ->
                intValue()
            ATHROW ->
                null
            CHECKCAST ->
                newValue(Type.getObjectType((insn as TypeInsnNode).desc))
            INSTANCEOF ->
                intValue()
            MONITORENTER, MONITOREXIT, IFNULL, IFNONNULL ->
                null
            else ->
                throw IllegalArgumentException("Unexpected instruction: " + insn.insnOpcodeText)
        }
}