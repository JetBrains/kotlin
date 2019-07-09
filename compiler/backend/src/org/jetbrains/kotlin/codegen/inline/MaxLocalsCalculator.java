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
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;

public class MaxLocalsCalculator extends MethodVisitor {

    private int maxLocals;

    public MaxLocalsCalculator(int api, int access, String descriptor, MethodVisitor mv) {
        super(api, mv);

        // updates maxLocals
        int size = Type.getArgumentsAndReturnSizes(descriptor) >> 2;
        if ((access & Opcodes.ACC_STATIC) != 0) {
            --size;
        }

        maxLocals = size;
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        int n;
        if (opcode == Opcodes.LLOAD || opcode == Opcodes.DLOAD ||
            opcode == Opcodes.LSTORE || opcode == Opcodes.DSTORE) {
            n = var + 2;
        }
        else {
            n = var + 1;
        }
        updateMaxLocals(n);

        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        updateMaxLocals(var + 1);

        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitLocalVariable(
            @NotNull String name, @NotNull String desc, String signature, @NotNull Label start, @NotNull Label end, int index
    ) {
        // updates max locals
        char c = desc.charAt(0);
        int n = index + (c == 'J' || c == 'D' ? 2 : 1);
        updateMaxLocals(n);

        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, this.maxLocals);
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    private void updateMaxLocals(int nextFreeSlotNumber) {
        if (nextFreeSlotNumber > maxLocals) {
            maxLocals = nextFreeSlotNumber;
        }
    }
}
