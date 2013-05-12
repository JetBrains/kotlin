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

package org.jetbrains.jet.preloading.instrumentation;

import org.jetbrains.asm4.Handle;
import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;

public class MethodVisitorWithUniversalHandler extends MethodVisitor {
    public MethodVisitorWithUniversalHandler(int api) {
        super(api);
    }

    public MethodVisitorWithUniversalHandler(int api, MethodVisitor mv) {
        super(api, mv);
    }

    protected boolean visitAnyInsn(int opcode) {
        return true;
    }
    
    @Override
    public void visitInsn(int opcode) {
        if (!visitAnyInsn(opcode)) return;
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        if (!visitAnyInsn(opcode)) return;
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if (!visitAnyInsn(opcode)) return;
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (!visitAnyInsn(opcode)) return;
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (!visitAnyInsn(opcode)) return;
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if (!visitAnyInsn(opcode)) return;
        super.visitMethodInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        if (!visitAnyInsn(Opcodes.INVOKEDYNAMIC)) return;
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (!visitAnyInsn(opcode)) return;
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (!visitAnyInsn(Opcodes.LDC)) return;
        super.visitLdcInsn(cst);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        if (!visitAnyInsn(Opcodes.IINC)) return;
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        if (!visitAnyInsn(Opcodes.TABLESWITCH)) return;
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        if (!visitAnyInsn(Opcodes.LOOKUPSWITCH)) return;
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        if (!visitAnyInsn(Opcodes.MULTIANEWARRAY)) return;
        super.visitMultiANewArrayInsn(desc, dims);
    }
}
