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

package org.jetbrains.jet.codegen.optimization.ranges;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.optimization.transformer.MethodTransformer;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame;

import java.util.ArrayList;
import java.util.List;

public class RangesOptimizationMethodTransformer extends MethodTransformer {
    private static final int REMOVED_INSNS_COUNT_FOR_EACH_PATTERN = 1;

    public RangesOptimizationMethodTransformer(MethodTransformer methodTransformer) {
        super(methodTransformer);
    }

    @Override
    public void transform(@NotNull String owner, @NotNull MethodNode methodNode) {
        Frame<BasicValue>[] frames = runAnalyzer(
                new Analyzer<BasicValue>(new RangesOptimizationInterpreter()),
                owner,
                methodNode
        );

        InsnList insnList = methodNode.instructions;

        List<AbstractInsnNode> foundPatterns = new ArrayList<AbstractInsnNode>();

        for (int i = 0; i < insnList.size(); i++) {
            if (isFrequentRangePattern(frames, insnList, i)) {
                foundPatterns.add(insnList.get(i));
            }
        }

        int collapsedPatternsCount = 0;

        for (AbstractInsnNode startInsn : foundPatterns) {
            collapsePattern(frames, insnList, startInsn, collapsedPatternsCount++);
        }

        super.transform(owner, methodNode);
    }

    /**
     * Pattern desciption:
     * 0 invokeinterface java/util/Iterator.next:()Ljava/lang/Object;
     * 1 astore x
     * 2 aload x
     * 3 checkcast java/lang/Number
     * 4 invokevirtual java/lang/Number.<type>Value:()T
     * 5 (i/l/d)store y
     *
     * @param frames
     * @param insnList
     * @param index
     * @return
     */
    private static boolean isFrequentRangePattern(@NotNull Frame<BasicValue>[] frames, @NotNull InsnList insnList, int index) {
        if (index + 6 > insnList.size() || insnList.get(index).getOpcode() != Opcodes.INVOKEINTERFACE) {
            return false;
        }

        if (!((MethodInsnNode) insnList.get(index)).name.endsWith("next")) {
            return false;
        }

        BasicValue mayBeRangeIteratorValue = frames[index].getStack(frames[index].getStackSize() - 1);

        if (!(mayBeRangeIteratorValue instanceof RangeIteratorBasicValue)) {
            return false;
        }

        if (insnList.get(index + 1).getOpcode() != Opcodes.ASTORE) return false;
        if (insnList.get(index + 2).getOpcode() != Opcodes.ALOAD) return false;

        if (((VarInsnNode) insnList.get(index + 1)).var != ((VarInsnNode) insnList.get(index + 2)).var) {
            return false;
        }

        if (insnList.get(index + 3).getOpcode() != Opcodes.CHECKCAST ||
            !((TypeInsnNode) insnList.get(index + 3)).desc.equals("java/lang/Number")) {
            return false;
        }

        if (insnList.get(index + 4).getOpcode() != Opcodes.INVOKEVIRTUAL) {
            return false;
        }

        //method should be <T>Value (e.g. intValue)
        if (!((MethodInsnNode) insnList.get(index + 4)).name.equals(
                ((RangeIteratorBasicValue) mayBeRangeIteratorValue).getValuesTypename().toLowerCase() + "Value"
        )) {
            return false;
        }

        AbstractInsnNode storeInsn = insnList.get(index + 5);

        if (storeInsn.getOpcode() != Opcodes.ISTORE &&
            storeInsn.getOpcode() != Opcodes.LSTORE &&
            storeInsn.getOpcode() != Opcodes.FSTORE &&
            storeInsn.getOpcode() != Opcodes.DSTORE) {
            return false;
        }

        return true;
    }

    /**
     * Collapse pattern described in isFrequentRangePattern, adding four instructions to insnList and removing five
     *
     * @param frames
     * @param insnList
     * @param startInsn
     */
    private static void collapsePattern(
            @NotNull Frame<BasicValue>[] frames,
            @NotNull InsnList insnList, @NotNull AbstractInsnNode startInsn,
            int collapsedPatternsCount
    ) {

        int index = insnList.indexOf(startInsn);

        int frameIndex = index + collapsedPatternsCount * REMOVED_INSNS_COUNT_FOR_EACH_PATTERN;
        RangeIteratorBasicValue iteratorBasicValue = (RangeIteratorBasicValue) frames[frameIndex].
                getStack(frames[frameIndex].getStackSize() - 1);

        /* Small workaround (actually it's dirty hack)
           I need to store something in variable that is used for storing boxed value (see operations 1,2 in the isFrequentRangePattern desc)
         */
        int varIndexToStub = ((VarInsnNode) insnList.get(index + 1)).var;
        insnList.insert(startInsn.getPrevious(), new InsnNode(Opcodes.ACONST_NULL));
        insnList.insert(startInsn.getPrevious(), new VarInsnNode(Opcodes.ASTORE, varIndexToStub));
        //dirty hack ends here

        //add checkcast to kotlin/<T>Iterator before next() call
        insnList.insert(
                startInsn.getPrevious(),
                new TypeInsnNode(Opcodes.CHECKCAST, iteratorBasicValue.getType().getInternalName())
        );

        //invoke concrete method (kotlin/<T>iteraror.next<T>())
        insnList.insert(
                startInsn.getPrevious(),
                new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        iteratorBasicValue.getType().getInternalName(),
                        "next" + iteratorBasicValue.getValuesTypename(),
                        "()" + iteratorBasicValue.getValuesType().getDescriptor(),
                        false
                )
        );

        //removing next five instructions
        /*
            removing next five instructions:
             0 invokeinterface java/util/Iterator.next:()Ljava/lang/Object;
             1 astore x
             2 aload x
             3 checkcast java/lang/Number
             4 invokevirtual java/lang/Number.<type>Value:()T
         */
        AbstractInsnNode current = startInsn;
        for (int i = 0; i < 5; i++) {
            AbstractInsnNode next = current.getNext();
            insnList.remove(current);
            current = next;
        }
    }
}
