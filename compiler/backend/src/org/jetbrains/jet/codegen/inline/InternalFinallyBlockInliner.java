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

import com.google.common.base.Objects;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.util.Textifier;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
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

        public boolean isEmpty() {
            if (!(startIns instanceof LabelNode)) {
                return false;

            }
            AbstractInsnNode end = endInsExclusive;
            while (end != startIns && end instanceof LabelNode) {
                end = end.getPrevious();
            }
            return startIns == end;
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

        MaxLocalsCalculator tempCalcNode = new MaxLocalsCalculator(
                InlineCodegenUtil.API,
                inlineFun.access, inlineFun.desc, null
        );
        inlineFun.accept(tempCalcNode);
        return tempCalcNode.getMaxLocals();
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
            if (!InlineCodegenUtil.isReturnOpcode(curIns.getOpcode()) ||
                !InlineCodegenUtil.isMarkedReturn(curIns) ||
                coveringTryCatchBlocks.isEmpty() ||
                coveringTryCatchBlocks.get(0).getOnlyCopyNotProcess()) {
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
            List<TryBlockCluster<TryCatchBlockNodeInfo>> clusters = InlinePackage.doClustering(coveringTryCatchBlocks);
            ListIterator<TryBlockCluster<TryCatchBlockNodeInfo>> tryCatchBlockIterator = clusters.listIterator(clusters.size());

            checkClusterInvariant(clusters);


            //Reverse visiting cause innermost tryCatchBlocks in the end
            List<TryCatchBlockNodeInfo> patched = new ArrayList<TryCatchBlockNodeInfo>();
            while (tryCatchBlockIterator.hasPrevious()) {

                TryBlockCluster clusterToFindFinally = tryCatchBlockIterator.previous();
                List<TryCatchBlockNodeInfo> clusterBlocks = clusterToFindFinally.getBlocks();
                TryCatchBlockNodeInfo originalTryCatchBlock = clusterBlocks.get(0);

                FinallyBlockInfo finallyInfo = findFinallyBlockBody(originalTryCatchBlock, inlineFunTryBlockInfo);
                if (finallyInfo == null) continue;

                if (originalTryCatchBlock.getOnlyCopyNotProcess()) {
                    patched.addAll(clusterBlocks);
                    continue;
                }

                instructions.resetLabels();

                List<TryCatchBlockNodePosition> tryCatchBlockInlinedInFinally = findTryCatchBlocksInlinedInFinally(finallyInfo);

                //Keep some information about label nodes, we need it to understand whether it's jump inside finally block or outside
                // in first case we do call VISIT on instruction otherwise recreating jump instruction (see below)
                Set<LabelNode> labelsInsideFinally = rememberOriginalLabelNodes(finallyInfo);

                //Creating temp node for finally block copy with some additional instruction
                MethodNode finallyBlockCopy = createEmptyMethodNode();
                Label newFinallyStart = new Label();
                Label newFinallyEnd = new Label();
                Label insertedBlockEnd = new Label();

                if (nonLocalReturnType != Type.VOID_TYPE && !finallyInfo.isEmpty()) {
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
                if (nonLocalReturnType != Type.VOID_TYPE && !finallyInfo.isEmpty()) {
                    finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ILOAD), nextTempNonLocalVarIndex);
                    nextTempNonLocalVarIndex += nonLocalReturnType.getSize(); //TODO: do more wise indexing
                }

                finallyBlockCopy.visitLabel(insertedBlockEnd);

                //Copying finally body before non-local return instruction
                InlineCodegenUtil.insertNodeBefore(finallyBlockCopy, inlineFun, instrInsertFinallyBefore);

                updateExceptionTable(clusterBlocks, newFinallyStart, newFinallyEnd,
                                                tryCatchBlockInlinedInFinally, labelsInsideFinally, (LabelNode) insertedBlockEnd.info, patched);


            }

            }

            curIns = curIns.getPrevious();
            while (curIns != null && curIns != nextPrev) {
                updateCoveringTryBlocks(coveringTryCatchBlocks, curIns);
                curIns = curIns.getPrevious();
            }
            //curIns = nextPrev;
        }

        substitureTryBlockNodes();
    }

    private void checkCoveringBlocksInvariant(Stack<TryCatchBlockNodeInfo> coveringTryCatchBlocks) {
        boolean isWasOnlyLocal = false;
        for (TryCatchBlockNodeInfo info : coveringTryCatchBlocks) {
            assert !isWasOnlyLocal || info.getOnlyCopyNotProcess();
            if (info.getOnlyCopyNotProcess()) {
                isWasOnlyLocal = true;
            }
        }
    }

    private void checkClusterInvariant(List<TryBlockCluster<TryCatchBlockNodeInfo>> clusters) {
        boolean isWasOnlyLocal;
        isWasOnlyLocal = false;
        for (TryBlockCluster<TryCatchBlockNodeInfo> cluster : clusters) {
            TryCatchBlockNodeInfo info = cluster.getBlocks().get(0);
            assert !isWasOnlyLocal || info.getOnlyCopyNotProcess();
            if (info.getOnlyCopyNotProcess()) {
                isWasOnlyLocal = true;
            }
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
    private void updateExceptionTable(
            @NotNull List<TryCatchBlockNodeInfo> updatingClusterBlocks,
            @NotNull Label newFinallyStart,
            @NotNull Label newFinallyEnd,
            @NotNull List<TryCatchBlockNodePosition> tryCatchBlockPresentInFinally,
            @NotNull Set<LabelNode> labelsInsideFinally,
            @NotNull LabelNode insertedBlockEnd,
            @NotNull List<TryCatchBlockNodeInfo> patched
    ) {

        //copy tryCatchFinallies that totally in finally block
        List<TryBlockCluster<TryCatchBlockNodePosition>> clusters = InlinePackage.doClustering(tryCatchBlockPresentInFinally);
        Map<LabelNode, TryBlockCluster<TryCatchBlockNodePosition>> handler2Cluster = new HashMap<LabelNode, TryBlockCluster<TryCatchBlockNodePosition>>();

        for (TryBlockCluster<TryCatchBlockNodePosition> cluster : clusters) {
            List<TryCatchBlockNodePosition> clusterBlocks = cluster.getBlocks();
            TryCatchBlockNodePosition block0 = clusterBlocks.get(0);
            TryCatchPosition clusterPosition = block0.getPosition();
            if (clusterPosition == TryCatchPosition.INNER) {
                for (TryCatchBlockNodePosition position : clusterBlocks) {
                    assert clusterPosition == position.getPosition() : "Wrong inner tryCatchBlock structure";
                    TryCatchBlockNode tryCatchBlockNode = position.getNodeInfo().getNode();

                    assert inlineFun.instructions.indexOf(tryCatchBlockNode.start) <= inlineFun.instructions.indexOf(tryCatchBlockNode.end);

                    TryCatchBlockNode additionalTryCatchBlock =
                            new TryCatchBlockNode((LabelNode) tryCatchBlockNode.start.getLabel().info,
                                                  (LabelNode) tryCatchBlockNode.end.getLabel().info,
                                                  getNewOrOldLabel(tryCatchBlockNode.handler, labelsInsideFinally),
                                                  tryCatchBlockNode.type);


                    assert inlineFun.instructions.indexOf(additionalTryCatchBlock.start) <= inlineFun.instructions.indexOf(additionalTryCatchBlock.end);

                    TryCatchBlockNodeInfo newInfo = new TryCatchBlockNodeInfo(additionalTryCatchBlock, true);
                    tryBlockStarts.put(newInfo.getStartLabel(), newInfo);
                    tryBlockEnds.put(newInfo.getEndLabel(), newInfo);
                    inlineFunTryBlockInfo.add(newInfo);
                }
            }
            else if (clusterPosition == TryCatchPosition.END) {
                TryCatchBlockNodePosition defaultHandler = cluster.getDefaultHandler();
                assert defaultHandler != null : "Default handler should be present";
                handler2Cluster.put(defaultHandler.getHandler(), cluster);
            }
            else {
                assert clusterPosition == TryCatchPosition.START;
                TryCatchBlockNodePosition defaultHandler = cluster.getDefaultHandler();
                assert defaultHandler != null : "Default handler should be present";
                TryBlockCluster<TryCatchBlockNodePosition> endCluster = handler2Cluster.remove(defaultHandler.getHandler());
                assert endCluster != null : "Could find start cluster for  " + clusterPosition;

                //at this point only external finallies could occurs
                //they don't collision with updatingClusterBlocks, but may with external ones on next updateExceptionTable invocation
                Iterator<TryCatchBlockNodePosition> startBlockPositions = clusterBlocks.iterator();
                for (TryCatchBlockNodePosition endBlockPosition : endCluster.getBlocks()) {
                    TryCatchBlockNodeInfo startNode = startBlockPositions.next().getNodeInfo();
                    TryCatchBlockNodeInfo endNode = endBlockPosition.getNodeInfo();

                    assert Objects.equal(startNode.getType(), endNode.getType()) : "Different handler types : " + startNode.getType() + " " + endNode.getType();

                    patchTryBlocks((LabelNode) startNode.getStartLabel().getLabel().info, endNode);
                }
            }
        }

        if (handler2Cluster.size() == 1) {
            TryBlockCluster<TryCatchBlockNodePosition> singleCluster = handler2Cluster.values().iterator().next();
            if (singleCluster.getBlocks().get(0).getPosition() == TryCatchPosition.END) {
                //Pair that starts on default handler don't added to tryCatchBlockPresentInFinally cause it's out of finally block
                //TODO rewrite to clusters
                for (TryCatchBlockNodePosition endBlockPosition : singleCluster.getBlocks()) {
                    TryCatchBlockNodeInfo endNode = endBlockPosition.getNodeInfo();
                    patchTryBlocks((LabelNode) insertedBlockEnd.getLabel().info, endNode);
                    //nextPrev = (AbstractInsnNode) insertedBlockEnd.getLabel().info;
                }

                handler2Cluster.clear();
            }
        }
        assert handler2Cluster.isEmpty() : "Unmatched clusters " + handler2Cluster.size();

        List<TryCatchBlockNodeInfo > toProcess = new ArrayList<TryCatchBlockNodeInfo >();
        toProcess.addAll(patched);
        toProcess.addAll(updatingClusterBlocks);
        patched.clear();
        // Inserted finally shouldn't be handled by corresponding catches,
        // so we should split original interval by inserted finally one
        for (TryCatchBlockNodeInfo block : toProcess) {
            //update exception mapping
            LabelNode oldStartNode = block.getNode().start;
            block.getNode().start = (LabelNode) newFinallyEnd.info;
            tryBlockStarts.remove(oldStartNode, block);
            tryBlockStarts.put(block.getStartLabel(), block);

            //if (!block.getOnlyCopyNotProcess()) {
                patched.add(block);
            //}

            TryCatchBlockNode additionalTryCatchBlock =
                    new TryCatchBlockNode(oldStartNode, (LabelNode) newFinallyStart.info, block.getNode().handler, block.getNode().type);

            TryCatchBlockNodeInfo newInfo = new TryCatchBlockNodeInfo(additionalTryCatchBlock, block.getOnlyCopyNotProcess());
            tryBlockStarts.put(additionalTryCatchBlock.start, newInfo);
            tryBlockEnds.put(additionalTryCatchBlock.end, newInfo);

            inlineFunTryBlockInfo.add(newInfo);

            //TODO add assert
        }
        sortTryCatchBlocks(inlineFunTryBlockInfo);
    }

    private void patchTryBlocks(@NotNull LabelNode newStartLabelNode, @NotNull TryCatchBlockNodeInfo endNode) {
        LabelNode oldStart = endNode.getStartLabel();
        endNode.getNode().start = newStartLabelNode;
        tryBlockStarts.remove(oldStart, endNode);
        tryBlockStarts.put(endNode.getNode().start, endNode);


        TryCatchBlockNode endTryBlock = endNode.getNode();
        TryCatchBlockNode additionalTryCatchBlock =
                new TryCatchBlockNode(oldStart,
                                      (LabelNode) endTryBlock.end.getLabel().info,
                                      endTryBlock.handler,
                                      endTryBlock.type);

        TryCatchBlockNodeInfo newInfo = new TryCatchBlockNodeInfo(additionalTryCatchBlock, endNode.getOnlyCopyNotProcess());
        tryBlockStarts.put(newInfo.getStartLabel(), newInfo);
        tryBlockEnds.put(newInfo.getEndLabel(), newInfo);

        inlineFunTryBlockInfo.add(newInfo);
    }

    private static LabelNode getNewOrOldLabel(LabelNode oldHandler, @NotNull Set<LabelNode> labelsInsideFinally) {
        if (labelsInsideFinally.contains(oldHandler)) {
            return (LabelNode) oldHandler.getLabel().info;
        }

        return oldHandler;
    }

    //Keep information about try blocks that cover current instruction -
    // pushing and popping it to stack entering and exiting tryCatchBlock start and end labels
    private void updateCoveringTryBlocks(Stack<TryCatchBlockNodeInfo> coveringTryBlocks, AbstractInsnNode curIns) {
        if (!(curIns instanceof LabelNode)) return;

        List<TryCatchBlockNodeInfo> infos = tryBlockStarts.get((LabelNode) curIns);
        for (TryCatchBlockNodeInfo startNode : infos) {
            TryCatchBlockNodeInfo pop = coveringTryBlocks.pop();
            //Temporary disabled cause during patched structure of exceptions changed
            //assert startNode == pop : "Wrong try-catch structure " + startNode + " " + pop + " " + infos.size();
        }

        //Reversing list order cause we should pop external block before internal one
        // (originally internal blocks goes before external one, such invariant preserved via sortTryCatchBlocks method)
        for (TryCatchBlockNodeInfo info : Lists.reverse(tryBlockEnds.get((LabelNode) curIns))) {
            coveringTryBlocks.add(info);
        }

        checkCoveringBlocksInvariant(coveringTryBlocks);
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
        if (tryCatchBlock.getOnlyCopyNotProcess()) {
            AbstractInsnNode start = new LabelNode();
            AbstractInsnNode end = new LabelNode();
            InsnList insnList = new InsnList();
            insnList.add(start);
            insnList.add(end);
            return new FinallyBlockInfo(start, end);
        }

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

        FinallyBlockInfo finallyInfo = new FinallyBlockInfo(startFinallyChain.getNext(), endFinallyChainExclusive);

        if (inlineFun.instructions.indexOf(finallyInfo.startIns) > inlineFun.instructions.indexOf(finallyInfo.endInsExclusive)) {
            AbstractInsnNode startNode = finallyInfo.endInsExclusive;
            AbstractInsnNode stopNode = finallyInfo.startIns;
            writeNodes(startNode, stopNode);
            throw new AssertionError();
        }

        return finallyInfo;
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
                    assert !processedBlocks.containsKey(block) : "Try catch block already processed before start label!!! " + block;
                    TryCatchBlockNodePosition info = new TryCatchBlockNodePosition(block, TryCatchPosition.START);
                    processedBlocks.put(block, info);
                    result.add(info);
                }
            }

            List<TryCatchBlockNodeInfo> endedTryBlocks = tryBlockEnds.get(curLabel);
            if (endedTryBlocks == null) continue;

            for (TryCatchBlockNodeInfo block : endedTryBlocks) {
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
        return result;
    }

    private void writeNodes(AbstractInsnNode startNode, AbstractInsnNode stopNode) {
        Textifier p = new Textifier();
        TraceMethodVisitor visitor = new TraceMethodVisitor(p);
        while (startNode != stopNode) {
            startNode.accept(visitor);
            startNode = startNode.getNext();
        }
        startNode.accept(visitor);
        StringWriter out = new StringWriter();
        p.print(new PrintWriter(out));
        System.out.println(out.toString());
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

    private void sortTryCatchBlocks(@NotNull List<TryCatchBlockNodeInfo> inlineFunTryBlockInfo) {
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

        for (TryCatchBlockNodeInfo info : inlineFunTryBlockInfo) {
            TryCatchBlockNode node = info.getNode();
            assertNotEmptyTryNode(node);
        }
    }

    private void assertNotEmptyTryNode(TryCatchBlockNode node) {
        LabelNode start = node.start;
        AbstractInsnNode end = node.end;
        while (end != start && end instanceof LabelNode) {
            end = end.getPrevious();
        }
        assert start != end;
    }

    private static int counter = 0;

    private void flushCurrentState(@NotNull AbstractInsnNode curNonLocal) {
        substitureTryBlockNodes();

        System.out.println( );
        System.out.println();
        System.out.println("Iteration: " + counter++);
        System.out.println("Will process instruction at : " + inlineFun.instructions.indexOf(curNonLocal) + " " + curNonLocal.toString());
        String text = getNodeText(inlineFun);
        System.out.println(text);
    }

    private void substitureTryBlockNodes() {
        inlineFun.tryCatchBlocks.clear();
        for (TryCatchBlockNodeInfo info : inlineFunTryBlockInfo) {
            inlineFun.tryCatchBlocks.add(info.getNode());
        }
    }

    private int indexOf(AbstractInsnNode node) {
        return inlineFun.instructions.indexOf(node) + 1;
    }
}
