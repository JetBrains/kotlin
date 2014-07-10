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
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.InsnList;
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode;
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

class RedundantBoxingInterpreter extends BoxingInterpreter {
    private static final ImmutableSet<Integer> UNSAFE_OPERATIONS_OPCODES;

    static {
        ImmutableSet.Builder<Integer> unsafeOperationsOpcodesBuilder = ImmutableSet.builder();
        unsafeOperationsOpcodesBuilder.add(
                Opcodes.AASTORE, Opcodes.PUTFIELD, Opcodes.PUTSTATIC, Opcodes.ARETURN,
                Opcodes.IFNONNULL, Opcodes.IFNULL, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE
        );

        UNSAFE_OPERATIONS_OPCODES = unsafeOperationsOpcodesBuilder.build();
    }

    private final RedundantBoxedValuesCollection values = new RedundantBoxedValuesCollection();

    public RedundantBoxingInterpreter(InsnList insnList) {
        super(insnList);
    }

    @Override
    public BasicValue binaryOperation(
            @NotNull AbstractInsnNode insn,
            @NotNull BasicValue value1,
            @NotNull BasicValue value2
    ) throws AnalyzerException {

        processOperationWithBoxedValue(value1, insn);
        processOperationWithBoxedValue(value2, insn);

        return super.binaryOperation(insn, value1, value2);
    }

    @Override
    public BasicValue ternaryOperation(
            @NotNull AbstractInsnNode insn,
            @NotNull BasicValue value1, @NotNull BasicValue value2, @NotNull BasicValue value3
    ) throws AnalyzerException {

        // in a valid code only aastore could happen with boxed value
        processOperationWithBoxedValue(value3, insn);

        return super.ternaryOperation(insn, value1, value2, value3);
    }

    @Nullable
    @Override
    public BasicValue unaryOperation(
            @NotNull AbstractInsnNode insn, @NotNull BasicValue value
    ) throws AnalyzerException {

        if (insn.getOpcode() == Opcodes.CHECKCAST && value instanceof BoxedBasicValue) {
            TypeInsnNode typeInsn = (TypeInsnNode) insn;

            if (!isSafeCast((BoxedBasicValue) value, typeInsn.desc)) {
                markValueAsDirty((BoxedBasicValue) value);
            }
        }

        processOperationWithBoxedValue(value, insn);

        return super.unaryOperation(insn, value);
    }

    private static boolean isSafeCast(@NotNull BoxedBasicValue value, @NotNull String descTo) {
        try {
            return Class.forName(Type.getObjectType(descTo).getClassName()).isAssignableFrom(
                    Class.forName(value.getType().getClassName())
            );
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    @NotNull
    public BasicValue copyOperation(@NotNull AbstractInsnNode insn, @NotNull BasicValue value) throws AnalyzerException {
        if (value instanceof BoxedBasicValue && insn.getOpcode() == Opcodes.ASTORE) {
            ((BoxedBasicValue) value).addVariableIndex(((VarInsnNode) insn).var);
        }

        processOperationWithBoxedValue(value, insn);

        return super.copyOperation(insn, value);
    }


    @Override
    protected void onNewBoxedValue(@NotNull BoxedBasicValue value) {
        values.add(value);
    }

    @Override
    protected void onUnboxing(
            @NotNull AbstractInsnNode insn, @NotNull BoxedBasicValue value, @NotNull Type resultType
    ) {
        if (value.getPrimitiveType().equals(resultType)) {
            addAssociatedInsn(value, insn);
        }
        else {
            value.addUnboxingWithCastTo(insn, resultType);
        }
    }

    @Override
    protected void onMethodCallWithBoxedValue(@NotNull BoxedBasicValue value) {
        markValueAsDirty(value);
    }

    @Override
    protected void onMergeFail(@NotNull BoxedBasicValue v) {
        markValueAsDirty(v);
    }

    @Override
    protected void onMergeSuccess(
            @NotNull BoxedBasicValue v, @NotNull BoxedBasicValue w
    ) {
        values.merge(v, w);
    }

    private void processOperationWithBoxedValue(@Nullable BasicValue value, @NotNull AbstractInsnNode insnNode) {
        if (value instanceof BoxedBasicValue) {
            if (UNSAFE_OPERATIONS_OPCODES.contains(insnNode.getOpcode())) {
                markValueAsDirty((BoxedBasicValue) value);
            }
            else {
                addAssociatedInsn((BoxedBasicValue) value, insnNode);
            }
        }
    }

    private void markValueAsDirty(@NotNull BoxedBasicValue value) {
        values.remove(value);
    }

    private static void addAssociatedInsn(@NotNull BoxedBasicValue value, @NotNull AbstractInsnNode insn) {
        if (value.isSafeToRemove()) {
            value.addInsn(insn);
        }
    }

    @NotNull
    public RedundantBoxedValuesCollection getCandidatesBoxedValues() {
        return values;
    }
}
