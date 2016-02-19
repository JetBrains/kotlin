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
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode;

public class RemapVisitor extends MethodBodyVisitor {
    private final LocalVarRemapper remapper;
    private final FieldRemapper nodeRemapper;
    private final InstructionAdapter instructionAdapter;

    protected RemapVisitor(
            MethodVisitor mv,
            LocalVarRemapper localVarRemapper,
            FieldRemapper nodeRemapper
    ) {
        super(mv);
        this.instructionAdapter = new InstructionAdapter(mv);
        this.remapper = localVarRemapper;
        this.nodeRemapper = nodeRemapper;
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        remapper.visitIincInsn(var, increment, mv);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        remapper.visitVarInsn(opcode, var, instructionAdapter);
    }

    @Override
    public void visitLocalVariable(
            @NotNull String name, @NotNull String desc, String signature, @NotNull Label start, @NotNull Label end, int index
    ) {
        remapper.visitLocalVariable(name, desc, signature, start, end, index, mv);
    }

    @Override
    public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String desc) {
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
}
