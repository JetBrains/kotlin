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

package org.jetbrains.jet.codegen.asm;

import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.tree.FieldInsnNode;
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
        public StackValue doRemap(int index) {
            int remappedIndex;

            if (index < allParamsSize) {
                ParameterInfo info = params.get(index);
                StackValue remapped = remapIndex[index];
                if (info.isSkipped || remapped == null) {
                    throw new RuntimeException("Trying to access skipped parameter: " + info.type + " at " +index);
                }
                if (info.isRemapped()) {
                    return remapped;
                } else {
                    remappedIndex = ((StackValue.Local)remapped).index;
                }
            } else {
                remappedIndex = actualParamsSize - params.totalSize() + index; //captured params not used directly in this inlined method, they used in closure
            }


            return StackValue.local(remappedIndex + additionalShift, AsmTypeConstants.OBJECT_TYPE);
        }
    }

    public StackValue remap(int index) {
        return doRemap(index);
    }

    public void visitIincInsn(int var, int increment, MethodVisitor mv) {
        StackValue remap = remap(var);
        assert remap instanceof StackValue.Local;
        mv.visitIincInsn(((StackValue.Local) remap).index, increment);
    }

    public void visitVarInsn(int opcode, int var, InstructionAdapter mv) {
        StackValue remap = remap(var);
        if (remap instanceof StackValue.Local) {
            mv.visitVarInsn(opcode, ((StackValue.Local) remap).index);
        } else {
            //Type stub = Type.getObjectType("STUB");
            //String descriptor = stub.getDescriptor();
            //
            //mv.visitFieldInsn(Opcodes.GETSTATIC, stub.getInternalName(), "$$$this$skip", descriptor);
            remap.put(remap.type, mv);
        }
    }

    abstract public StackValue doRemap(int index);
}
