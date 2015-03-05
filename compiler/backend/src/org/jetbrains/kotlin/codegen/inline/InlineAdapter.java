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
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.ArrayList;
import java.util.List;

public class InlineAdapter extends InstructionAdapter {

    private int nextLocalIndex = 0;
    private final SourceMapper sourceMapper;

    private boolean isLambdaInlining = false;

    private final List<CatchBlock> blocks = new ArrayList<CatchBlock>();

    private int nextLocalIndexBeforeInline = -1;

    public InlineAdapter(MethodVisitor mv, int localsSize, @NotNull SourceMapper sourceMapper) {
        super(InlineCodegenUtil.API, mv);
        nextLocalIndex = localsSize;
        this.sourceMapper = sourceMapper;
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        updateIndex(var, 1);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        updateIndex(var, (opcode == Opcodes.DSTORE || opcode == Opcodes.LSTORE || opcode == Opcodes.DLOAD || opcode == Opcodes.LLOAD ? 2 : 1));
    }

    private void updateIndex(int var, int varSize) {
        int newIndex = var + varSize;
        if (newIndex > nextLocalIndex) {
            nextLocalIndex = newIndex;
        }
    }

    public int getNextLocalIndex() {
        return nextLocalIndex;
    }

    public void setLambdaInlining(boolean isInlining) {
        this.isLambdaInlining = isInlining;
        if (isInlining) {
            nextLocalIndexBeforeInline = nextLocalIndex;
        } else {
            nextLocalIndex = nextLocalIndexBeforeInline;
        }
    }

    @Override
    public void visitTryCatchBlock(Label start,
            Label end, Label handler, String type) {
        if(!isLambdaInlining) {
            blocks.add(new CatchBlock(start, end, handler, type));
        }
        else {
            super.visitTryCatchBlock(start, end, handler, type);
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        sourceMapper.visitLineNumber(mv, line, start);
    }

    @Override
    public void visitMaxs(int stack, int locals) {
        for (CatchBlock b : blocks) {
            super.visitTryCatchBlock(b.start, b.end, b.handler, b.type);
        }
        super.visitMaxs(stack, locals);
    }

    private static class CatchBlock {
        private final Label start;
        private final Label end;
        private final Label handler;
        private final String type;

        public CatchBlock(Label start, Label end, Label handler, String type) {
            this.start = start;
            this.end = end;
            this.handler = handler;
            this.type = type;
        }
    }
}
