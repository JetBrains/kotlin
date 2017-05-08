/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import static org.jetbrains.kotlin.codegen.inline.LocalVarRemapper.RemapStatus.*;

public class LocalVarRemapper {
    private final Parameters params;
    private final int actualParamsSize;
    private final StackValue[] remapValues;
    private final int additionalShift;

    public LocalVarRemapper(@NotNull Parameters params, int additionalShift) {
        this.additionalShift = additionalShift;
        this.params = params;

        remapValues = new StackValue[params.getArgsSizeOnStack()];

        int realSize = 0;
        for (ParameterInfo info : params) {
            Integer shift = params.getDeclarationSlot(info);
            if (!info.isSkippedOrRemapped()) {
                remapValues[shift] = StackValue.local(realSize, AsmTypes.OBJECT_TYPE);
                realSize += info.getType().getSize();
            }
            else {
                remapValues[shift] = info.isRemapped() ? info.getRemapValue() : null;
                if (CapturedParamInfo.isSynthetic(info)) {
                    realSize += info.getType().getSize();
                }
            }
        }

        actualParamsSize = realSize;
    }

    @NotNull
    private RemapInfo doRemap(int index) {
        int remappedIndex;

        if (index < params.getArgsSizeOnStack()) {
            ParameterInfo info = params.getParameterByDeclarationSlot(index);
            StackValue remapped = remapValues[index];
            if (info.isSkipped || remapped == null) {
                return new RemapInfo(info);
            }
            if (info.isRemapped()) {
                return new RemapInfo(remapped, info, REMAPPED);
            }
            else {
                remappedIndex = ((StackValue.Local) remapped).index;
            }
        }
        else {
            //captured params are not used directly in this inlined method, they are used in closure
            //except captured ones for default lambdas, they are generated in default body
            remappedIndex = actualParamsSize - params.getArgsSizeOnStack() + index;
        }

        return new RemapInfo(StackValue.local(remappedIndex + additionalShift, AsmTypes.OBJECT_TYPE), null, SHIFT);
    }

    @NotNull
    public RemapInfo remap(int index) {
        RemapInfo info = doRemap(index);
        if (FAIL == info.status) {
            assert info.parameterInfo != null : "Parameter info should be not null";
            throw new RuntimeException("Trying to access skipped parameter: " + info.parameterInfo.type + " at " +index);
        }
        return info;
    }

    public void visitIincInsn(int var, int increment, @NotNull MethodVisitor mv) {
        RemapInfo remap = remap(var);
        assert remap.value instanceof StackValue.Local : "Remapped value should be a local: " + remap.value;
        mv.visitIincInsn(((StackValue.Local) remap.value).index, increment);
    }

    public void visitLocalVariable(
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @NotNull Label start,
            @NotNull Label end,
            int index,
            MethodVisitor mv
    ) {
        RemapInfo info = doRemap(index);
        //add entries only for shifted vars
        if (SHIFT == info.status) {
            mv.visitLocalVariable(name, desc, signature, start, end, ((StackValue.Local) info.value).index);
        }
    }

    public void visitVarInsn(int opcode, int var, @NotNull InstructionAdapter mv) {
        RemapInfo remapInfo = remap(var);
        StackValue value = remapInfo.value;
        if (value instanceof StackValue.Local) {
            boolean isStore = InlineCodegenUtil.isStoreInstruction(opcode);
            if (remapInfo.parameterInfo != null) {
                //All remapped value parameters can't be rewritten except case of default ones.
                //On remapping default parameter to actual value there is only one instruction that writes to it according to mask value
                //but if such parameter remapped then it passed and this mask branch code never executed
                //TODO add assertion about parameter default value: descriptor is required
                opcode = value.type.getOpcode(isStore ? Opcodes.ISTORE : Opcodes.ILOAD);
            }
            mv.visitVarInsn(opcode, ((StackValue.Local) value).index);
            if (remapInfo.parameterInfo != null && !isStore) {
                StackValue.coerce(value.type, remapInfo.parameterInfo.type, mv);
            }
        }
        else {
            assert remapInfo.parameterInfo != null : "Non local value should have parameter info";
            value.put(remapInfo.parameterInfo.type, mv);
        }
    }

    public enum RemapStatus {
        SHIFT,
        REMAPPED,
        FAIL
    }

    public static class RemapInfo {
        public final StackValue value;
        public final ParameterInfo parameterInfo;
        public final RemapStatus status;

        public RemapInfo(@NotNull StackValue value, @Nullable ParameterInfo parameterInfo, @NotNull RemapStatus remapStatus) {
            this.value = value;
            this.parameterInfo = parameterInfo;
            this.status = remapStatus;
        }

        public RemapInfo(@NotNull ParameterInfo parameterInfo) {
            this.value = null;
            this.parameterInfo = parameterInfo;
            this.status = FAIL;
        }
    }
}
