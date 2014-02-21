/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;

public abstract class VarRemapper {

    public static class ParamRemapper extends VarRemapper {

        private final int allParamsSize;
        private final Parameters params;
        private final int actualParamsSize;

        private final StackValue [] remapIndex;

        private int additionalShift;

        public ParamRemapper(Parameters params, int additionalShift) {
            this.additionalShift = additionalShift;
            this.allParamsSize = params.totalSize();
            this.params = params;

            int realSize = 0;
            remapIndex = new StackValue [params.totalSize()];

            int index = 0;
            for (ParameterInfo info : params) {
                if (!info.isSkippedOrRemapped()) {
                    remapIndex[index] = StackValue.local(realSize, AsmTypeConstants.OBJECT_TYPE);
                    realSize += info.getType().getSize();
                } else {
                    remapIndex[index] = info.isRemapped() ? info.getRemapIndex() : null;
                }
                index++;
            }

            actualParamsSize = realSize;
        }

        @Override
        public RemapInfo doRemap(int index) {
            int remappedIndex;

            if (index < allParamsSize) {
                ParameterInfo info = params.get(index);
                StackValue remapped = remapIndex[index];
                if (info.isSkipped || remapped == null) {
                    throw new RuntimeException("Trying to access skipped parameter: " + info.type + " at " +index);
                }
                if (info.isRemapped()) {
                    return new RemapInfo(remapped, info);
                } else {
                    remappedIndex = ((StackValue.Local)remapped).index;
                }
            } else {
                remappedIndex = actualParamsSize - params.totalSize() + index; //captured params not used directly in this inlined method, they used in closure
            }

            return new RemapInfo(StackValue.local(remappedIndex + additionalShift, AsmTypeConstants.OBJECT_TYPE), null);
        }
    }

    public RemapInfo remap(int index) {
        return doRemap(index);
    }

    public void visitIincInsn(int var, int increment, MethodVisitor mv) {
        RemapInfo remap = remap(var);
        assert remap.value instanceof StackValue.Local;
        mv.visitIincInsn(((StackValue.Local) remap.value).index, increment);
    }

    public void visitVarInsn(int opcode, int var, InstructionAdapter mv) {
        RemapInfo remapInfo = remap(var);
        StackValue value = remapInfo.value;
        if (value instanceof StackValue.Local) {
            if (remapInfo.parameterInfo != null) {
                opcode = value.type.getOpcode(Opcodes.ILOAD);
            }
            mv.visitVarInsn(opcode, ((StackValue.Local) value).index);
            if (remapInfo.parameterInfo != null) {
                value.coerce(value.type, remapInfo.parameterInfo.type, mv);
            }
        } else {
            value.put(remapInfo.parameterInfo.type, mv);
        }
    }

    abstract public RemapInfo doRemap(int index);

    public static class RemapInfo {

        public final StackValue value;

        public final ParameterInfo parameterInfo;

        public RemapInfo(@NotNull StackValue value, @Nullable ParameterInfo info) {
            this.value = value;
            parameterInfo = info;
        }
    }
}
