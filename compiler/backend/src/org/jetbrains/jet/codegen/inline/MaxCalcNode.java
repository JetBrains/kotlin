/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

public class MaxCalcNode extends MethodVisitor {

    private int maxLocal;

    private final MethodNode node;

    public MaxCalcNode(@NotNull MethodNode node) {
        this(node.desc, node, (node.access & Opcodes.ACC_STATIC) != 0);
    }

    public MaxCalcNode(@NotNull String desc, boolean isStatic) {
        this(desc, null, isStatic);
    }

    private MaxCalcNode(@NotNull String desc, @Nullable MethodNode node, boolean isStatic) {
        super(InlineCodegenUtil.API, node);
        this.node = node;
        maxLocal = (Type.getArgumentsAndReturnSizes(desc) >> 2) - (isStatic ? 1 : 0);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        int size = opcode == Opcodes.LLOAD || opcode == Opcodes.DLOAD || opcode == Opcodes.LSTORE || opcode == Opcodes.DSTORE ? 2 : 1;
        updateMaxLocal(var, size);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        updateMaxLocal(var, 1);
    }

    private void updateMaxLocal(int index, int size) {
        maxLocal = Math.max(maxLocal, index + size);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        //NB: it's hack for fast maxStack calculation cause it performed only in MethodWriter
        //temporary solution: maxStack = instruction size (without labels and line numbers) * 2 (cause 1 instruction could put value of size 2)
        if (node != null) {
            int size = 0;
            ListIterator<AbstractInsnNode> iterator = node.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode next = iterator.next();
                int type = next.getType();
                if (type != AbstractInsnNode.LINE && type != AbstractInsnNode.LABEL) {
                    size++;
                }
            }
            super.visitMaxs(Math.max(size * 2, maxStack), Math.max(maxLocals, this.maxLocal));
        }
        else {
            super.visitMaxs(maxStack, maxLocals);
        }
    }

    public int getMaxLocal() {
        return maxLocal;
    }
}
