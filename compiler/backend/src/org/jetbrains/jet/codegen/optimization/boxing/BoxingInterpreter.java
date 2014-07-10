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
import org.jetbrains.jet.codegen.RangeCodegenUtil;
import org.jetbrains.jet.codegen.optimization.common.OptimizationBasicInterpreter;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
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
    private static final ImmutableSet<String> UNBOXING_METHOD_NAMES;

    static {
        UNBOXING_METHOD_NAMES = ImmutableSet.of(
                "booleanValue", "charValue", "byteValue", "shortValue", "intValue", "floatValue", "longValue", "doubleValue"
        );
    }


    private final Map<Integer, BoxedBasicValue> boxingPlaces = new HashMap<Integer, BoxedBasicValue>();
    private final InsnList insnList;

    public BoxingInterpreter(InsnList insnList) {
        this.insnList = insnList;
    }

    @NotNull
    private BoxedBasicValue createNewBoxing(
            @NotNull AbstractInsnNode insn, @NotNull Type type,
            @Nullable ProgressionIteratorBasicValue progressionIterator
    ) {
        int index = insnList.indexOf(insn);
        if (!boxingPlaces.containsKey(index)) {
            BoxedBasicValue boxedBasicValue = new BoxedBasicValue(type, insn, progressionIterator);
            onNewBoxedValue(boxedBasicValue);
            boxingPlaces.put(index, boxedBasicValue);
        }

        return boxingPlaces.get(index);
    }

    @Override
    @Nullable
    public BasicValue naryOperation(@NotNull AbstractInsnNode insn, @NotNull List<? extends BasicValue> values) throws AnalyzerException {
        BasicValue value = super.naryOperation(insn, values);

        if (values.isEmpty()) return value;

        BasicValue firstArg = values.get(0);

        if (isBoxing(insn)) {
            return createNewBoxing(insn, value.getType(), null);
        }
        else if (isUnboxing(insn) &&
                 firstArg instanceof BoxedBasicValue) {
            onUnboxing(insn, (BoxedBasicValue) firstArg, value.getType());
        }
        else if (isIteratorMethodCallOfProgression(insn, values)) {
            return new ProgressionIteratorBasicValue(
                    getValuesTypeOfProgressionClass(firstArg.getType().getInternalName())
            );
        }
        else if (isNextMethodCallOfProgressionIterator(insn, values)) {
            assert firstArg instanceof ProgressionIteratorBasicValue : "firstArg should be progression iterator";

            ProgressionIteratorBasicValue progressionIterator = (ProgressionIteratorBasicValue) firstArg;
            return createNewBoxing(
                    insn,
                    AsmUtil.boxType(progressionIterator.getValuesPrimitiveType()),
                    progressionIterator
            );
        }
        else {
            // nary operation should be a method call or multinewarray
            // arguments for multinewarray could be only numeric
            // so if there are boxed values in args, it's not a case of multinewarray
            for (BasicValue arg : values) {
                if (arg instanceof BoxedBasicValue) {
                    onMethodCallWithBoxedValue((BoxedBasicValue) arg);
                }
            }
        }

        return value;
    }

    private static boolean isWrapperClassNameOrNumber(@NotNull String internalClassName) {
        return isWrapperClassName(internalClassName) || internalClassName.equals(Type.getInternalName(Number.class));
    }

    private static boolean isWrapperClassName(@NotNull String internalClassName) {
        return JvmPrimitiveType.isWrapperClassName(
                buildFqNameByInternal(internalClassName)
        );
    }

    @NotNull
    private static FqName buildFqNameByInternal(@NotNull String internalClassName) {
        return new FqName(Type.getObjectType(internalClassName).getClassName());
    }

    private static boolean isUnboxing(@NotNull AbstractInsnNode insn) {
        if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) return false;

        MethodInsnNode methodInsn = (MethodInsnNode) insn;

        return isWrapperClassNameOrNumber(methodInsn.owner) && isUnboxingMethodName(methodInsn.name);
    }

    private static boolean isUnboxingMethodName(@NotNull String name) {
        return UNBOXING_METHOD_NAMES.contains(name);
    }

    private static boolean isBoxing(@NotNull AbstractInsnNode insn) {
        if (insn.getOpcode() != Opcodes.INVOKESTATIC) return false;

        MethodInsnNode methodInsnNode = (MethodInsnNode) insn;

        return isWrapperClassName(methodInsnNode.owner) && "valueOf".equals(methodInsnNode.name);
    }

    private static boolean isNextMethodCallOfProgressionIterator(
            @NotNull AbstractInsnNode insn, @NotNull List<? extends BasicValue> values
    ) {
        return (insn.getOpcode() == Opcodes.INVOKEINTERFACE &&
                values.get(0) instanceof ProgressionIteratorBasicValue &&
                "next".equals(((MethodInsnNode) insn).name));
    }

    private static boolean isIteratorMethodCallOfProgression(
            @NotNull AbstractInsnNode insn, @NotNull List<? extends BasicValue> values
    ) {
        return (insn.getOpcode() == Opcodes.INVOKEINTERFACE &&
                values.get(0).getType() != null &&
                isProgressionClass(values.get(0).getType().getInternalName()) &&
                "iterator".equals(((MethodInsnNode) insn).name));
    }

    private static boolean isProgressionClass(String internalClassName) {
        return RangeCodegenUtil.isRangeOrProgression(buildFqNameByInternal(internalClassName));
    }

    /**
     * e.g. for "kotlin/IntRange" it returns "Int"
     *
     * @param progressionClassInternalName
     * @return
     * @throws java.lang.AssertionError if progressionClassInternalName is not progression class internal name
     */
    @NotNull
    private static String getValuesTypeOfProgressionClass(String progressionClassInternalName) {
        PrimitiveType type = RangeCodegenUtil.getPrimitiveRangeOrProgressionElementType(
                buildFqNameByInternal(progressionClassInternalName)
        );

        assert type != null : "type should be not null";

        return type.getTypeName().asString();
    }

    @Override
    public BasicValue unaryOperation(@NotNull AbstractInsnNode insn, @NotNull BasicValue value) throws AnalyzerException {
        if (insn.getOpcode() == Opcodes.CHECKCAST && isExactValue(value)) {
            return value;
        }

        return super.unaryOperation(insn, value);
    }

    private static boolean isExactValue(@NotNull BasicValue value) {
        return value instanceof ProgressionIteratorBasicValue ||
               value instanceof BoxedBasicValue ||
               (value.getType() != null && isProgressionClass(value.getType().getInternalName()));
    }

    @Override
    @NotNull
    public BasicValue merge(@NotNull BasicValue v, @NotNull BasicValue w) {
        if (v instanceof BoxedBasicValue && ((BoxedBasicValue) v).typeEquals(w)) {
            onMergeSuccess((BoxedBasicValue) v, (BoxedBasicValue) w);
            return v;
        }

        if (v instanceof BoxedBasicValue && w == BasicValue.UNINITIALIZED_VALUE) {
            return v;
        }

        if (w instanceof BoxedBasicValue && v == BasicValue.UNINITIALIZED_VALUE) {
            return w;
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

    protected void onNewBoxedValue(@NotNull BoxedBasicValue value) {

    }

    protected void onUnboxing(@NotNull AbstractInsnNode insn, @NotNull BoxedBasicValue value, @NotNull Type resultType) {

    }

    protected void onMethodCallWithBoxedValue(@NotNull BoxedBasicValue value) {

    }

    protected void onMergeFail(@NotNull BoxedBasicValue value) {

    }

    protected void onMergeSuccess(@NotNull BoxedBasicValue v, @NotNull BoxedBasicValue w) {

    }
}
