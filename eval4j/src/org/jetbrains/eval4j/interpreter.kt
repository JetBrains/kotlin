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

package org.jetbrains.eval4j

import org.jetbrains.org.objectweb.asm.tree.analysis.*
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.IntInsnNode
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.org.objectweb.asm.tree.IincInsnNode

class UnsupportedByteCodeException(message: String) : RuntimeException(message)

public trait Eval {
    public fun loadClass(classType: Type): Value
    public fun loadString(str: String): Value
    public fun newInstance(classType: Type): Value
    public fun isInstanceOf(value: Value, targetType: Type): Boolean

    public fun newArray(arrayType: Type, size: Int): Value
    public fun newMultiDimensionalArray(arrayType: Type, dimensionSizes: List<Int>): Value
    public fun getArrayLength(array: Value): Value
    public fun getArrayElement(array: Value, index: Value): Value
    public fun setArrayElement(array: Value, index: Value, newValue: Value)

    public fun getStaticField(fieldDesc: FieldDescription): Value
    public fun setStaticField(fieldDesc: FieldDescription, newValue: Value)
    public fun invokeStaticMethod(methodDesc: MethodDescription, arguments: List<Value>): Value

    public fun getField(instance: Value, fieldDesc: FieldDescription): Value
    public fun setField(instance: Value, fieldDesc: FieldDescription, newValue: Value)
    public fun invokeMethod(instance: Value, methodDesc: MethodDescription, arguments: List<Value>, invokespecial: Boolean = false): Value
}

class SingleInstructionInterpreter(private val eval: Eval) : Interpreter<Value>(ASM5) {
    override fun newValue(`type`: Type?): Value? {
        if (`type` == null) {
            return NOT_A_VALUE
        }

        return makeNotInitializedValue(`type`)
    }

    override fun newOperation(insn: AbstractInsnNode): Value? {
        return when (insn.getOpcode()) {
            ACONST_NULL -> {
                return NULL_VALUE
            }

            ICONST_M1 -> int(-1)
            ICONST_0 -> int(0)
            ICONST_1 -> int(1)
            ICONST_2 -> int(2)
            ICONST_3 -> int(3)
            ICONST_4 -> int(4)
            ICONST_5 -> int(5)

            LCONST_0 -> long(0)
            LCONST_1 -> long(1)

            FCONST_0 -> float(0.0f)
            FCONST_1 -> float(1.0f)
            FCONST_2 -> float(2.0f)

            DCONST_0 -> double(0.0)
            DCONST_1 -> double(1.0)

            BIPUSH, SIPUSH -> int((insn as IntInsnNode).operand)

            LDC -> {
                val cst = ((insn as LdcInsnNode)).cst
                when (cst) {
                    is Int -> int(cst)
                    is Float -> float(cst)
                    is Long -> long(cst)
                    is Double -> double(cst)
                    is String -> eval.loadString(cst)
                    is Type -> {
                        val sort = (cst as Type).getSort()
                        when (sort) {
                            Type.OBJECT, Type.ARRAY -> eval.loadClass(cst)
                            Type.METHOD -> throw UnsupportedByteCodeException("Mothod handles are not supported")
                            else -> throw UnsupportedByteCodeException("Illegal LDC constant " + cst)
                        }
                    }
                    is Handle -> throw UnsupportedByteCodeException("Method handles are not supported")
                    else -> throw UnsupportedByteCodeException("Illegal LDC constant " + cst)
                }
            }
            JSR -> LabelValue((insn as JumpInsnNode).label)
            GETSTATIC -> eval.getStaticField(FieldDescription(insn as FieldInsnNode))
            NEW -> eval.newInstance(Type.getObjectType((insn as TypeInsnNode).desc))
            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    override fun copyOperation(insn: AbstractInsnNode, value: Value): Value {
        return value
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: Value): Value? {
        return when (insn.getOpcode()) {
            INEG -> int(-value.int)
            IINC -> int(value.int + (insn as IincInsnNode).incr)
            L2I -> int(value.long.toInt())
            F2I -> int(value.float.toInt())
            D2I -> int(value.double.toInt())
            I2B -> byte(value.int.toByte())
            I2C -> char(value.int.toChar())
            I2S -> short(value.int.toShort())

            FNEG -> float(-value.float)
            I2F -> float(value.int.toFloat())
            L2F -> float(value.long.toFloat())
            D2F -> float(value.double.toFloat())

            LNEG -> long(-value.long)
            I2L -> long(value.int.toLong())
            F2L -> long(value.float.toLong())
            D2L -> long(value.double.toLong())

            DNEG -> double(-value.double)
            I2D -> double(value.int.toDouble())
            L2D -> double(value.long.toDouble())
            F2D -> double(value.float.toDouble())

            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL -> {
                // Handled by interpreter loop, see checkUnaryCondition()
                null
            }

            // TODO: switch
            TABLESWITCH,
            LOOKUPSWITCH -> throw UnsupportedByteCodeException("Switch is not supported yet")

            PUTSTATIC -> {
                eval.setStaticField(FieldDescription(insn as FieldInsnNode), value)
                null
            }

            GETFIELD -> eval.getField(value, FieldDescription(insn as FieldInsnNode))

            NEWARRAY -> {
                val typeStr = when ((insn as IntInsnNode).operand) {
                    T_BOOLEAN -> "[Z"
                    T_CHAR    -> "[C"
                    T_BYTE    -> "[B"
                    T_SHORT   -> "[S"
                    T_INT     -> "[I"
                    T_FLOAT   -> "[F"
                    T_DOUBLE  -> "[D"
                    T_LONG    -> "[J"
                    else -> throw AnalyzerException(insn, "Invalid array type")
                }
                eval.newArray(Type.getType(typeStr), value.int)
            }
            ANEWARRAY -> {
                val desc = (insn as TypeInsnNode).desc
                eval.newArray(Type.getType("[" + Type.getObjectType(desc)), value.int)
            }
            ARRAYLENGTH -> eval.getArrayLength(value)

            ATHROW -> {
                // Handled by interpreter loop
                null
            }

            CHECKCAST -> {
                val targetType = Type.getObjectType((insn as TypeInsnNode).desc)
                if (value == NULL_VALUE) {
                    NULL_VALUE
                } else if (eval.isInstanceOf(value, targetType)) {
                    ObjectValue(value.obj(), targetType)
                }
                else {
                    throwEvalException(ClassCastException("${value.asmType.getClassName()} cannot be cast to ${targetType.getClassName()}"))
                }
            }

            INSTANCEOF -> {
                val targetType = Type.getObjectType((insn as TypeInsnNode).desc)
                boolean(eval.isInstanceOf(value, targetType))
            }

            // TODO: maybe just do nothing?
            MONITORENTER, MONITOREXIT -> throw UnsupportedByteCodeException("Monitor instructions are not supported")

            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    public fun checkUnaryCondition(value: Value, opcode: Int): Boolean {
        return when (opcode) {
            IFEQ -> value.int == 0
            IFNE -> value.int != 0
            IFLT -> value.int < 0
            IFGT -> value.int > 0
            IFLE -> value.int <= 0
            IFGE -> value.int >= 0
            IFNULL -> value.obj() == null
            IFNONNULL -> value.obj() != null
            else -> throw UnsupportedByteCodeException("Unknown opcode: $opcode")
        }
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: Value, value2: Value): Value? {
        return when (insn.getOpcode()) {
            IALOAD, BALOAD, CALOAD, SALOAD,
            FALOAD, LALOAD, DALOAD,
            AALOAD -> eval.getArrayElement(value1, value2)

            IADD -> int(value1.int + value2.int)
            ISUB -> int(value1.int - value2.int)
            IMUL -> int(value1.int * value2.int)
            IDIV -> int(value1.int / value2.int)
            IREM -> int(value1.int % value2.int)
            ISHL -> int(value1.int shl value2.int)
            ISHR -> int(value1.int shr value2.int)
            IUSHR -> int(value1.int ushr value2.int)
            IAND -> int(value1.int and value2.int)
            IOR -> int(value1.int or value2.int)
            IXOR -> int(value1.int xor value2.int)

            LADD -> long(value1.long + value2.long)
            LSUB -> long(value1.long - value2.long)
            LMUL -> long(value1.long * value2.long)
            LDIV -> long(value1.long / value2.long)
            LREM -> long(value1.long % value2.long)
            LSHL -> long(value1.long shl value2.int)
            LSHR -> long(value1.long shr value2.int)
            LUSHR -> long(value1.long ushr value2.int)
            LAND -> long(value1.long and value2.long)
            LOR -> long(value1.long or value2.long)
            LXOR -> long(value1.long xor value2.long)

            FADD -> float(value1.float + value2.float)
            FSUB -> float(value1.float - value2.float)
            FMUL -> float(value1.float * value2.float)
            FDIV -> float(value1.float / value2.float)
            FREM -> float(value1.float % value2.float)

            DADD -> double(value1.double + value2.double)
            DSUB -> double(value1.double - value2.double)
            DMUL -> double(value1.double * value2.double)
            DDIV -> double(value1.double / value2.double)
            DREM -> double(value1.double % value2.double)

            LCMP -> {
                val l1 = value1.long
                val l2 = value2.long

                int(when {
                    l1 > l2 -> 1
                    l1 == l2 -> 0
                    else -> -1
                })
            }

            FCMPL,
            FCMPG -> {
                val l1 = value1.float
                val l2 = value2.float

                int(when {
                    l1 > l2 -> 1
                    l1 == l2 -> 0
                    l1 < l2 -> -1
                    // one of them is NaN
                    else -> if (insn.getOpcode() == FCMPG) 1 else -1
                })
            }

            DCMPL,
            DCMPG -> {
                val l1 = value1.double
                val l2 = value2.double

                int(when {
                    l1 > l2 -> 1
                    l1 == l2 -> 0
                    l1 < l2 -> -1
                    // one of them is NaN
                    else -> if (insn.getOpcode() == DCMPG) 1 else -1
                })
            }

            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> {
                // Handled by interpreter loop, see checkBinaryCondition()
                null
            }

            PUTFIELD -> {
                eval.setField(value1, FieldDescription(insn as FieldInsnNode), value2)
                null
            }

            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    public fun checkBinaryCondition(value1: Value, value2: Value, opcode: Int): Boolean {
        return when (opcode) {
            IF_ICMPEQ -> value1.int == value2.int
            IF_ICMPNE -> value1.int != value2.int
            IF_ICMPLT -> value1.int < value2.int
            IF_ICMPGT -> value1.int > value2.int
            IF_ICMPLE -> value1.int <= value2.int
            IF_ICMPGE -> value1.int >= value2.int

            IF_ACMPEQ -> value1.obj() == value2.obj()
            IF_ACMPNE -> value1.obj() != value2.obj()
            else -> throw UnsupportedByteCodeException("Unknown opcode: $opcode")
        }
    }

    override fun ternaryOperation(insn: AbstractInsnNode, value1: Value, value2: Value, value3: Value): Value? {
        return when (insn.getOpcode()) {
            IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE -> {
                eval.setArrayElement(value1, value2, value3)
                null
            }
            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    override fun naryOperation(insn: AbstractInsnNode, values: List<Value>): Value {
        return when (insn.getOpcode()) {
            MULTIANEWARRAY -> {
                val node = insn as MultiANewArrayInsnNode
                eval.newMultiDimensionalArray(Type.getType(node.desc), values.map { v -> v.int })
            }

            INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE -> {
                eval.invokeMethod(
                        values[0],
                        MethodDescription(insn as MethodInsnNode),
                        values.subList(1, values.size()),
                        insn.getOpcode() == INVOKESPECIAL
                )
            }

            INVOKESTATIC -> eval.invokeStaticMethod(MethodDescription(insn as MethodInsnNode), values)

            INVOKEDYNAMIC -> throw UnsupportedByteCodeException("INVOKEDYNAMIC is not supported")
            else -> throw UnsupportedByteCodeException("$insn")
        }
    }


    override fun returnOperation(insn: AbstractInsnNode, value: Value, expected: Value) {
        when (insn.getOpcode()) {
            IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> {
                // Handled by interpreter loop
            }

            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    override fun merge(v: Value, w: Value): Value {
        // We always remember the NEW value
        return w
    }
}
