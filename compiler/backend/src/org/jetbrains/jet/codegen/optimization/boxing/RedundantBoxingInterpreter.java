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
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.InsnList;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class RedundantBoxingInterpreter extends BoxingInterpreter {
    private static final ImmutableSet<Integer> unsafeOperationsOpcodes;

    static {
        ImmutableSet.Builder<Integer> unsafeOperationsOpcodesBuilder = ImmutableSet.builder();
        unsafeOperationsOpcodesBuilder.add(
                Opcodes.AASTORE, Opcodes.PUTFIELD, Opcodes.PUTSTATIC, Opcodes.ARETURN
        );

        unsafeOperationsOpcodes = unsafeOperationsOpcodesBuilder.build();
    }

    private final Set<BoxedBasicValue> candidatesBoxedValues = new HashSet<BoxedBasicValue>();

    RedundantBoxingInterpreter(InsnList insnList) {
        super(insnList);
    }

    private void checkIfUnsafeOperationWithBoxed(@Nullable BasicValue value, @NotNull AbstractInsnNode insnNode) {
        if (value instanceof BoxedBasicValue && unsafeOperationsOpcodes.contains(insnNode.getOpcode())) {
            markAsDirty((BoxedBasicValue) value);
        }
    }

    @Override
    public BasicValue binaryOperation(
            @NotNull AbstractInsnNode insn,
            @NotNull BasicValue value1,
            @NotNull BasicValue value2
    ) throws AnalyzerException {

        checkIfUnsafeOperationWithBoxed(value2, insn);

        return super.binaryOperation(insn, value1, value2);
    }

    @Override
    public BasicValue ternaryOperation(
            @NotNull AbstractInsnNode insn,
            @NotNull BasicValue value1, @NotNull BasicValue value2, @NotNull BasicValue value3
    ) throws AnalyzerException {

        checkIfUnsafeOperationWithBoxed(value3, insn);

        return super.ternaryOperation(insn, value1, value2, value3);
    }

    @Override
    protected boolean isAllowedUnaryOperationWithBoxed(int opcode) {
        return opcode == Opcodes.CHECKCAST;
    }

    @Override
    @NotNull
    public BasicValue copyOperation(@NotNull AbstractInsnNode insn, @NotNull BasicValue value) throws AnalyzerException {
        // currently we don't allow any copy operations with boxed values
        if (value instanceof BoxedBasicValue) {
            markAsDirty((BoxedBasicValue) value);
            return new BasicValue(value.getType());
        }

        return super.copyOperation(insn, value);
    }

    @Override
    protected void onNewBoxedValue(BoxedBasicValue value) {
        candidatesBoxedValues.add(value);
    }

    private void markAsDirty(BoxedBasicValue value) {
        candidatesBoxedValues.remove(value);
    }

    @Override
    protected void onMethodCallWithBoxedValue(BoxedBasicValue value) {
        markAsDirty(value);
    }

    @Override
    protected void onMergeFail(BoxedBasicValue v) {
        markAsDirty(v);
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
