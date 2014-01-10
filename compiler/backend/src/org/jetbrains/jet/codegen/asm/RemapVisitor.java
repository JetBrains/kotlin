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

import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.commons.InstructionAdapter;

public class RemapVisitor extends InstructionAdapter {

    private final Label end;

    private final VarRemapper remapper;

    private final boolean remapReturn;

    protected RemapVisitor(MethodVisitor mv, Label end, VarRemapper remapper, boolean remapReturn) {
        super(InlineCodegenUtil.API, mv);
        this.end = end;
        this.remapper = remapper;
        this.remapReturn = remapReturn;
    }

    @Override
    public void visitInsn(int opcode) {
        if (remapReturn && opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
            super.visitJumpInsn(Opcodes.GOTO, end);
        }
        else {
            super.visitInsn(opcode);
        }
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(remapper.remap(var), increment);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        int newVar = remapper.remap(var);
        super.visitVarInsn(opcode, newVar);
    }

    @Override
    public void visitLocalVariable(
            String name, String desc, String signature, Label start, Label end, int index
    ) {

    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return null;
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {

    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return null;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (name.equals("$$$this")) {
            super.visitVarInsn(Opcodes.ALOAD, 0);
        } else {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }
}
