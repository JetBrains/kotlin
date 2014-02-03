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
import org.jetbrains.asm4.commons.InstructionAdapter;

public class ShiftAdapter extends InstructionAdapter {

    private int shift;

    public ShiftAdapter(MethodVisitor mv, int shift) {
        super(mv);
        this.shift = shift;
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var + shift, increment);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var + shift);
    }
}
