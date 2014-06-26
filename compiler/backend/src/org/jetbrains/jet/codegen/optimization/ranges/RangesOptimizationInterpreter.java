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

package org.jetbrains.jet.codegen.optimization.ranges;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.optimization.common.OptimizationBasicInterpreter;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import java.util.List;

public class RangesOptimizationInterpreter extends OptimizationBasicInterpreter {
    private static final Type kotlinIteratorType = Type.getObjectType("kotlin/Iterator");

    private static boolean isProgressionClass(String internalName) {
        return internalName.startsWith("kotlin/") && (
                internalName.endsWith("Progression") ||
                internalName.endsWith("Range")
        );
    }

    private static String getValuesTypePrefix(String progressionClassInternalName) {
        progressionClassInternalName = progressionClassInternalName.substring("kotlin/".length());

        int cutAtTheEnd = (progressionClassInternalName.endsWith("Progression")) ? "Progression".length() : "Range".length();
        return progressionClassInternalName.substring(0, progressionClassInternalName.length() - cutAtTheEnd);
    }

    @Override
    @Nullable
    public BasicValue naryOperation(@NotNull AbstractInsnNode insn, @NotNull List<? extends BasicValue> values) throws AnalyzerException {
        BasicValue value = super.naryOperation(insn, values);

        int opcode = insn.getOpcode();

        if (opcode == Opcodes.INVOKEINTERFACE &&
            values.get(0).getType() != null &&
            isProgressionClass(values.get(0).getType().getInternalName()) &&
            ((MethodInsnNode) insn).name.equals("iterator")) {

            String valueTypePrefix = getValuesTypePrefix(values.get(0).getType().getInternalName());
            return new RangeIteratorBasicValue(valueTypePrefix);
        }

        return value;
    }

    private static boolean isExactValue(@NotNull BasicValue value) {
        return value instanceof RangeIteratorBasicValue || (value.getType() != null && isProgressionClass(value.getType().getInternalName()));
    }

    @Override
    public BasicValue unaryOperation(@NotNull AbstractInsnNode insn, @NotNull BasicValue value) throws AnalyzerException {
        if (insn.getOpcode() == Opcodes.CHECKCAST && isExactValue(value)) {
            return value;
        }

        return super.unaryOperation(insn, value);
    }

    @NotNull
    @Override
    public BasicValue merge(@NotNull BasicValue v, @NotNull BasicValue w) {
        if (v instanceof RangeIteratorBasicValue && w instanceof RangeIteratorBasicValue && v.equals(w)) {
            return v;
        }

        if (v instanceof RangeIteratorBasicValue) {
            v = new BasicValue(kotlinIteratorType);
        }

        if (w instanceof RangeIteratorBasicValue) {
            w = new BasicValue(kotlinIteratorType);
        }

        return super.merge(v, w);
    }
}
