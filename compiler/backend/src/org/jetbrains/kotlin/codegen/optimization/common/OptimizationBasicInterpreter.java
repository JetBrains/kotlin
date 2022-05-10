/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.optimization.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.org.objectweb.asm.Handle;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter;

import java.util.List;

import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtilsKt.getInsnOpcodeText;
import static org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue.*;

public class OptimizationBasicInterpreter extends Interpreter<BasicValue> implements Opcodes {
    public OptimizationBasicInterpreter() {
        super(API_VERSION);
    }

    @Override
    @Nullable
    public StrictBasicValue newValue(@Nullable Type type) {
        if (type == null) {
            return UNINITIALIZED_VALUE;
        }

        switch (type.getSort()) {
            case Type.VOID:
                return null;
            case Type.INT:
                return INT_VALUE;
            case Type.FLOAT:
                return FLOAT_VALUE;
            case Type.LONG:
                return LONG_VALUE;
            case Type.DOUBLE:
                return DOUBLE_VALUE;
            case Type.BOOLEAN:
                return BOOLEAN_VALUE;
            case Type.CHAR:
                return CHAR_VALUE;
            case Type.BYTE:
                return BYTE_VALUE;
            case Type.SHORT:
                return SHORT_VALUE;
            case Type.OBJECT:
            case Type.ARRAY:
                return new StrictBasicValue(type);
            default:
                throw new IllegalArgumentException("Unknown type sort " + type.getSort());
        }
    }

    @Override
    public BasicValue newOperation(@NotNull AbstractInsnNode insn) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case ACONST_NULL:
                return NULL_VALUE;
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
                return StrictBasicValue.INT_VALUE;
            case LCONST_0:
            case LCONST_1:
                return StrictBasicValue.LONG_VALUE;
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                return StrictBasicValue.FLOAT_VALUE;
            case DCONST_0:
            case DCONST_1:
                return StrictBasicValue.DOUBLE_VALUE;
            case BIPUSH:
            case SIPUSH:
                return StrictBasicValue.INT_VALUE;
            case LDC:
                Object cst = ((LdcInsnNode) insn).cst;
                if (cst instanceof Integer) {
                    return StrictBasicValue.INT_VALUE;
                }
                else if (cst instanceof Float) {
                    return StrictBasicValue.FLOAT_VALUE;
                }
                else if (cst instanceof Long) {
                    return StrictBasicValue.LONG_VALUE;
                }
                else if (cst instanceof Double) {
                    return StrictBasicValue.DOUBLE_VALUE;
                }
                else if (cst instanceof String) {
                    return newValue(Type.getObjectType("java/lang/String"));
                }
                else if (cst instanceof Type) {
                    int sort = ((Type) cst).getSort();
                    if (sort == Type.OBJECT || sort == Type.ARRAY) {
                        return newValue(Type.getObjectType("java/lang/Class"));
                    }
                    else if (sort == Type.METHOD) {
                        return newValue(Type.getObjectType("java/lang/invoke/MethodType"));
                    }
                    else {
                        throw new IllegalArgumentException("Illegal LDC constant " + cst);
                    }
                }
                else if (cst instanceof Handle) {
                    return newValue(Type.getObjectType("java/lang/invoke/MethodHandle"));
                }
                else {
                    throw new IllegalArgumentException("Illegal LDC constant " + cst);
                }
            case GETSTATIC:
                return newValue(Type.getType(((FieldInsnNode) insn).desc));
            case NEW:
                return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
            default:
                throw new IllegalArgumentException("Unexpected instruction: " + getInsnOpcodeText(insn));
        }
    }

    @Override
    public BasicValue copyOperation(@NotNull AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        return value;
    }

    @Override
    public BasicValue binaryOperation(
            @NotNull AbstractInsnNode insn,
            @NotNull BasicValue value1,
            @NotNull BasicValue value2
    ) throws AnalyzerException {
        if (insn.getOpcode() == Opcodes.AALOAD) {
            Type arrayType = value1.getType();
            if (arrayType != null && arrayType.getSort() == Type.ARRAY) {
                return new StrictBasicValue(AsmUtil.correctElementType(arrayType));
            }
        }

        switch (insn.getOpcode()) {
            case IALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IREM:
            case ISHL:
            case ISHR:
            case IUSHR:
            case IAND:
            case IOR:
            case IXOR:
                return StrictBasicValue.INT_VALUE;
            case FALOAD:
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
                return StrictBasicValue.FLOAT_VALUE;
            case LALOAD:
            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LREM:
            case LSHL:
            case LSHR:
            case LUSHR:
            case LAND:
            case LOR:
            case LXOR:
                return StrictBasicValue.LONG_VALUE;
            case DALOAD:
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                return StrictBasicValue.DOUBLE_VALUE;
            case AALOAD:
                return StrictBasicValue.NULL_VALUE;
            case LCMP:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG:
                return StrictBasicValue.INT_VALUE;
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case PUTFIELD:
                return null;
            default:
                throw new IllegalArgumentException("Unexpected instruction: " + getInsnOpcodeText(insn));
        }
    }

    @Override
    public BasicValue ternaryOperation(
            AbstractInsnNode insn, BasicValue value1, BasicValue value2, BasicValue value3
    ) throws AnalyzerException {
        return null;
    }

    @Override
    public BasicValue naryOperation(
            AbstractInsnNode insn, List<? extends BasicValue> values
    ) throws AnalyzerException {
        int opcode = insn.getOpcode();
        if (opcode == MULTIANEWARRAY) {
            return newValue(Type.getType(((MultiANewArrayInsnNode) insn).desc));
        }
        else if (opcode == INVOKEDYNAMIC) {
            return newValue(Type.getReturnType(((InvokeDynamicInsnNode) insn).desc));
        }
        else {
            return newValue(Type.getReturnType(((MethodInsnNode) insn).desc));
        }
    }

    @Override
    public void returnOperation(
            AbstractInsnNode insn, BasicValue value, BasicValue expected
    ) throws AnalyzerException {
    }

    @Override
    public BasicValue unaryOperation(
            AbstractInsnNode insn,
            BasicValue value
    ) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case INEG:
            case IINC:
            case L2I:
            case F2I:
            case D2I:
            case I2B:
            case I2C:
            case I2S:
                return StrictBasicValue.INT_VALUE;
            case FNEG:
            case I2F:
            case L2F:
            case D2F:
                return StrictBasicValue.FLOAT_VALUE;
            case LNEG:
            case I2L:
            case F2L:
            case D2L:
                return StrictBasicValue.LONG_VALUE;
            case DNEG:
            case I2D:
            case L2D:
            case F2D:
                return StrictBasicValue.DOUBLE_VALUE;
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
            case TABLESWITCH:
            case LOOKUPSWITCH:
            case IRETURN:
            case LRETURN:
            case FRETURN:
            case DRETURN:
            case ARETURN:
            case PUTSTATIC:
                return null;
            case GETFIELD:
                return newValue(Type.getType(((FieldInsnNode) insn).desc));
            case NEWARRAY:
                switch (((IntInsnNode) insn).operand) {
                    case T_BOOLEAN:
                        return newValue(Type.getType("[Z"));
                    case T_CHAR:
                        return newValue(Type.getType("[C"));
                    case T_BYTE:
                        return newValue(Type.getType("[B"));
                    case T_SHORT:
                        return newValue(Type.getType("[S"));
                    case T_INT:
                        return newValue(Type.getType("[I"));
                    case T_FLOAT:
                        return newValue(Type.getType("[F"));
                    case T_DOUBLE:
                        return newValue(Type.getType("[D"));
                    case T_LONG:
                        return newValue(Type.getType("[J"));
                    default:
                        throw new AnalyzerException(insn, "Invalid array type");
                }
            case ANEWARRAY:
                String desc = ((TypeInsnNode) insn).desc;
                return newValue(Type.getType("[" + Type.getObjectType(desc)));
            case ARRAYLENGTH:
                return StrictBasicValue.INT_VALUE;
            case ATHROW:
                return null;
            case CHECKCAST:
                if (value == StrictBasicValue.NULL_VALUE) {
                    return StrictBasicValue.NULL_VALUE;
                }
                desc = ((TypeInsnNode) insn).desc;
                return newValue(Type.getObjectType(desc));
            case INSTANCEOF:
                return StrictBasicValue.INT_VALUE;
            case MONITORENTER:
            case MONITOREXIT:
            case IFNULL:
            case IFNONNULL:
                return null;
            default:
                throw new IllegalArgumentException("Unexpected instruction: " + getInsnOpcodeText(insn));
        }
    }

    @NotNull
    @Override
    public BasicValue merge(
            @NotNull BasicValue v, @NotNull BasicValue w
    ) {
        if (v.equals(w)) return v;

        if (v == StrictBasicValue.UNINITIALIZED_VALUE || w == StrictBasicValue.UNINITIALIZED_VALUE) {
            return StrictBasicValue.UNINITIALIZED_VALUE;
        }

        // if merge of two references then `lub` is java/lang/Object
        // arrays also are BasicValues with reference type's
        if (isReference(v) && isReference(w)) {
            if (v == NULL_VALUE) return newValue(w.getType());
            if (w == NULL_VALUE) return newValue(v.getType());

            return StrictBasicValue.REFERENCE_VALUE;
        }

        // if merge of something can be stored in int var (int, char, boolean, byte, character)
        if (v.getType().getOpcode(Opcodes.ISTORE) == Opcodes.ISTORE &&
            w.getType().getOpcode(Opcodes.ISTORE) == Opcodes.ISTORE) {
            return StrictBasicValue.INT_VALUE;
        }

        return StrictBasicValue.UNINITIALIZED_VALUE;
    }

    private static boolean isReference(@NotNull BasicValue v) {
        return v.getType().getSort() == Type.OBJECT || v.getType().getSort() == Type.ARRAY;
    }
}
