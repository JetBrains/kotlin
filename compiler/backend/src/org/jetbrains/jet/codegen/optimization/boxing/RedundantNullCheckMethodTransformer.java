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

package org.jetbrains.jet.codegen.optimization.boxing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.optimization.transformer.MethodTransformer;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame;

import java.util.ArrayList;
import java.util.List;

public class RedundantNullCheckMethodTransformer extends MethodTransformer {
    public RedundantNullCheckMethodTransformer(MethodTransformer methodTransformer) {
        super(methodTransformer);
    }

    @Override
    public void transform(@NotNull String internalClassName, @NotNull MethodNode methodNode) {

        while (removeRedundantNullCheckPass(internalClassName, methodNode)) {
            //do nothing
        }

        super.transform(internalClassName, methodNode);
    }

    private static boolean removeRedundantNullCheckPass(@NotNull String internalClassName, @NotNull MethodNode methodNode) {
        InsnList insnList = methodNode.instructions;
        Frame<BasicValue>[] frames = analyze(
                internalClassName, methodNode,
                new BoxingInterpreter(insnList)
        );

        List<AbstractInsnNode> insnsToOptimize = new ArrayList<AbstractInsnNode>();

        for (int i = 0; i < insnList.size(); i++) {
            Frame<BasicValue> frame = frames[i];
            AbstractInsnNode insn = insnList.get(i);

            if ((insn.getOpcode() == Opcodes.IFNULL || insn.getOpcode() == Opcodes.IFNONNULL) &&
                frame != null && frame.getStack(frame.getStackSize() - 1) instanceof BoxedBasicValue) {

                insnsToOptimize.add(insn);
            }
        }

        for (AbstractInsnNode insn : insnsToOptimize) {
            if (insn.getPrevious() != null && insn.getPrevious().getOpcode() == Opcodes.DUP) {
                insnList.remove(insn.getPrevious());
            }
            else {
                insnList.insertBefore(insn, new InsnNode(Opcodes.POP));
            }

            assert insn.getOpcode() == Opcodes.IFNULL
                   || insn.getOpcode() == Opcodes.IFNONNULL : "only IFNULL/IFNONNULL are supported";

            if (insn.getOpcode() == Opcodes.IFNULL) {
                insnList.remove(insn);
            }
            else {
                insnList.set(
                        insn,
                        new JumpInsnNode(Opcodes.GOTO, ((JumpInsnNode) insn).label)
                );
            }
        }

        return insnsToOptimize.size() > 0;
    }
}
