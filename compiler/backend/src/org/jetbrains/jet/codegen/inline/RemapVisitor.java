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

import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.tree.FieldInsnNode;
import org.jetbrains.jet.codegen.StackValue;

public class RemapVisitor extends InstructionAdapter {

    private final Label end;

    private final VarRemapper remapper;

    private final boolean remapReturn;
    private FieldRemapper nodeRemapper;

    protected RemapVisitor(MethodVisitor mv, Label end, VarRemapper.ParamRemapper remapper, boolean remapReturn, FieldRemapper nodeRemapper) {
        super(InlineCodegenUtil.API, mv);
        this.end = end;
        this.remapper = remapper;
        this.remapReturn = remapReturn;
        this.nodeRemapper = nodeRemapper;
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
        remapper.visitIincInsn(var, increment, mv);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        remapper.visitVarInsn(opcode, var, new InstructionAdapter(mv));
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (name.startsWith("$$$")) {
            if (nodeRemapper instanceof RegeneratedLambdaFieldRemapper || nodeRemapper.isRoot()) {
                FieldInsnNode fin = new FieldInsnNode(opcode, owner, name, desc);
                StackValue inline = nodeRemapper.getFieldForInline(fin, null);
                assert inline != null : "Captured field should have not null stackValue " + fin;
                inline.put(inline.type, this);
            }
            else {
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        }
        else {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
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

    //TODO not skip for lambdas
    @Override
    public void visitLineNumber(int line, Label start) {

    }
}
