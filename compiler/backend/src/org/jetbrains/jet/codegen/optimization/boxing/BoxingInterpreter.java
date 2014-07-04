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
    private static boolean isProgressionClass(String internalName) {
        return internalName.startsWith("kotlin/") && (
                internalName.endsWith("Progression") ||
                internalName.endsWith("Range")
        );
    }

    /**
     * e.g. for "kotlin/IntRange" it returns "Int"
     *
     * @param progressionClassInternalName
     * @return
     */
    private static String getValuesTypePartOfProgressionClass(String progressionClassInternalName) {
        progressionClassInternalName = progressionClassInternalName.substring("kotlin/".length());

        int cutAtTheEnd = (progressionClassInternalName.endsWith("Progression")) ? "Progression".length() : "Range".length();
        return progressionClassInternalName.substring(0, progressionClassInternalName.length() - cutAtTheEnd);
    }

    private static final ImmutableSet<String> wrappersClassNames;

    static {
        ImmutableSet.Builder<String> wrappersClassesBuilder = ImmutableSet.builder();

        for (JvmPrimitiveType primitiveType : JvmPrimitiveType.values()) {
            wrappersClassesBuilder.add(AsmUtil.internalNameByFqNameWithoutInnerClasses(primitiveType.getWrapperFqName()));
        }

        wrappersClassNames = wrappersClassesBuilder.build();
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

    private static boolean isIteratorMethodCallOfProgression(
            @NotNull AbstractInsnNode insn, @NotNull List<? extends BasicValue> values
    ) {
        return (insn.getOpcode() == Opcodes.INVOKEINTERFACE &&
                values.get(0).getType() != null &&
                isProgressionClass(values.get(0).getType().getInternalName()) &&
                ((MethodInsnNode) insn).name.equals("iterator"));
    }

    private static boolean isNextMethodCallOfProgressionIterator(
            @NotNull AbstractInsnNode insn, @NotNull List<? extends BasicValue> values
    ) {
        return (insn.getOpcode() == Opcodes.INVOKEINTERFACE &&
                values.get(0) instanceof RangeIteratorBasicValue &&
                ((MethodInsnNode) insn).name.equals("next"));
    }

    private final Map<Integer, BoxedBasicValue> boxingPlaces = new HashMap<Integer, BoxedBasicValue>();
    private final InsnList insnList;

    BoxingInterpreter(InsnList insnList) {
        this.insnList = insnList;
    }

    @NotNull
    private BoxedBasicValue createNewBoxing(
            @NotNull AbstractInsnNode insn, @NotNull Type type, @Nullable RangeIteratorBasicValue numberIterator
    ) {
        int index = insnList.indexOf(insn);
        if (!boxingPlaces.containsKey(index)) {
            BoxedBasicValue boxedBasicValue = new BoxedBasicValue(type, insn, numberIterator);
            onNewBoxedValue(boxedBasicValue);
            boxingPlaces.put(index, boxedBasicValue);
        }

        return boxingPlaces.get(index);
    }

    @Override
    @Nullable
    public BasicValue naryOperation(@NotNull AbstractInsnNode insn, @NotNull List<? extends BasicValue> values) throws AnalyzerException {
        BasicValue value = super.naryOperation(insn, values);

        if (isBoxing(insn)) {
            return createNewBoxing(insn, value.getType(), null);
        }
        else if (isUnboxing(insn) &&
                 values.get(0) instanceof BoxedBasicValue &&
                 value.getType().equals(((BoxedBasicValue) values.get(0)).getPrimitiveType())) {
            onUnboxing((BoxedBasicValue) values.get(0), insn);
        }
        else if (isIteratorMethodCallOfProgression(insn, values)) {
            return new RangeIteratorBasicValue(
                    getValuesTypePartOfProgressionClass(values.get(0).getType().getInternalName())
            );
        }
        else if (isNextMethodCallOfProgressionIterator(insn, values)) {
            RangeIteratorBasicValue numberIterator = (RangeIteratorBasicValue) values.get(0);
            return createNewBoxing(
                    insn,
                    AsmUtil.boxType(numberIterator.getValuesPrimitiveType()),
                    numberIterator
            );
        }
        else {
            for (BasicValue arg : values) {
                if (arg instanceof BoxedBasicValue) {
                    onMethodCallWithBoxedValue((BoxedBasicValue) arg);
                }
            }
        }

        return value;
    }

    private static boolean isExactValue(@NotNull BasicValue value) {
        return value instanceof RangeIteratorBasicValue ||
               value instanceof BoxedBasicValue ||
               (value.getType() != null && isProgressionClass(value.getType().getInternalName()));
    }

    @Override
    public BasicValue unaryOperation(@NotNull AbstractInsnNode insn, @NotNull BasicValue value) throws AnalyzerException {
        if (insn.getOpcode() == Opcodes.CHECKCAST && isExactValue(value)) {
            return value;
        }

        return super.unaryOperation(insn, value);
    }

    @Override
    @NotNull
    public BasicValue merge(@NotNull BasicValue v, @NotNull BasicValue w) {
        if (v instanceof BoxedBasicValue && ((BoxedBasicValue) v).typeEquals(w)) {
            onMergeSuccess((BoxedBasicValue) v, (BoxedBasicValue) w);
            return v;
        }

        if (v instanceof BoxedBasicValue && w != BasicValue.UNINITIALIZED_VALUE) {
            onMergeFail((BoxedBasicValue) v);
            v = new BasicValue(v.getType());
        }

        if (w instanceof BoxedBasicValue && v != BasicValue.UNINITIALIZED_VALUE) {
            onMergeFail((BoxedBasicValue) w);
            w = new BasicValue(w.getType());
        }

        return super.merge(v, w);
    }

    protected void onNewBoxedValue(@NotNull BoxedBasicValue value) {

    }

    protected void onUnboxing(@NotNull BoxedBasicValue value, @NotNull AbstractInsnNode insn) {

    }

    protected void onMethodCallWithBoxedValue(@NotNull BoxedBasicValue value) {

    }

    protected void onMergeFail(@NotNull BoxedBasicValue value) {

    }

    protected void onMergeSuccess(@NotNull BoxedBasicValue v, @NotNull BoxedBasicValue w) {

    }
}
