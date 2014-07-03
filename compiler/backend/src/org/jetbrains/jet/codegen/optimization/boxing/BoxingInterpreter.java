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

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.jet.codegen.optimization.common.OptimizationBasicInterpreter;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.InsnList;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoxingInterpreter extends OptimizationBasicInterpreter {
    private static final ImmutableSet<String> wrappersClassNames;

    static {
        ImmutableSet.Builder<String> wrappersClassesBuilder = ImmutableSet.builder();

        for (JvmPrimitiveType primitiveType : JvmPrimitiveType.values()) {
            wrappersClassesBuilder.add(AsmUtil.internalNameByFqNameWithoutInnerClasses(primitiveType.getWrapperFqName()));
        }

        wrappersClassNames = wrappersClassesBuilder.build();
    }

    private final Map<Integer, BoxedBasicValue> boxingPlaces = new HashMap<Integer, BoxedBasicValue>();
    private final InsnList insnList;

    BoxingInterpreter(InsnList insnList) {
        this.insnList = insnList;
    }

    private static boolean isWrapperClassName(@NotNull String owner) {
        return wrappersClassNames.contains(owner);
    }

    private static boolean isWrapperClassNameOrNumber(@NotNull String owner) {
        return isWrapperClassName(owner) || owner.equals(Type.getInternalName(Number.class));
    }

    private static boolean isUnboxingMethodName(@NotNull String name) {
        return name.endsWith("Value");
    }

    private static boolean isUnboxing(@NotNull AbstractInsnNode insn) {
        if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
            return false;
        }

        MethodInsnNode methodInsn = (MethodInsnNode) insn;

        return isWrapperClassNameOrNumber(methodInsn.owner) && isUnboxingMethodName(methodInsn.name);
    }

    private static boolean isBoxing(@NotNull AbstractInsnNode insn) {
        if (insn.getOpcode() != Opcodes.INVOKESTATIC) {
            return false;
        }

        MethodInsnNode methodInsnNode = (MethodInsnNode) insn;

        return isWrapperClassName(methodInsnNode.owner) && methodInsnNode.name.equals("valueOf");
    }

    @Override
    @Nullable
    public BasicValue naryOperation(@NotNull AbstractInsnNode insn, @NotNull List<? extends BasicValue> values) throws AnalyzerException {
        BasicValue value = super.naryOperation(insn, values);

        if (isBoxing(insn)) {
            int index = insnList.indexOf(insn);
            if (!boxingPlaces.containsKey(index)) {
                BoxedBasicValue boxedBasicValue = new BoxedBasicValue(values.get(0).getType(), insn);
                onNewBoxedValue(boxedBasicValue);
                boxingPlaces.put(index, boxedBasicValue);
            }

            return boxingPlaces.get(index);
        } else if (isUnboxing(insn) &&
            values.get(0) instanceof BoxedBasicValue &&
            value.getType().equals(((BoxedBasicValue) values.get(0)).getPrimitiveType())) {

            BoxedBasicValue boxedBasicValue = (BoxedBasicValue) values.get(0);
            boxedBasicValue.addInsn(insn);
            boxedBasicValue.setWasUnboxed(true);
        } else {
            for (BasicValue arg : values) {
                if (arg instanceof BoxedBasicValue) {
                    onMethodCallWithBoxedValue((BoxedBasicValue) arg);
                }
            }
        }

        return value;
    }


    @Override
    @Nullable
    public BasicValue unaryOperation(@NotNull AbstractInsnNode insn, @NotNull BasicValue value) throws AnalyzerException {
        if (isAllowedUnaryOperationWithBoxed(insn.getOpcode()) && value instanceof BoxedBasicValue) {
            ((BoxedBasicValue) value).addInsn(insn);
            return value;
        }

        return super.unaryOperation(insn, value);
    }

    @Override
    @NotNull
    public BasicValue merge(@NotNull BasicValue v, @NotNull BasicValue w) {
        if (v instanceof BoxedBasicValue && v.equals(w)) {
            return v;
        }

        if (v instanceof BoxedBasicValue) {
            onMergeFail((BoxedBasicValue) v);
            v = new BasicValue(v.getType());
        }

        if (w instanceof BoxedBasicValue) {
            onMergeFail((BoxedBasicValue) w);
            w = new BasicValue(w.getType());
        }

        return super.merge(v, w);
    }


    protected void onNewBoxedValue(BoxedBasicValue value) {

    }

    protected void onMethodCallWithBoxedValue(BoxedBasicValue value) {

    }

    protected void onMergeFail(BoxedBasicValue value) {

    }

    protected boolean isAllowedUnaryOperationWithBoxed(int opcode) {
        return opcode == Opcodes.CHECKCAST || opcode == Opcodes.IFNULL;
    }
}
