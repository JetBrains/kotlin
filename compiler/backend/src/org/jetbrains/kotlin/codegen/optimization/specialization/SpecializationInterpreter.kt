/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.specialization

import org.jetbrains.kotlin.codegen.util.inlinecodegen.JvmSpecializeMetadataValue
import org.jetbrains.kotlin.codegen.util.inlinecodegen.LightIrType
import org.jetbrains.kotlin.codegen.util.inlinecodegen.SpecTypeParametersUsages
import org.jetbrains.kotlin.codegen.util.inlinecodegen.isSpecBootstrapCall
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Value

internal class SpecializationInterpreter(
    val localToArgumentIndex: Map<Int, Int>,
    val metadataValue: JvmSpecializeMetadataValue,
) : Interpreter<SpecializationValue>(Opcodes.ASM9) {
    val specializedLoadStore = mutableListOf<Pair<VarInsnNode, SpecTypeParametersUsages.Usage>>()

    override fun newValue(type: Type?): SpecializationValue? {
        if (type == null) return SpecializationValue(1)
        if (type == Type.VOID_TYPE) return null
        return SpecializationValue(type.size)
    }

    override fun newReturnTypeValue(type: Type?): SpecializationValue? {
        metadataValue.specTypeParametersUsages.returnType?.let { return SpecializationValue(it) }
        return newValue(type)
    }

    override fun newParameterValue(isInstanceMethod: Boolean, local: Int, type: Type?): SpecializationValue? {
        val argumentIndex = localToArgumentIndex[local] ?: error("bad local variable index")
        metadataValue.specTypeParametersUsages.parameterGenericIndices[argumentIndex]?.let { return SpecializationValue(it) }
        return newValue(type)
    }

    override fun newOperation(insn: AbstractInsnNode): SpecializationValue {
        val size = when (insn.opcode) {
            Opcodes.LCONST_0, Opcodes.LCONST_1, Opcodes.DCONST_0, Opcodes.DCONST_1 -> 2
            Opcodes.GETSTATIC -> Type.getType((insn as FieldInsnNode).desc).size
            Opcodes.LDC -> when ((insn as LdcInsnNode).cst) {
                is Long, is Double -> 2
                else -> 1
            }
            else -> 1
        }
        return SpecializationValue(size)
    }

    override fun copyOperation(
        insn: AbstractInsnNode,
        value: SpecializationValue?,
    ): SpecializationValue? {
        value?.genericUsage?.let { genericUsage ->
            when (insn.opcode) {
                Opcodes.ALOAD, Opcodes.ASTORE -> specializedLoadStore.add(insn as VarInsnNode to genericUsage)
                // TODO: dup and swap
            }
        }
        return value
    }

    override fun unaryOperation(
        insn: AbstractInsnNode,
        value: SpecializationValue?,
    ): SpecializationValue? {
        val size = when (insn.opcode) {
            Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN,
            Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE, Opcodes.IFNULL, Opcodes.IFNONNULL,
            Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH,
            Opcodes.PUTSTATIC,
            Opcodes.MONITORENTER, Opcodes.MONITOREXIT,
                -> return null

            Opcodes.INEG, Opcodes.FNEG, Opcodes.IINC, Opcodes.I2F, Opcodes.L2I, Opcodes.L2F,
            Opcodes.F2I, Opcodes.D2I, Opcodes.D2F, Opcodes.I2B, Opcodes.I2C, Opcodes.I2S,
            Opcodes.CHECKCAST, Opcodes.NEWARRAY, Opcodes.ANEWARRAY, Opcodes.ARRAYLENGTH, Opcodes.ATHROW,
            Opcodes.INSTANCEOF,
                -> 1

            Opcodes.LNEG, Opcodes.DNEG, Opcodes.I2L, Opcodes.I2D, Opcodes.L2D, Opcodes.F2L, Opcodes.F2D, Opcodes.D2L,
                -> 2

            Opcodes.GETFIELD -> Type.getType((insn as FieldInsnNode).desc).size
            else -> error("unexpected opcode: ${insn.opcode}")
        }

        return SpecializationValue(size)
    }

    override fun binaryOperation(
        insn: AbstractInsnNode,
        value1: SpecializationValue?,
        value2: SpecializationValue?,
    ): SpecializationValue? {
        val size = when (insn.opcode) {
            Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE,
            Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE,
            Opcodes.PUTFIELD,
                -> return null

            Opcodes.IALOAD, Opcodes.FALOAD, Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD,
            Opcodes.IADD, Opcodes.FADD, Opcodes.ISUB, Opcodes.FSUB,
            Opcodes.IMUL, Opcodes.FMUL, Opcodes.IDIV, Opcodes.FDIV, Opcodes.IREM, Opcodes.FREM,
            Opcodes.ISHL, Opcodes.ISHR, Opcodes.IUSHR,
            Opcodes.IAND, Opcodes.IOR, Opcodes.IXOR,
            Opcodes.LCMP, Opcodes.FCMPL, Opcodes.FCMPG, Opcodes.DCMPL, Opcodes.DCMPG,
                -> 1

            Opcodes.LALOAD, Opcodes.DALOAD,
            Opcodes.LADD, Opcodes.DADD, Opcodes.LSUB, Opcodes.DSUB,
            Opcodes.LMUL, Opcodes.DMUL, Opcodes.LDIV, Opcodes.DDIV, Opcodes.LREM, Opcodes.DREM,
            Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR,
            Opcodes.LAND, Opcodes.LOR, Opcodes.LXOR,
                -> 2

            else -> error("unexpected opcode: ${insn.opcode}")
        }

        return SpecializationValue(size)
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode,
        value1: SpecializationValue?,
        value2: SpecializationValue?,
        value3: SpecializationValue?,
    ): SpecializationValue? {
        when (insn.opcode) {
            Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE
                -> return null

            else -> error("unexpected opcode: ${insn.opcode}")
        }
    }

    override fun naryOperation(
        insn: AbstractInsnNode,
        values: List<SpecializationValue?>,
    ): SpecializationValue? {
        if (insn is MethodInsnNode &&
            insn.opcode == Opcodes.INVOKESTATIC &&
            insn.owner == "kotlin/jvm/internal/Intrinsics" &&
            insn.name.startsWith("specializedTypeDefaultValueMarker")
        ) {
            val genericUsage = SpecTypeParametersUsages.Usage.decode(insn.name.substring("specializedTypeDefaultValueMarker".length))
            return SpecializationValue(genericUsage)
        }

        insn.isCallWithSpecializedReturnType()?.let { genericUsage ->
            return SpecializationValue(genericUsage)
        }

        return when (insn.opcode) {
            Opcodes.MULTIANEWARRAY -> SpecializationValue(1)

            Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC, Opcodes.INVOKEINTERFACE -> {
                val insn = insn as MethodInsnNode
                SpecializationValue(Type.getReturnType(insn.desc).size)
            }

            Opcodes.INVOKEDYNAMIC -> {
                val insn = insn as InvokeDynamicInsnNode
                SpecializationValue(Type.getReturnType(insn.desc).size)
            }

            else -> error("unexpected opcode: ${insn.opcode}")
        }
    }

    override fun returnOperation(
        insn: AbstractInsnNode?,
        value: SpecializationValue?,
        expected: SpecializationValue?,
    ) {
    }

    override fun merge(
        value1: SpecializationValue?,
        value2: SpecializationValue?,
    ): SpecializationValue? {
        if (value1 == null) return value2
        if (value2 == null) return value1
        if (value1.size_ != value2.size_) error("Cannot merge values of different sizes: ${value1.size_} != ${value2.size_}")
        if (value1.genericUsage != value2.genericUsage) error("Cannot merge values of different specializations: ${value1.genericUsage} != ${value2.genericUsage}")
        return value1
    }
}

/**
 * @return The index of the specialized generic that is the return type of the call
 */
internal fun AbstractInsnNode.isCallWithSpecializedReturnType(): SpecTypeParametersUsages.Usage? {
    if (this is InvokeDynamicInsnNode && this.isSpecBootstrapCall) {
        val returnTypeGenericUsage = SpecTypeParametersUsages.decode(this.bsmArgs[2] as String).returnType ?: return null
        val specializedTypeParameters = LightIrType.decodeTypeParameters(this.bsmArgs[3] as String)
        val returnTypeSpecializedTo = returnTypeGenericUsage.adjustType(specializedTypeParameters) ?: return null
        val returnTypeParameter = returnTypeSpecializedTo.classifier as? LightIrType.Classifier.TypeParameter ?: return null
        if (!returnTypeParameter.specialized) return null
        return SpecTypeParametersUsages.Usage(returnTypeParameter.index, returnTypeSpecializedTo.nullable)
    }

    if (this is MethodInsnNode &&
        this.opcode == Opcodes.INVOKESTATIC &&
        this.owner == "kotlin/jvm/internal/Intrinsics" &&
        this.name.startsWith("unboxMarker")
    ) {
        val typeParameterUsageStr = this.name.substring("unboxMarker".length)
        return SpecTypeParametersUsages.Usage.decode(typeParameterUsageStr)
    }

    return null
}

internal data class SpecializationValue(val size_: Int, val genericUsage: SpecTypeParametersUsages.Usage? = null) : Value {
    constructor(genericUsage: SpecTypeParametersUsages.Usage) : this(1, genericUsage)

    override fun getSize() = size_

    override fun toString() = if (genericUsage != null) "special@${genericUsage.encode()}" else "value(size=$size_)"
}
