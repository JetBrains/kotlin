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

package org.jetbrains.jet.codegen.asm;

import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.tree.AbstractInsnNode;
import org.jetbrains.asm4.tree.MethodNode;

import java.util.ListIterator;

public class MaxCalcNode extends MethodVisitor {

    private int maxLocal;

    private final MethodNode node;

    public MaxCalcNode(MethodNode node)
    {
        super(Opcodes.ASM4, node);
        this.node = node;
        int paramsSize = (node.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;

        Type[] types = Type.getArgumentTypes(node.desc);
        for (Type type : types) {
            paramsSize += type.getSize();
        }
        maxLocal = paramsSize;
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
}
