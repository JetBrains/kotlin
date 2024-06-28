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
 *
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.jetbrains.kotlin.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.utils.SmartSet;
import org.jetbrains.kotlin.utils.SmartIdentityTable;
import org.jetbrains.org.objectweb.asm.*;

import java.util.*;

/**
 * This class is based on `org.objectweb.asm.MethodWriter`
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 * @author Denis Zharkov
 */
public class MaxStackFrameSizeAndLocalsCalculator extends MaxLocalsCalculator {
    private static final int[] FRAME_SIZE_CHANGE_BY_OPCODE;

    static {
        // copy-pasted from org.jetbrains.org.objectweb.asm.Frame
        int i;
        int[] b = new int[202];
        String s = "EFFFFFFFFGGFFFGGFFFEEFGFGFEEEEEEEEEEEEEEEEEEEEDEDEDDDDD"
                   + "CDCDEEEEEEEEEEEEEEEEEEEEBABABBBBDCFFFGGGEDCDCDCDCDCDCDCDCD"
                   + "CDCEEEEDDDDDDDCDCDCEFEFDDEEFFDEDEEEBDDBBDDDDDDCCCCCCCCEFED"
                   + "DDCDCDEEEEEEEEEEFEEEEEEDDEEDDEE";
        for (i = 0; i < b.length; ++i) {
            b[i] = s.charAt(i) - 'E';
        }

        FRAME_SIZE_CHANGE_BY_OPCODE = b;
    }

    private final LabelWrapper firstLabel;

    private LabelWrapper currentBlock;
    private LabelWrapper previousBlock;

    /**
     * The (relative) stack size after the last visited instruction. This size
     * is relative to the beginning of the current basic block, i.e., the true
     * stack size after the last visited instruction is equal to the
     * {@link MaxStackFrameSizeAndLocalsCalculator.LabelWrapper#inputStackSize} of the current basic block
     * plus <tt>stackSize</tt>.
     */
    private int stackSize;

    /**
     * The (relative) maximum stack size after the last visited instruction.
     * This size is relative to the beginning of the current basic block, i.e.,
     * the true maximum stack size after the last visited instruction is equal
     * to the {@link MaxStackFrameSizeAndLocalsCalculator.LabelWrapper#inputStackSize} of the current basic
     * block plus <tt>stackSize</tt>.
     */
    private int maxStackSize;

    /**
     * Maximum stack size of this method.
     */
    private int maxStack;

    private final Collection<ExceptionHandler> exceptionHandlers = new LinkedList<>();
    private final SmartIdentityTable<Label, LabelWrapper> labelWrappersTable = new SmartIdentityTable<>();

    public MaxStackFrameSizeAndLocalsCalculator(int api, int access, String descriptor, MethodVisitor mv) {
        super(api, access, descriptor, mv);

        firstLabel = getLabelWrapper(new Label());
        processLabel(firstLabel.label);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        throw new AssertionError("We don't support visitFrame because currently nobody needs");
    }

    @Override
    public void visitInsn(int opcode) {
        increaseStackSize(FRAME_SIZE_CHANGE_BY_OPCODE[opcode]);

        // if opcode == ATHROW or xRETURN, ends current block (no successor)
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
            noSuccessor();
        }

        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        if (opcode != Opcodes.NEWARRAY) {
            // updates current and max stack sizes only if it's not NEWARRAY
            // (stack size variation is 0 for NEWARRAY and +1 BIPUSH or SIPUSH)
            increaseStackSize(1);
        }

        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        increaseStackSize(FRAME_SIZE_CHANGE_BY_OPCODE[opcode]);

        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, @NotNull String type) {
        if (opcode == Opcodes.NEW) {
            // updates current and max stack sizes only if opcode == NEW
            // (no stack change for ANEWARRAY, CHECKCAST, INSTANCEOF)
            increaseStackSize(1);
        }

        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String desc) {
        int stackSizeVariation;

        // computes the stack size variation
        char c = desc.charAt(0);
        switch (opcode) {
            case Opcodes.GETSTATIC:
                stackSizeVariation = c == 'D' || c == 'J' ? 2 : 1;
                break;
            case Opcodes.PUTSTATIC:
                stackSizeVariation = c == 'D' || c == 'J' ? -2 : -1;
                break;
            case Opcodes.GETFIELD:
                stackSizeVariation = c == 'D' || c == 'J' ? 1 : 0;
                break;
            // case Constants.PUTFIELD:
            default:
                stackSizeVariation = c == 'D' || c == 'J' ? -3 : -2;
                break;
        }

        increaseStackSize(stackSizeVariation);

        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        int argSize = Type.getArgumentsAndReturnSizes(desc);
        int sizeVariation;
        if (opcode == Opcodes.INVOKESTATIC) {
            sizeVariation = (argSize & 0x03) - (argSize >> 2) + 1;
        }
        else {
            sizeVariation = (argSize & 0x03) - (argSize >> 2);
        }

        increaseStackSize(sizeVariation);

        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitInvokeDynamicInsn(@NotNull String name, @NotNull String desc, @NotNull Handle bsm, @NotNull Object... bsmArgs) {
        int argSize = Type.getArgumentsAndReturnSizes(desc);
        increaseStackSize((argSize & 0x03) - (argSize >> 2) + 1);

        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    @Override
    public void visitJumpInsn(int opcode, @NotNull Label label) {
        if (currentBlock != null) {
            // updates current stack size (max stack size unchanged
            // because stack size variation always negative in this
            // case)
            stackSize += FRAME_SIZE_CHANGE_BY_OPCODE[opcode];
            addSuccessor(getLabelWrapper(label), stackSize);

            if (opcode == Opcodes.GOTO) {
                noSuccessor();
            }
        }

        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLabel(@NotNull Label label) {
        processLabel(label);
        super.visitLabel(label);
    }

    private void processLabel(Label label) {
        LabelWrapper wrapper = getLabelWrapper(label);

        if (currentBlock != null) {
            // ends current block (with one new successor)
            currentBlock.outputStackMax = maxStackSize;
            addSuccessor(wrapper, stackSize);
        }

        // begins a new current block
        currentBlock = wrapper;
        // resets the relative current and max stack sizes
        stackSize = 0;
        maxStackSize = 0;

        if (previousBlock != null) {
            previousBlock.nextLabel = wrapper;
        }

        previousBlock = wrapper;
    }

    @Override
    public void visitLdcInsn(@NotNull Object cst) {
        // computes the stack size variation
        if (cst instanceof Long || cst instanceof Double) {
            increaseStackSize(2);
        }
        else {
            increaseStackSize(1);
        }

        super.visitLdcInsn(cst);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, @NotNull Label dflt, @NotNull Label... labels) {
        visitSwitchInsn(dflt, labels);

        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(@NotNull Label dflt, @NotNull int[] keys, @NotNull Label[] labels) {
        visitSwitchInsn(dflt, labels);

        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    private void visitSwitchInsn(Label dflt, Label[] labels) {
        if (currentBlock != null) {
            // updates current stack size (max stack size unchanged)
            --stackSize;
            // adds current block successors
            addSuccessor(getLabelWrapper(dflt), stackSize);
            for (Label label : labels) {
                addSuccessor(getLabelWrapper(label), stackSize);
            }
            // ends current block
            noSuccessor();
        }
    }

    @Override
    public void visitMultiANewArrayInsn(@NotNull String desc, int dims) {
        if (currentBlock != null) {
            increaseStackSize(dims - 1);
        }

        super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        // completes the control flow graph with exception handler blocks
        for (ExceptionHandler handler : exceptionHandlers) {
            LabelWrapper l = handler.start;
            LabelWrapper e = handler.end;

            while (l != e) {
                if (l == null) {
                    throw new IllegalStateException("Bad exception handler end");
                }

                l.addSuccessor(handler.handlerLabel, 0, true);
                l = l.nextLabel;
            }
        }

        /*
         * control flow analysis algorithm: while the block stack is not
         * empty, pop a block from this stack, update the max stack size,
         * compute the true (non relative) begin stack size of the
         * successors of this block, and push these successors onto the
         * stack (unless they have already been pushed onto the stack).
         * Note: by hypothesis, the {@link LabelWrapper#inputStackSize} of the
         * blocks in the block stack are the true (non relative) beginning
         * stack sizes of these blocks.
         */
        int max = 0;
        Stack<LabelWrapper> stack = new Stack<>();
        Set<LabelWrapper> pushed = new HashSet<>();

        stack.push(firstLabel);
        pushed.add(firstLabel);

        while (!stack.empty()) {
            LabelWrapper current = stack.pop();
            int start = current.inputStackSize;
            int blockMax = start + current.outputStackMax;

            // updates the global max stack size
            if (blockMax > max) {
                max = blockMax;
            }

            // analyzes the successors of the block
            for (ControlFlowEdge edge : current.successors) {
                LabelWrapper successor = edge.successor;

                if (!pushed.contains(successor)) {
                    // computes its true beginning stack size...
                    successor.inputStackSize = edge.isExceptional ? 1 : start + edge.outputStackSize;
                    // ...and pushes it onto the stack
                    pushed.add(successor);
                    stack.push(successor);
                }
            }
        }

        this.maxStack = Math.max(this.maxStack, Math.max(maxStack, max));

        super.visitMaxs(this.maxStack, maxLocals);
    }

    @Override
    public void visitTryCatchBlock(
            @NotNull Label start, @NotNull Label end,
            @NotNull Label handler, String type
    ) {
        ExceptionHandler exceptionHandler = new ExceptionHandler(
                getLabelWrapper(start), getLabelWrapper(end), getLabelWrapper(handler)
        );

        exceptionHandlers.add(exceptionHandler);

        super.visitTryCatchBlock(start, end, handler, type);
    }

    private static class ExceptionHandler {
        private final LabelWrapper start;
        private final LabelWrapper end;
        private final LabelWrapper handlerLabel;

        public ExceptionHandler(
                LabelWrapper start,
                LabelWrapper end,
                LabelWrapper handlerLabel
        ) {
            this.start = start;
            this.end = end;
            this.handlerLabel = handlerLabel;
        }
    }

    private static class ControlFlowEdge {
        private final LabelWrapper successor;
        private final int outputStackSize;
        private final boolean isExceptional;

        public ControlFlowEdge(LabelWrapper successor, int outputStackSize, boolean isExceptional) {
            this.successor = successor;
            this.outputStackSize = outputStackSize;
            this.isExceptional = isExceptional;
        }
    }

    private static class LabelWrapper {
        private final Label label;
        private LabelWrapper nextLabel = null;
        private final Collection<ControlFlowEdge> successors = new LinkedList<>();

        private final int index;
        private int outputStackMax = 0;
        private int inputStackSize = 0;

        public LabelWrapper(Label label, int index) {
            this.label = label;
            this.index = index;
        }

        private void addSuccessor(LabelWrapper successor, int outputStackSize, boolean isExceptional) {
            successors.add(new ControlFlowEdge(successor, outputStackSize, isExceptional));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LabelWrapper wrapper = (LabelWrapper) o;
            return index == wrapper.index;
        }

        @Override
        public int hashCode() {
            return index;
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    private LabelWrapper getLabelWrapper(Label label) {
        return labelWrappersTable.getOrCreate(label, () -> new LabelWrapper(label, labelWrappersTable.getSize()));
    }

    private void increaseStackSize(int variation) {
        updateStackSize(stackSize + variation);
    }

    private void updateStackSize(int size) {
        if (size > maxStackSize) {
            maxStackSize = size;
        }

        stackSize = size;
    }

    private void addSuccessor(LabelWrapper successor, int outputStackSize) {
        currentBlock.addSuccessor(successor, outputStackSize, false);
    }

    /**
     * Ends the current basic block. This method must be used in the case where
     * the current basic block does not have any successor.
     */
    private void noSuccessor() {
        if (currentBlock != null) {
            currentBlock.outputStackMax = maxStackSize;
            currentBlock = null;
        }
    }
}
