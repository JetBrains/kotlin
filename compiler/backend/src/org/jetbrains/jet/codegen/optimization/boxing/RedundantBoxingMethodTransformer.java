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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.optimization.transformer.MethodTransformer;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame;

import java.util.HashSet;
import java.util.Set;

public class RedundantBoxingMethodTransformer extends MethodTransformer {
    public RedundantBoxingMethodTransformer(MethodTransformer methodTransformer) {
        super(methodTransformer);
    }

    @Override
    public void transform(@NotNull String internalClassName, @NotNull MethodNode node) {
        RedundantBoxingInterpreter interpreter = new RedundantBoxingInterpreter(node.instructions);
        Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(interpreter);

        Frame<BasicValue>[] frames = runAnalyzer(analyzer, internalClassName, node);
        Set<BoxedBasicValue> valuesToOptimize = filterSafeToRemoveValues(interpreter.getCandidatesBoxedValues());

        if (!valuesToOptimize.isEmpty()) {
            findValuesClashingWithVariables(node, frames);

            valuesToOptimize = filterSafeToRemoveValues(valuesToOptimize);

            adaptLocalVariableTableForBoxedValues(node, frames);

            applyVariablesRemapping(node, buildVariablesRemapping(valuesToOptimize, node));

            adaptInstructionsForBoxedValues(node, frames, valuesToOptimize);
        }

        super.transform(internalClassName, node);
    }

    @NotNull
    private static Set<BoxedBasicValue> filterSafeToRemoveValues(@NotNull Set<BoxedBasicValue> values) {
        return new HashSet<BoxedBasicValue>(Collections2.filter(values, new Predicate<BoxedBasicValue>() {
            @Override
            public boolean apply(BoxedBasicValue input) {
                return input.isSafeToRemove();
            }
        }));
    }

    private static void adaptLocalVariableTableForBoxedValues(@NotNull MethodNode node, @NotNull Frame<BasicValue>[] frames) {
        for (LocalVariableNode localVariableNode : node.localVariables) {
            if (Type.getType(localVariableNode.desc).getSort() != Type.OBJECT) {
                continue;
            }

            adaptLocalVariableTableEntryForBoxedValues(node, frames, localVariableNode);
        }
    }

    private static void adaptLocalVariableTableEntryForBoxedValues(
            @NotNull MethodNode node, @NotNull Frame<BasicValue>[] frames, @NotNull LocalVariableNode localVariableNode
    ) {
        InsnList insnList = node.instructions;
        int from = insnList.indexOf(localVariableNode.start) + 1;
        int to = insnList.indexOf(localVariableNode.end) - 1;

        for (int i = from; i <= to; i++) {
            AbstractInsnNode insn = insnList.get(i);
            if (insn.getOpcode() == Opcodes.ASTORE && ((VarInsnNode) insn).var == localVariableNode.index) {
                if (frames[i] == null) {
                    return;
                }

                BasicValue top = frames[i].getStack(frames[i].getStackSize() - 1);
                if (!(top instanceof BoxedBasicValue) || !((BoxedBasicValue) top).isSafeToRemove()) {
                    return;
                }

                localVariableNode.desc = ((BoxedBasicValue) top).getPrimitiveType().getDescriptor();

                return;
            }
        }
    }

    private static void findValuesClashingWithVariables(
            @NotNull MethodNode node, @NotNull Frame<BasicValue>[] frames
    ) {
        while (findValuesClashingWithVariablesPass(node, frames)) {
            // do nothing
        }
    }

    private static boolean findValuesClashingWithVariablesPass(
            @NotNull MethodNode node, @NotNull Frame<BasicValue>[] frames
    ) {
        InsnList insnList = node.instructions;
        boolean needToRepeat = false;

        for (LocalVariableNode localVariableNode : node.localVariables) {
            if (Type.getType(localVariableNode.desc).getSort() != Type.OBJECT) {
                continue;
            }

            int index = localVariableNode.index;
            int from = insnList.indexOf(localVariableNode.start) + 1;
            int to = insnList.indexOf(localVariableNode.end) - 1;

            if (isThereUnsafeStoreInstruction(insnList, frames, from, to, index)) {
                needToRepeat |= markAllBoxedValuesStoredAsUnsafeToRemove(insnList, frames, from, to, index);
            }
        }

        return needToRepeat;
    }

    /**
     * Check if there are unsafe ASTORE instructions, that put into var something but boxed values of the same type
     *
     * @param insnList
     * @param frames
     * @param from
     * @param to
     * @param varIndex
     * @return
     */
    private static boolean isThereUnsafeStoreInstruction(
            @NotNull InsnList insnList,
            @NotNull Frame<BasicValue>[] frames,
            int from, int to, int varIndex
    ) {
        Type usedAsType = null;

        for (int i = from; i <= to; i++) {
            AbstractInsnNode insn = insnList.get(i); //TODO: handle exception?
            if (insn.getOpcode() == Opcodes.ASTORE && ((VarInsnNode) insn).var == varIndex) {
                if (frames[i] == null) {
                    return true;
                }

                BasicValue top = frames[i].getStack(frames[i].getStackSize() - 1);
                if (!(top instanceof BoxedBasicValue) || !((BoxedBasicValue) top).isSafeToRemove()) {
                    return true;
                }

                if (usedAsType == null) {
                    usedAsType = ((BoxedBasicValue) top).getPrimitiveType();
                }

                if (!usedAsType.equals(((BoxedBasicValue) top).getPrimitiveType())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean markAllBoxedValuesStoredAsUnsafeToRemove(
            @NotNull InsnList insnList,
            @NotNull Frame<BasicValue>[] frames,
            int from, int to, int varIndex
    ) {
        boolean wasChanges = false;

        for (int i = from; i <= to; i++) {
            AbstractInsnNode insn = insnList.get(i);
            if (insn.getOpcode() == Opcodes.ASTORE && ((VarInsnNode) insn).var == varIndex) {
                if (frames[i] == null) {
                    continue;
                }

                BasicValue top = frames[i].getStack(frames[i].getStackSize() - 1);
                if (!(top instanceof BoxedBasicValue) || !((BoxedBasicValue) top).isSafeToRemove()) {
                    continue;
                }

                wasChanges |= true;
                ((BoxedBasicValue) top).propagateRemovingAsUnsafe();
            }
        }

        return wasChanges;
    }

    @NotNull
    private static int[] buildVariablesRemapping(@NotNull Iterable<BoxedBasicValue> values, @NotNull MethodNode node) {
        Set<Integer> doubleSizedVars = new HashSet<Integer>();
        for (BoxedBasicValue value : values) {
            if (value.getPrimitiveType().getSize() == 2) {
                doubleSizedVars.addAll(value.getVariablesIndexes());
            }
        }

        node.maxLocals += doubleSizedVars.size();
        int[] remapping = new int[node.maxLocals];
        for (int i = 0; i < remapping.length; i++) {
            remapping[i] = i;
        }

        for (int varIndex : doubleSizedVars) {
            for (int i = varIndex + 1; i < remapping.length; i++) {
                remapping[i]++;
            }
        }

        return remapping;
    }

    private static void applyVariablesRemapping(@NotNull MethodNode node, @NotNull int[] remapping) {
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof VarInsnNode) {
                ((VarInsnNode) insn).var = remapping[((VarInsnNode) insn).var];
            }
            if (insn instanceof IincInsnNode) {
                ((IincInsnNode) insn).var = remapping[((IincInsnNode) insn).var];
            }
        }

        for (LocalVariableNode localVariableNode : node.localVariables) {
            localVariableNode.index = remapping[localVariableNode.index];
        }
    }

    private static void adaptInstructionsForBoxedValues(
            @NotNull MethodNode node, @NotNull Frame<BasicValue>[] frames, @NotNull Set<BoxedBasicValue> values
    ) {
        addPopInstructionsForBoxedValues(node, frames);

        for (BoxedBasicValue value : values) {
            adaptInstructionsForBoxedValue(node, value);
        }
    }

    private static void addPopInstructionsForBoxedValues(
            @NotNull MethodNode node, @NotNull Frame<BasicValue>[] frames
    ) {
        for (int i = 0; i < node.instructions.size(); i++) {
            AbstractInsnNode insn = node.instructions.get(i);
            if (insn.getOpcode() != Opcodes.POP || frames[i] == null) {
                continue;
            }

            BasicValue top = frames[i].getStack(frames[i].getStackSize() - 1);
            if (top instanceof BoxedBasicValue && ((BoxedBasicValue) top).isSafeToRemove()) {
                ((BoxedBasicValue) top).addInsn(node.instructions.get(i));
            }
        }
    }

    private static void adaptInstructionsForBoxedValue(@NotNull MethodNode node, @NotNull BoxedBasicValue value) {
        adaptBoxingInstruction(node, value);

        for (AbstractInsnNode insn : value.getAssociatedInsns()) {
            adaptInstruction(node, insn, value);
        }
    }

    private static void adaptBoxingInstruction(@NotNull MethodNode node, @NotNull BoxedBasicValue value) {
        if (!value.isFromProgressionIterator()) {
            node.instructions.remove(value.getBoxingInsn());
        }
        else {
            ProgressionIteratorBasicValue iterator = value.getProgressionIterator();
            assert iterator != null : "iterator should not be null because isFromProgressionIterator returns true";

            //add checkcast to kotlin/<T>Iterator before next() call
            node.instructions.insertBefore(
                    value.getBoxingInsn(),
                    new TypeInsnNode(Opcodes.CHECKCAST, iterator.getType().getInternalName())
            );

            //invoke concrete method (kotlin/<T>iteraror.next<T>())
            node.instructions.set(
                    value.getBoxingInsn(),
                    new MethodInsnNode(
                            Opcodes.INVOKEVIRTUAL,
                            iterator.getType().getInternalName(),
                            iterator.getNextMethodName(),
                            iterator.getNextMethodDesc(),
                            false
                    )
            );
        }
    }

    private static void adaptInstruction(
            @NotNull MethodNode node, @NotNull AbstractInsnNode insn, @NotNull BoxedBasicValue value
    ) {
        boolean isDoubleSize = value.isDoubleSize();

        switch (insn.getOpcode()) {
            case Opcodes.POP:
                if (isDoubleSize) {
                    node.instructions.set(
                            insn,
                            new InsnNode(Opcodes.POP2)
                    );
                }
                break;
            case Opcodes.DUP:
                if (isDoubleSize) {
                    node.instructions.set(
                            insn,
                            new InsnNode(Opcodes.DUP2)
                    );
                }
                break;
            case Opcodes.ASTORE:
            case Opcodes.ALOAD:
                int intVarOpcode = insn.getOpcode() == Opcodes.ASTORE ? Opcodes.ISTORE : Opcodes.ILOAD;
                node.instructions.set(
                        insn,
                        new VarInsnNode(
                                value.getPrimitiveType().getOpcode(intVarOpcode),
                                ((VarInsnNode) insn).var
                        )
                );
                break;
            default:
                node.instructions.remove(insn);
        }
    }
}
