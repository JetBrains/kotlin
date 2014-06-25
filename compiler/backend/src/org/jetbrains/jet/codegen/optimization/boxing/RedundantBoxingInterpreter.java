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

package org.jetbrains.jet.codegen.optimization.boxing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.InsnList;
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import java.util.*;

class RedundantBoxingInterpreter extends BasicInterpreter {
    private final Map<Integer, BoxedBasicValue> boxingPlaces = new HashMap<Integer, BoxedBasicValue>();
    private final Set<BoxedBasicValue> candidatesBoxedValues = new HashSet<BoxedBasicValue>();
    private final InsnList insnList;

    RedundantBoxingInterpreter(InsnList insnList) {
        this.insnList = insnList;
    }

    private static boolean isClassBox(@NotNull String owner) {

        if (!owner.startsWith("java/lang/")) {
            return false;
        }

        String className = owner.substring("java/lang/".length());

        return (className.equals("Integer") ||
                className.equals("Double") ||
                className.equals("Long") ||
                className.equals("Char") ||
                className.equals("Byte") ||
                className.equals("Boolean")) ||
               className.endsWith("Number");
    }

    private static boolean isUnboxingMethod(@NotNull String name) {
        return name.endsWith("Value");
    }

    private static boolean isUnboxing(@NotNull AbstractInsnNode insn) {
        if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
            return false;
        }

        MethodInsnNode methodInsn = (MethodInsnNode) insn;

        return isClassBox(methodInsn.owner) && isUnboxingMethod(methodInsn.name);
    }

    private static boolean isBoxing(@NotNull AbstractInsnNode insn) {
        if (insn.getOpcode() != Opcodes.INVOKESTATIC) {
            return false;
        }

        MethodInsnNode methodInsnNode = (MethodInsnNode) insn;

        return isClassBox(methodInsnNode.owner) && methodInsnNode.name.equals("valueOf");
    }

    @Override
    @Nullable
    public BasicValue newValue(@Nullable Type type) {
        if (type == null) {
            return super.newValue(null);
        }
        if (type.getSort() == Type.OBJECT) {
            return new BasicValue(type);
        }

        return super.newValue(type);
    }

    @Override
    public BasicValue newOperation(@NotNull AbstractInsnNode insn) throws AnalyzerException {
        if (insn.getOpcode() == Opcodes.LDC) {
            Object cst = ((LdcInsnNode) insn).cst;

            if (cst instanceof Long) {
                return BasicValue.LONG_VALUE;
            }
            if (cst instanceof Boolean ||
                cst instanceof Integer ||
                cst instanceof Short ||
                cst instanceof Byte ||
                cst instanceof Character) {
                return BasicValue.INT_VALUE;
            }

            if (cst instanceof Float) {
                return BasicValue.FLOAT_VALUE;
            }

            if (cst instanceof Double) {
                return BasicValue.DOUBLE_VALUE;
            }
        }

        return super.newOperation(insn);
    }

    @Override
    @Nullable
    public BasicValue naryOperation(@NotNull AbstractInsnNode insn, @NotNull List<? extends BasicValue> values) throws AnalyzerException {
        BasicValue value = super.naryOperation(insn, values);

        if (isBoxing(insn)) {
            int index = insnList.indexOf(insn);
            if (!boxingPlaces.containsKey(index)) {
                BoxedBasicValue boxedBasicValue = new BoxedBasicValue(value.getType(), values.get(0).getType(), insn);
                candidatesBoxedValues.add(boxedBasicValue);
                boxingPlaces.put(index, boxedBasicValue);
            }

            return boxingPlaces.get(index);
        }

        if (isUnboxing(insn) &&
            values.get(0) instanceof BoxedBasicValue &&
            value.getType().equals(((BoxedBasicValue) values.get(0)).getBoxedType())) {
            BoxedBasicValue boxedBasicValue = (BoxedBasicValue) values.get(0);
            boxedBasicValue.addInsn(insn);
            boxedBasicValue.setWasUnboxed(true);
        }

        return value;
    }

    @Override
    @Nullable
    public BasicValue unaryOperation(@NotNull AbstractInsnNode insn, @NotNull BasicValue value) throws AnalyzerException {
        if (insn.getOpcode() == Opcodes.CHECKCAST && value instanceof BoxedBasicValue) {
            ((BoxedBasicValue) value).addInsn(insn);
            return value;
        }

        return super.unaryOperation(insn, value);
    }

    @Override
    @NotNull
    public BasicValue copyOperation(@NotNull AbstractInsnNode insn, @NotNull BasicValue value) throws AnalyzerException {
        if (value instanceof BoxedBasicValue) {
            candidatesBoxedValues.remove(value);
            return new BasicValue(value.getType());
        }

        return super.copyOperation(insn, value);
    }

    @Override
    @NotNull
    public BasicValue merge(@NotNull BasicValue v, @NotNull BasicValue w) {
        if (v instanceof BoxedBasicValue && w instanceof BoxedBasicValue && v == w) {
            return v;
        }

        if (v instanceof BoxedBasicValue) {
            candidatesBoxedValues.remove(v);
            v = new BasicValue(v.getType());
        }

        if (w instanceof BoxedBasicValue) {
            candidatesBoxedValues.remove(w);
            w = new BasicValue(w.getType());
        }

        return super.merge(v, w);
    }

    @NotNull
    public List<BoxedBasicValue> getCandidatesBoxedValues() {
        List<BoxedBasicValue> result = new ArrayList<BoxedBasicValue>();

        for (BoxedBasicValue value : candidatesBoxedValues) {
            if (value.wasUnboxed()) {
                result.add(value);
            }
        }

        return result;
    }
}
