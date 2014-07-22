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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.*;


import java.util.*;

import static org.jetbrains.jet.codegen.inline.InlineCodegenUtil.*;

public class InternalFinallyBlockInliner {

    private static class FinallyBlockInfo {

        final AbstractInsnNode startIns;

        final AbstractInsnNode endInsExclusive;

        private FinallyBlockInfo(@NotNull AbstractInsnNode inclusiveStart, @NotNull AbstractInsnNode exclusiveEnd) {
            startIns = inclusiveStart;
            endInsExclusive = exclusiveEnd;
        }
    }

    public static void processInlineFunFinallyBlocks(@NotNull MethodNode inlineFun, int lambdaTryCatchBlockNodes) {
        int index = 0;
        List<TryCatchBlockNodeInfo> inlineFunTryBlockInfo = new ArrayList<TryCatchBlockNodeInfo>();
        for (TryCatchBlockNode block : inlineFun.tryCatchBlocks) {
            inlineFunTryBlockInfo.add(new TryCatchBlockNodeInfo(block, index++ < lambdaTryCatchBlockNodes));
        }

        if (hasFinallyBlocks(inlineFunTryBlockInfo)) {
            new InternalFinallyBlockInliner(inlineFun, inlineFunTryBlockInfo).processInlineFunFinallyBlocks();
        }
    }

    @NotNull
    private final MethodNode inlineFun;

    private final List<TryCatchBlockNodeInfo> inlineFunTryBlockInfo;

    private final ListMultimap<LabelNode, TryCatchBlockNodeInfo> tryBlockStarts = LinkedListMultimap.create();

    private final ListMultimap<LabelNode, TryCatchBlockNodeInfo> tryBlockEnds = LinkedListMultimap.create();

    //lambdaTryCatchBlockNodes is number of TryCatchBlockNodes that was inlined with lambdas into function
    //due to code generation specific they placed before function TryCatchBlockNodes
    private InternalFinallyBlockInliner(@NotNull MethodNode inlineFun, List<TryCatchBlockNodeInfo> inlineFunTryBlockInfo) {
        this.inlineFun = inlineFun;
        this.inlineFunTryBlockInfo = inlineFunTryBlockInfo;
    }

    private int initAndGetVarIndexForNonLocalReturnValue() {
        //sortTryCatchBlocks();/*TODO maybe remove*/
        mapLabelsToTryCatchBlocks();

        MaxCalcNode tempCalcNode = new MaxCalcNode(inlineFun.desc, (inlineFun.access & Opcodes.ACC_STATIC) != 0);
        inlineFun.accept(tempCalcNode);
        return tempCalcNode.getMaxLocal();
    }

    private void processInlineFunFinallyBlocks() {
        int nextTempNonLocalVarIndex = initAndGetVarIndexForNonLocalReturnValue();

        Stack<TryCatchBlockNodeInfo> coveringTryCatchBlocks = new Stack<TryCatchBlockNodeInfo>();
        InsnList instructions = inlineFun.instructions;

        //As we do finally block code search after non-local return instruction
        // we should be sure that all others non-local returns already processed in this finally block.
        // So we do instruction processing in reverse order!
        AbstractInsnNode curIns = instructions.getLast();
        while (curIns != null) {
            updateCoveringTryBlocks(coveringTryCatchBlocks, curIns);

            //At this point only global return is possible, local one already substituted with: goto endLabel
            if (!InlineCodegenUtil.isReturnOpcode(curIns.getOpcode()) || !InlineCodegenUtil.isMarkedReturn(curIns)) {
                curIns = curIns.getPrevious();
                continue;
            }

            AbstractInsnNode instrInsertFinallyBefore = curIns.getPrevious();
            AbstractInsnNode nextPrev = instrInsertFinallyBefore.getPrevious();
            Type nonLocalReturnType = InlineCodegenUtil.getReturnType(curIns.getOpcode());

            //Generally there could be several tryCatch blocks (group) on one code interval (same start and end labels, but maybe different handlers) -
            // all of them refer to one try/*catches*/finally or try/catches.
            // Each group that corresponds to try/*catches*/finally contains tryCatch block with default handler.
            // For each such group we should insert corresponding finally before non-local return.
            // So we split all try blocks on current instructions to groups and process them independently
            List<TryBlockCluster> clusters = InlinePackage.doClustering(coveringTryCatchBlocks);
            ListIterator<TryBlockCluster> tryCatchBlockIterator = clusters.listIterator(clusters.size());
            //Reverse visiting cause innermost tryCatchBlocks in the end
            while (tryCatchBlockIterator.hasPrevious()) {
                TryBlockCluster originalFinallyCluster = tryCatchBlockIterator.previous();
                List<TryCatchBlockNodeInfo> clusterBlocks = originalFinallyCluster.getBlocks();
                TryCatchBlockNodeInfo originalFinallyBlock = clusterBlocks.get(0);

                FinallyBlockInfo finallyInfo = findFinallyBlockBody(originalFinallyBlock, inlineFunTryBlockInfo);
                if (finallyInfo == null) continue;

                instructions.resetLabels();

                List<TryCatchBlockNodePosition> tryCatchBlockInlinedInFinally = findTryCatchBlocksInlinedInFinally(finallyInfo);

                //Keep some information about label nodes, we need it to understand whether it's jump inside finally block or outside
                // in first case we do call VISIT on instruction otherwise recreating jump instruction (see below)
                Set<LabelNode> labelsInsideFinally = rememberOriginalLabelNodes(finallyInfo);

                //Creating temp node for finally block copy with some additional instruction
                MethodNode finallyBlockCopy = createEmptyMethodNode();
                Label newFinallyStart = new Label();
                Label newFinallyEnd = new Label();

                if (nonLocalReturnType != Type.VOID_TYPE) {
                    finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ISTORE), nextTempNonLocalVarIndex);
                }
                finallyBlockCopy.visitLabel(newFinallyStart);

                //Writing finally block body to temporary node
                AbstractInsnNode currentIns = finallyInfo.startIns;
                while (currentIns != finallyInfo.endInsExclusive) {
                    //This condition allows another model for non-local returns processing
                    if (false && InlineCodegenUtil.isReturnOpcode(currentIns.getOpcode()) && !InlineCodegenUtil.isMarkedReturn(currentIns)) {
                        //substitute all local returns in finally finallyInfo with non-local one lambdaFinallyBlocks try finallyInfo
                        //TODO same for jumps
                        Type localReturnType = InlineCodegenUtil.getReturnType(currentIns.getOpcode());
                        substituteReturnValueInFinally(nextTempNonLocalVarIndex, nonLocalReturnType, finallyBlockCopy,
                                                       localReturnType, true);

                        instrInsertFinallyBefore.accept(finallyBlockCopy);
                        curIns.accept(finallyBlockCopy);
                    }
                    else {
                        boolean isInsOrJumpInsideFinally =
                                !(currentIns instanceof JumpInsnNode) ||
                                labelsInsideFinally.contains(((JumpInsnNode) currentIns).label);

                        if (isInsOrJumpInsideFinally) {
                            currentIns.accept(finallyBlockCopy); //VISIT
                        }
                        else {
                            //keep original jump: add currentIns clone
                            finallyBlockCopy.instructions.add(new JumpInsnNode(currentIns.getOpcode(), ((JumpInsnNode) currentIns).label));
                        }
                    }

                    currentIns = currentIns.getNext();
                }

                finallyBlockCopy.visitLabel(newFinallyEnd);
                if (nonLocalReturnType != Type.VOID_TYPE) {
                    finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ILOAD), nextTempNonLocalVarIndex);
                    nextTempNonLocalVarIndex += nonLocalReturnType.getSize(); //TODO: do more wise indexing
                }

                //Copying finally body before non-local return instruction
                InlineCodegenUtil.insertNodeBefore(finallyBlockCopy, inlineFun, instrInsertFinallyBefore);

                nextPrev = updateExceptionTable(coveringTryCatchBlocks, nextPrev, clusterBlocks, newFinallyStart, newFinallyEnd,
                                                tryCatchBlockInlinedInFinally);
            }
            curIns = nextPrev;
        }

        inlineFun.tryCatchBlocks.clear();
        for (TryCatchBlockNodeInfo info : inlineFunTryBlockInfo) {
            inlineFun.tryCatchBlocks.add(info.getNode());
        }
    }

    @NotNull
    private static Set<LabelNode> rememberOriginalLabelNodes(@NotNull FinallyBlockInfo finallyInfo) {
        Set<LabelNode> labelsInsideFinally = new HashSet<LabelNode>();
        for (AbstractInsnNode currentIns = finallyInfo.startIns; currentIns != finallyInfo.endInsExclusive; currentIns = currentIns.getNext()) {
            if (currentIns instanceof LabelNode) {
                labelsInsideFinally.add((LabelNode) currentIns);
            }
        }
        return labelsInsideFinally;
    }

    @Nullable
    private AbstractInsnNode updateExceptionTable(
            @NotNull Stack<TryCatchBlockNodeInfo> coveringTryBlocks,
            @Nullable AbstractInsnNode nextPrev,
            @NotNull List<TryCatchBlockNodeInfo> clusterBlocks,
            @NotNull Label newFinallyStart,
            @NotNull Label newFinallyEnd,
            @NotNull List<TryCatchBlockNodePosition> tryCatchBlockPresentInFinally
    ) {

        //copy tryCatchFinallies that totally in finally block
        for (TryCatchBlockNodePosition position : tryCatchBlockPresentInFinally) {
            //TODO assert INNER
            TryCatchBlockNode tryCatchBlockNode = position.getNodeInfo().getNode();
            TryCatchBlockNode additionalTryCatchBlock =
                    new TryCatchBlockNode((LabelNode) tryCatchBlockNode.start.getLabel().info,
                                          (LabelNode) tryCatchBlockNode.end.getLabel().info,
                                          (LabelNode) tryCatchBlockNode.handler.getLabel().info,
                                          tryCatchBlockNode.type);

            TryCatchBlockNodeInfo newInfo = new TryCatchBlockNodeInfo(additionalTryCatchBlock, true);
            tryBlockStarts.put(additionalTryCatchBlock.start, newInfo);
            tryBlockEnds.put(additionalTryCatchBlock.end, newInfo);
            inlineFunTryBlockInfo.add(newInfo);
        }

        // Inserted finally shouldn't be handled by corresponding catches,
        // so we should split original interval by inserted finally one
        for (TryCatchBlockNodeInfo block : clusterBlocks) {
            //update exception mapping
            LabelNode oldStartNode = block.getNode().start;
            tryBlockStarts.remove(oldStartNode, block);
            block.getNode().start = (LabelNode) newFinallyEnd.info;

            TryCatchBlockNode additionalTryCatchBlock =
                    new TryCatchBlockNode(oldStartNode, (LabelNode) newFinallyStart.info, block.getNode().handler, block.getNode().type);

            TryCatchBlockNodeInfo newInfo = new TryCatchBlockNodeInfo(additionalTryCatchBlock, false);
            tryBlockStarts.put(additionalTryCatchBlock.start, newInfo);
            tryBlockEnds.put(additionalTryCatchBlock.end, newInfo);

            inlineFunTryBlockInfo.add(newInfo);

            //TODO add assert
            nextPrev = additionalTryCatchBlock.end;
            coveringTryBlocks.pop();
        }
        sortTryCatchBlocks();
        return nextPrev;
    }

    //Keep information about try blocks that cover current instruction -
    // pushing and popping it to stack entering and exiting tryCatchBlock start and end labels
    private void updateCoveringTryBlocks(Stack<TryCatchBlockNodeInfo> coveringTryBlocks, AbstractInsnNode curIns) {
        if (!(curIns instanceof LabelNode)) return;

        for (TryCatchBlockNodeInfo startNode : tryBlockStarts.get((LabelNode) curIns)) {
            if (!startNode.getOnlyCopyNotProcess()) {
                TryCatchBlockNodeInfo pop = coveringTryBlocks.pop();
                assert startNode == pop : "Wrong try-catch structure " + startNode + " " + pop;
            }
        }

        //Reversing list order cause we should pop external block before internal one
        // (originally internal blocks goes before external one, such invariant preserved via sortTryCatchBlocks method)
        for (TryCatchBlockNodeInfo info : Lists.reverse(tryBlockEnds.get((LabelNode) curIns))) {
            if (!info.getOnlyCopyNotProcess()) {
                coveringTryBlocks.add(info);
            }
        }
    }

    private static boolean hasFinallyBlocks(List<TryCatchBlockNodeInfo> inlineFunTryBlockInfo) {
        for (TryCatchBlockNodeInfo block : inlineFunTryBlockInfo) {
            if (!block.getOnlyCopyNotProcess() && block.getNode().type == null) {
                return true;
            }
        }
        return false;
    }

    private void mapLabelsToTryCatchBlocks() {
        for (TryCatchBlockNodeInfo block : inlineFunTryBlockInfo) {
            tryBlockStarts.put(block.getNode().start, block);
            tryBlockEnds.put(block.getNode().end, block);
        }
    }

    //As described above all tryCatch group that have finally block also should contains tryCatchBlockNode with default handler.
    //So we assume that instructions between end of tryCatchBlock and start of next tryCatchBlock with same default handler is required finally body.
    //There is at least two tryCatchBlockNodes in list cause there is always tryCatchBlockNode on first instruction of default handler:
    // "ASTORE defaultHandlerExceptionIndex" (handles itself, as does java).
    @Nullable
    private FinallyBlockInfo findFinallyBlockBody(
            @NotNull TryCatchBlockNodeInfo tryCatchBlock,
            @NotNull List<TryCatchBlockNodeInfo> tryCatchBlocks
    ) {
        List<TryCatchBlockNodeInfo> sameDefaultHandler = new ArrayList<TryCatchBlockNodeInfo>();
        LabelNode defaultHandler = null;
        boolean afterStartBlock = false;
        for (TryCatchBlockNodeInfo block : tryCatchBlocks) {
            if (tryCatchBlock == block) {
                afterStartBlock = true;
            }

            if (afterStartBlock) {
                if (block.getNode().type == null && (firstLabelInChain(tryCatchBlock.getNode().start) == firstLabelInChain(block.getNode().start) &&
                                                     firstLabelInChain(tryCatchBlock.getNode().end) == firstLabelInChain(block.getNode().end)
                                                     || defaultHandler == firstLabelInChain(block.getNode().handler))) {
                    sameDefaultHandler.add(block); //first is tryCatchBlock if no catch clauses
                    if (defaultHandler == null) {
                        defaultHandler = firstLabelInChain(block.getNode().handler);
                    }
                }
            }
        }

        if (sameDefaultHandler.isEmpty()) {
            //there is no finally block
            //it always should be present in default handler
            return null;
        }

        TryCatchBlockNodeInfo nextIntervalWithSameDefaultHandler = sameDefaultHandler.get(1);
        AbstractInsnNode startFinallyChain = tryCatchBlock.getNode().end;
        AbstractInsnNode endFinallyChainExclusive = skipLastGotoIfNeeded(nextIntervalWithSameDefaultHandler.getNode().handler,
                                                        nextIntervalWithSameDefaultHandler.getNode().start);

        return new FinallyBlockInfo(startFinallyChain.getNext(), endFinallyChainExclusive);
    }

    @NotNull
    private AbstractInsnNode skipLastGotoIfNeeded(
            @NotNull LabelNode defaultHandlerStartLabel,
            @NotNull AbstractInsnNode lastFinallyInsExclusive
    ) {

        AbstractInsnNode prevLast = getPrevNoLineNumberOrLabel(lastFinallyInsExclusive, true);
        assert prevLast != null : "Empty finally block: " + lastFinallyInsExclusive;

        if (prevLast.getOpcode() == Opcodes.GOTO) {
            //There we should understand whether goto is jump over catches or last break/continue command inside finally.
            //If it's a jump over catches so next is true:
            //      1. jump label should go after default catch handler start label
            //          AND
            //      2. it shouldn't be present in default catch block, otherwise it break/continue
            LabelNode targetJump = ((JumpInsnNode) prevLast).label;

            InsnList instructions = inlineFun.instructions;
            if (instructions.indexOf(defaultHandlerStartLabel) < instructions.indexOf(targetJump)) { //1 condition
                AbstractInsnNode cur = defaultHandlerStartLabel;
                while (cur != targetJump) {
                    if (cur.getOpcode() == Opcodes.GOTO) {
                        //noinspection ConstantConditions
                        if (((JumpInsnNode) cur).label == targetJump) { //fail of 2 condition
                            return lastFinallyInsExclusive;
                        }
                    }
                    cur = cur.getNext();
                }

                return prevLast;
            }
        }
        return lastFinallyInsExclusive;
    }

    @NotNull
    private List<TryCatchBlockNodePosition> findTryCatchBlocksInlinedInFinally(@NotNull FinallyBlockInfo finallyInfo) {
        List<TryCatchBlockNodePosition> result = new ArrayList<TryCatchBlockNodePosition>();
        Map<TryCatchBlockNodeInfo, TryCatchBlockNodePosition> processedBlocks = new HashMap<TryCatchBlockNodeInfo, TryCatchBlockNodePosition>();
        for (AbstractInsnNode curInstr = finallyInfo.startIns; curInstr != finallyInfo.endInsExclusive; curInstr = curInstr.getNext()) {
            if (!(curInstr instanceof LabelNode)) continue;

            LabelNode curLabel = (LabelNode) curInstr;
            List<TryCatchBlockNodeInfo> startedTryBlocks = tryBlockStarts.get(curLabel);
            if (startedTryBlocks != null) {
                for (TryCatchBlockNodeInfo block : startedTryBlocks) {
                    if (block.getOnlyCopyNotProcess()) {
                        assert !processedBlocks.containsKey(block) : "Try catch block already processed before start label!!! " + block;
                        TryCatchBlockNodePosition info = new TryCatchBlockNodePosition(block, TryCatchPosition.START);
                        processedBlocks.put(block, info);
                        result.add(info);
                    }
                }
            }

            List<TryCatchBlockNodeInfo> endedTryBlocks = tryBlockEnds.get(curLabel);
            if (endedTryBlocks == null) continue;

            for (TryCatchBlockNodeInfo block : endedTryBlocks) {
                if (block.getOnlyCopyNotProcess()) {
                    TryCatchBlockNodePosition info = processedBlocks.get(block);
                    if (info != null) {
                        assert info.getPosition() == TryCatchPosition.START;
                        info.setPosition(TryCatchPosition.INNER);
                    }
                    else {
                        info = new TryCatchBlockNodePosition(block, TryCatchPosition.END);
                        processedBlocks.put(block, info);
                        result.add(info);
                    }
                }
            }
        }
        return result;
    }

    private static void substituteReturnValueInFinally(
            int nonLocalVarIndex,
            @NotNull Type nonLocalReturnType,
            @NotNull MethodNode finallyBlockCopy,
            @NotNull Type localReturnType,
            boolean doPop
    ) {
        if (doPop && localReturnType != Type.VOID_TYPE) {
            AsmUtil.pop(finallyBlockCopy, localReturnType);
        }
        if (nonLocalReturnType != Type.VOID_TYPE) {
            finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ILOAD), nonLocalVarIndex);
        }
    }

    @Nullable
    private static AbstractInsnNode getPrevNoLineNumberOrLabel(@NotNull AbstractInsnNode node, boolean strict) {
        AbstractInsnNode result = strict ? node.getPrevious() : node;
        while (isLineNumberOrLabel(result)) {
            result = result.getPrevious();
        }
        return result;
    }

    public void sortTryCatchBlocks() {
        Comparator<TryCatchBlockNodeInfo> comp = new Comparator<TryCatchBlockNodeInfo>() {
            @Override
            public int compare(@NotNull TryCatchBlockNodeInfo t1, @NotNull TryCatchBlockNodeInfo t2) {
                int result = inlineFun.instructions.indexOf(t1.getNode().handler) - inlineFun.instructions.indexOf(t2.getNode().handler);
                if (result == 0) {
                    result = inlineFun.instructions.indexOf(t1.getNode().start) - inlineFun.instructions.indexOf(t2.getNode().start);
                    if (result == 0) {
                        assert false : "Error: support multicatch finallies!";
                        result = inlineFun.instructions.indexOf(t1.getNode().end) - inlineFun.instructions.indexOf(t2.getNode().end);
                    }
                }
                return result;
            }
        };
        Collections.sort(inlineFunTryBlockInfo, comp);
    }
}
