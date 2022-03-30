/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame

// BasicValue interpreter from ASM does not distinct 'int' types from other int-like types like 'byte' or 'boolean',
// neither do HotSpot and JVM spec.
// But it seems like Dalvik does not follow it, and spilling boolean value into an 'int' field fails with VerifyError on Android 4,
// so this function calculates refined frames' markup.
// Note that type of some values is only possible to determine by their usages (e.g. ICONST_1, BALOAD both may push boolean or byte on stack)
// In this case, coerce the type of the value.

internal class IloadedValue(val insns: Set<VarInsnNode>) : BasicValue(Type.INT_TYPE)

private class IntLikeCoerceInterpreter : OptimizationBasicInterpreter() {
    val needsToBeCoerced = mutableMapOf<VarInsnNode, Type>()

    private fun coerce(value: IloadedValue, type: Type) {
        for (insn in value.insns) {
            needsToBeCoerced[insn] = type
        }
    }

    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? =
        when {
            insn.opcode == Opcodes.ILOAD -> IloadedValue(setOf(insn as VarInsnNode))
            value == null -> null
            else -> BasicValue(value.type)
        }

    override fun binaryOperation(insn: AbstractInsnNode, v: BasicValue, w: BasicValue): BasicValue? {
        if (insn.opcode == Opcodes.PUTFIELD) {
            val expectedType = Type.getType((insn as FieldInsnNode).desc)
            if (w is IloadedValue && expectedType.isIntLike()) {
                coerce(w, expectedType)
            }
        }
        return super.binaryOperation(insn, v, w)
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? {
        if (insn.opcode == Opcodes.PUTSTATIC) {
            val expectedType = Type.getType((insn as FieldInsnNode).desc)
            if (value is IloadedValue && expectedType.isIntLike()) {
                coerce(value, expectedType)
            }
        }
        return super.unaryOperation(insn, value)
    }

    override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out BasicValue?>): BasicValue? {
        fun checkTypes(argTypes: Array<Type>, withReceiver: Boolean) {
            val offset = if (withReceiver) 1 else 0
            for ((index, argType) in argTypes.withIndex()) {
                val value = values[index + offset] ?: continue
                if (argType.isIntLike() && value is IloadedValue) {
                    coerce(value, argType)
                }
            }
        }
        when (insn.opcode) {
            Opcodes.INVOKEDYNAMIC -> {
                checkTypes(Type.getArgumentTypes((insn as InvokeDynamicInsnNode).desc), false)
            }
            Opcodes.INVOKESTATIC -> {
                checkTypes(Type.getArgumentTypes((insn as MethodInsnNode).desc), false)
            }
            Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE, Opcodes.INVOKESPECIAL -> {
                checkTypes(Type.getArgumentTypes((insn as MethodInsnNode).desc), true)
            }
        }
        return super.naryOperation(insn, values)
    }

    override fun ternaryOperation(insn: AbstractInsnNode, arrayref: BasicValue?, index: BasicValue?, value: BasicValue?): BasicValue? {
        when (insn.opcode) {
            Opcodes.BASTORE -> {
                if (value is IloadedValue) {
                    val type = if (arrayref?.type?.descriptor == "[Z") Type.BOOLEAN_TYPE else Type.BYTE_TYPE
                    coerce(value, type)
                }
            }
            Opcodes.CASTORE -> {
                if (value is IloadedValue) {
                    coerce(value, Type.CHAR_TYPE)
                }
            }
            Opcodes.SASTORE -> {
                if (value is IloadedValue) {
                    coerce(value, Type.SHORT_TYPE)
                }
            }
        }
        return super.ternaryOperation(insn, arrayref, index, value)
    }

    override fun merge(v: BasicValue, w: BasicValue): BasicValue =
        when {
            v is IloadedValue && w is IloadedValue && v.type == w.type -> {
                val insns = v.insns + w.insns
                insns.find { it in needsToBeCoerced }?.let {
                    val type = needsToBeCoerced[it]!!
                    coerce(v, type)
                    coerce(w, type)
                }
                IloadedValue(insns)
            }
            v.type == w.type -> {
                if (w is IloadedValue) w else v
            }
            else -> super.merge(v, w)
        }
}

internal fun performSpilledVariableFieldTypesAnalysis(
    methodNode: MethodNode,
    thisName: String
): Array<out Frame<BasicValue>?> {
    val interpreter = IntLikeCoerceInterpreter()
    FastMethodAnalyzer(thisName, methodNode, interpreter).analyze()
    for ((insn, type) in interpreter.needsToBeCoerced) {
        methodNode.instructions.insert(insn, withInstructionAdapter { coerceInt(type, this) })
    }
    return FastMethodAnalyzer(thisName, methodNode, NullCheckcastAwareOptimizationBasicInterpreter()).analyze()
}

private fun coerceInt(to: Type, v: InstructionAdapter) {
    if (to == Type.BOOLEAN_TYPE) {
        with(v) {
            val zeroLabel = Label()
            val resLabel = Label()
            ifeq(zeroLabel)
            iconst(1)
            goTo(resLabel)
            mark(zeroLabel)
            iconst(0)
            mark(resLabel)
        }
    } else {
        StackValue.coerce(Type.INT_TYPE, to, v)
    }
}

private fun Type.isIntLike(): Boolean = when (sort) {
    Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT -> true
    else -> false
}

// Represents [ACONST_NULL, CHECKCAST Type] sequence result.
internal class TypedNullValue(type: Type) : BasicValue(type)

// Preserves nulls through CHECKCASTS.
private class NullCheckcastAwareOptimizationBasicInterpreter : OptimizationBasicInterpreter() {
    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? {
        if (insn.opcode == Opcodes.CHECKCAST && (value == StrictBasicValue.NULL_VALUE || value is TypedNullValue)) {
            return TypedNullValue(Type.getObjectType((insn as TypeInsnNode).desc))
        }
        return super.unaryOperation(insn, value)
    }
}