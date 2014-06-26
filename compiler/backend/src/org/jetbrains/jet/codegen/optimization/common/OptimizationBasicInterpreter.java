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

package org.jetbrains.jet.codegen.optimization.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

public class OptimizationBasicInterpreter extends BasicInterpreter {
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
}
