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

import org.jetbrains.asm4.*;
import org.jetbrains.asm4.commons.InstructionAdapter;

import java.util.ArrayList;
import java.util.List;

//http://asm.ow2.org/current/asm-transformations.pdf
public class InliningAdapter extends InstructionAdapter {

    private final Label end;
    protected final VarRemapper remapper;
    private int nextLocalIndex = 0;

    public InliningAdapter(MethodVisitor mv, int api, String desc, Label end, int localsStart, VarRemapper remapper) {
        super(api, mv);
        this.end = end;
        this.remapper = remapper;
        nextLocalIndex = localsStart;
    }

    public void visitInsn(int opcode) {
        if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
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
        int size = newVar + (opcode == Opcodes.DSTORE || opcode == Opcodes.LSTORE ? 2 : 1);
        if (size > nextLocalIndex) {
            nextLocalIndex = size;
        }
    }

    public int getNextLocalIndex() {
        return nextLocalIndex;
    }

    @Override
    public void visitLocalVariable(
            String name, String desc, String signature, Label start, Label end, int index
    ) {
        //super.visitLocalVariable(name, desc, signature, start, end, index);
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

}