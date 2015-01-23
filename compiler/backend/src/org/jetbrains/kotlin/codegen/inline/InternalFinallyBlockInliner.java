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

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.util.Textifier;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil.*;

public class InternalFinallyBlockInliner extends CoveringTryCatchNodeProcessor<TryCatchBlockNodeInfo> {

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


    //lambdaTryCatchBlockNodes is number of TryCatchBlockNodes that was inlined with lambdas into function
    //due to code generation specific they placed before function TryCatchBlockNodes
    private InternalFinallyBlockInliner(@NotNull MethodNode inlineFun, List<TryCatchBlockNodeInfo> inlineFunTryBlockInfo) {
        this.inlineFun = inlineFun;
        for (TryCatchBlockNodeInfo block : inlineFunTryBlockInfo) {
            addNewTryCatchNode(block);
        }
    }

    private int initAndGetVarIndexForNonLocalReturnValue() {
        //sortTryCatchBlocks();/*TODO maybe remove*/

        MaxLocalsCalculator tempCalcNode = new MaxLocalsCalculator(
                InlineCodegenUtil.API,
                inlineFun.access, inlineFun.desc, null
        );
        inlineFun.accept(tempCalcNode);
        return tempCalcNode.getMaxLocals();
    }

    private void processInlineFunFinallyBlocks() {
        int nextTempNonLocalVarIndex = initAndGetVarIndexForNonLocalReturnValue();

        InsnList instructions = inlineFun.instructions;

        //As we do finally block code search after non-local return instruction
        // we should be sure that all others non-local returns already processed in this finally block.
        // So we do instruction processing in reverse order!
        AbstractInsnNode curIns = instructions.getLast();
        while (curIns != null) {
            updateCoveringTryBlocks(curIns);

            //At this point only global return is possible, local one already substituted with: goto endLabel
            if (!InlineCodegenUtil.isReturnOpcode(curIns.getOpcode()) ||
                !InlineCodegenUtil.isMarkedReturn(curIns)) {
                curIns = curIns.getPrevious();
                continue;
            }

            List<TryCatchBlockNodeInfo> currentCoveringNodes = getCoveringFromInnermost();
            checkCoveringBlocksInvariant(currentCoveringNodes);
            if (currentCoveringNodes.isEmpty() ||
                currentCoveringNodes.get(currentCoveringNodes.size() - 1).getOnlyCopyNotProcess()) {
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
            List<TryBlockCluster<TryCatchBlockNodeInfo>> clustersFromInnermost = InlinePackage.doClustering(currentCoveringNodes);
            Iterator<TryBlockCluster<TryCatchBlockNodeInfo>> tryCatchBlockIterator = clustersFromInnermost.iterator();

            checkClusterInvariant(clustersFromInnermost);

            List<TryCatchBlockNodeInfo> additionalNodesToSplit = new ArrayList<TryCatchBlockNodeInfo>();
            while (tryCatchBlockIterator.hasNext()) {
                TryBlockCluster clusterToFindFinally = tryCatchBlockIterator.next();
                List<TryCatchBlockNodeInfo> clusterBlocks = clusterToFindFinally.getBlocks();
                TryCatchBlockNodeInfo nodeWithDefaultHandlerIfExists = clusterBlocks.get(clusterBlocks.size() - 1);

                FinallyBlockInfo finallyInfo = findFinallyBlockBody(nodeWithDefaultHandlerIfExists, getAllTryCatchNodes());
                if (finallyInfo == null) continue;

                if (nodeWithDefaultHandlerIfExists.getOnlyCopyNotProcess()) {
                    additionalNodesToSplit.addAll(clusterBlocks);
                    continue;
                }

                instructions.resetLabels();

                List<TryCatchBlockNodePosition> tryCatchBlockInlinedInFinally = findTryCatchBlocksInlinedInFinally(finallyInfo);

                //Creating temp node for finally block copy with some additional instruction
                MethodNode finallyBlockCopy = createEmptyMethodNode();
                Label newFinallyStart = new Label();
                Label newFinallyEnd = new Label();
                Label insertedBlockEnd = new Label();

                boolean generateAloadAstore = nonLocalReturnType != Type.VOID_TYPE && !finallyInfo.isEmpty();
                if (generateAloadAstore) {
                    finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ISTORE), nextTempNonLocalVarIndex);
                }
                finallyBlockCopy.visitLabel(newFinallyStart);

                //Keep some information about label nodes, we need it to understand whether it's jump inside finally block or outside
                // in first case we do call VISIT on instruction otherwise recreating jump instruction (see below)
                Set<LabelNode> labelsInsideFinally = rememberOriginalLabelNodes(finallyInfo);
                //Writing finally block body to temporary node
                AbstractInsnNode currentIns = finallyInfo.startIns;
                while (currentIns != finallyInfo.endInsExclusive) {
                    boolean isInsOrJumpInsideFinally =
                            !(currentIns instanceof JumpInsnNode) ||
                            labelsInsideFinally.contains(((JumpInsnNode) currentIns).label);

                    copyInstruction(nextTempNonLocalVarIndex, curIns, instrInsertFinallyBefore, nonLocalReturnType, finallyBlockCopy,
                                    currentIns, isInsOrJumpInsideFinally);

                    currentIns = currentIns.getNext();
                }

                finallyBlockCopy.visitLabel(newFinallyEnd);
                if (generateAloadAstore) {
                    finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ILOAD), nextTempNonLocalVarIndex);
                    nextTempNonLocalVarIndex += nonLocalReturnType.getSize(); //TODO: do more wise indexing
                }

                finallyBlockCopy.visitLabel(insertedBlockEnd);

                //Copying finally body before non-local return instruction
                InlineCodegenUtil.insertNodeBefore(finallyBlockCopy, inlineFun, instrInsertFinallyBefore);

                updateExceptionTable(clusterBlocks, newFinallyStart, newFinallyEnd,
                                     tryCatchBlockInlinedInFinally, labelsInsideFinally, (LabelNode) insertedBlockEnd.info, additionalNodesToSplit);
            }

            //skip just inserted finally
            curIns = curIns.getPrevious();
            while (curIns != null && curIns != nextPrev) {
                updateCoveringTryBlocks(curIns);
                curIns = curIns.getPrevious();
            }
        }

        substituteTryBlockNodes();
    }

    private static void copyInstruction(
            int nextTempNonLocalVarIndex,
            @NotNull AbstractInsnNode curIns,
            @NotNull AbstractInsnNode instrInsertFinallyBefore,
            @NotNull Type nonLocalReturnType,
            @NotNull MethodNode finallyBlockCopy,
            @NotNull AbstractInsnNode currentIns,
            boolean isInsOrJumpInsideFinally
    ) {
        //This condition allows another model for non-local returns processing
        if (false) {
            boolean isReturnForSubstitution =
                    InlineCodegenUtil.isReturnOpcode(currentIns.getOpcode()) && !InlineCodegenUtil.isMarkedReturn(currentIns);
            if (!isInsOrJumpInsideFinally || isReturnForSubstitution) {
                //substitute all local returns and jumps outside finally with non-local return
                Type localReturnType = InlineCodegenUtil.getReturnType(currentIns.getOpcode());
                substituteReturnValueInFinally(nextTempNonLocalVarIndex, nonLocalReturnType, finallyBlockCopy,
                                               localReturnType, isReturnForSubstitution);

                instrInsertFinallyBefore.accept(finallyBlockCopy);
                curIns.accept(finallyBlockCopy);
            } else {
                currentIns.accept(finallyBlockCopy); //VISIT
            }
        }
        else {
            if (isInsOrJumpInsideFinally) {
                currentIns.accept(finallyBlockCopy); //VISIT
            }
            else {
                //keep original jump: add currentIns clone
                finallyBlockCopy.instructions.add(new JumpInsnNode(currentIns.getOpcode(), ((JumpInsnNode) currentIns).label));
            }
        }
    }

    private static void checkCoveringBlocksInvariant(List<TryCatchBlockNodeInfo> coveringTryCatchBlocks) {
        boolean isWasOnlyLocal = false;
        for (TryCatchBlockNodeInfo info : Lists.reverse(coveringTryCatchBlocks)) {
            assert !isWasOnlyLocal || info.getOnlyCopyNotProcess();
            if (info.getOnlyCopyNotProcess()) {
                isWasOnlyLocal = true;
            }
        }
    }

    private static void checkClusterInvariant(List<TryBlockCluster<TryCatchBlockNodeInfo>> clusters) {
        boolean isWasOnlyLocal;
        isWasOnlyLocal = false;
        for (TryBlockCluster<TryCatchBlockNodeInfo> cluster : Lists.reverse(clusters)) {
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
                    addNewTryCatchNode(newInfo);
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

        List<TryCatchBlockNodeInfo > toProcess = new ArrayList<TryCatchBlockNodeInfo>();
        toProcess.addAll(patched);
        toProcess.addAll(updatingClusterBlocks);
        patched.clear();
        // Inserted finally shouldn't be handled by corresponding catches,
        // so we should split original interval by inserted finally one
        for (TryCatchBlockNodeInfo block : toProcess) {
            //update exception mapping
            LabelNode oldStartNode = block.getNode().start;
            block.getNode().start = (LabelNode) newFinallyEnd.info;
            remapStartLabel(oldStartNode, block);

            patched.add(block);

            TryCatchBlockNode additionalTryCatchBlock =
                    new TryCatchBlockNode(oldStartNode, (LabelNode) newFinallyStart.info, block.getNode().handler, block.getNode().type);

            TryCatchBlockNodeInfo newInfo = new TryCatchBlockNodeInfo(additionalTryCatchBlock, block.getOnlyCopyNotProcess());
            addNewTryCatchNode(newInfo);

            //TODO add assert
        }
        sortTryCatchBlocks();
    }

    private void patchTryBlocks(@NotNull LabelNode newStartLabelNode, @NotNull TryCatchBlockNodeInfo endNode) {
        LabelNode oldStart = endNode.getStartLabel();
        endNode.getNode().start = newStartLabelNode;
        remapStartLabel(oldStart, endNode);

        TryCatchBlockNode endTryBlock = endNode.getNode();
        TryCatchBlockNode additionalTryCatchBlock =
                new TryCatchBlockNode(oldStart,
                                      (LabelNode) endTryBlock.end.getLabel().info,
                                      endTryBlock.handler,
                                      endTryBlock.type);

        TryCatchBlockNodeInfo newInfo = new TryCatchBlockNodeInfo(additionalTryCatchBlock, endNode.getOnlyCopyNotProcess());
        addNewTryCatchNode(newInfo);
    }

    private static LabelNode getNewOrOldLabel(LabelNode oldHandler, @NotNull Set<LabelNode> labelsInsideFinally) {
        if (labelsInsideFinally.contains(oldHandler)) {
            return (LabelNode) oldHandler.getLabel().info;
        }

        return oldHandler;
    }

    //Keep information about try blocks that cover current instruction -
    // pushing and popping it to stack entering and exiting tryCatchBlock start and end labels
    public void updateCoveringTryBlocks(@NotNull AbstractInsnNode curIns) {
        super.updateCoveringTryBlocks(curIns, false);
        checkCoveringBlocksInvariant(getCoveringFromInnermost());
    }

    private static boolean hasFinallyBlocks(List<TryCatchBlockNodeInfo> inlineFunTryBlockInfo) {
        for (TryCatchBlockNodeInfo block : inlineFunTryBlockInfo) {
            if (!block.getOnlyCopyNotProcess() && block.getNode().type == null) {
                return true;
            }
        }
        return false;
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
        AbstractInsnNode endFinallyChainExclusive = skipLastGotoIfNeeded(nextIntervalWithSameDefaultHandler.getNode().start);

        FinallyBlockInfo finallyInfo = new FinallyBlockInfo(startFinallyChain.getNext(), endFinallyChainExclusive);

        if (inlineFun.instructions.indexOf(finallyInfo.startIns) > inlineFun.instructions.indexOf(finallyInfo.endInsExclusive)) {
            throw new AssertionError("Inconsistent finally: block end occurs before start " + traceInterval(finallyInfo.endInsExclusive, finallyInfo.startIns));
        }

        return finallyInfo;
    }

    @NotNull
    private static AbstractInsnNode skipLastGotoIfNeeded(
            @NotNull AbstractInsnNode lastFinallyInsExclusive
    ) {

        AbstractInsnNode prevLast = getPrevNoLineNumberOrLabel(lastFinallyInsExclusive, true);
        assert prevLast != null : "Empty finally block: " + lastFinallyInsExclusive;

        if (InlineCodegenUtil.isGoToTryCatchBlockEnd(prevLast)) {
            return prevLast.getPrevious();
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
            List<TryCatchBlockNodeInfo> startedTryBlocks = getStartNodes(curLabel);
            for (TryCatchBlockNodeInfo block : startedTryBlocks) {
                assert !processedBlocks.containsKey(block) : "Try catch block already processed before start label!!! " + block;
                TryCatchBlockNodePosition info = new TryCatchBlockNodePosition(block, TryCatchPosition.START);
                processedBlocks.put(block, info);
                result.add(info);
            }

            List<TryCatchBlockNodeInfo> endedTryBlocks = getEndNodes(curLabel);

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

    @Override
    public int instructionIndex(@NotNull AbstractInsnNode inst) {
        return inlineFun.instructions.indexOf(inst);
    }

    private void substituteTryBlockNodes() {
        inlineFun.tryCatchBlocks.clear();
        for (TryCatchBlockNodeInfo info : getNonEmptyNodes()) {
            inlineFun.tryCatchBlocks.add(info.getNode());
        }
    }

    private static String traceInterval(AbstractInsnNode startNode, AbstractInsnNode stopNode) {
        Textifier p = new Textifier();
        TraceMethodVisitor visitor = new TraceMethodVisitor(p);
        while (startNode != stopNode) {
            startNode.accept(visitor);
            startNode = startNode.getNext();
        }
        startNode.accept(visitor);
        StringWriter out = new StringWriter();
        p.print(new PrintWriter(out));
        return out.toString();
    }

    @SuppressWarnings({"UnusedDeclaration", "UseOfSystemOutOrSystemErr"})
    @TestOnly
    private void flushCurrentState(@NotNull AbstractInsnNode curNonLocal) {
        substituteTryBlockNodes();
        System.out.println("Will process instruction at : " + inlineFun.instructions.indexOf(curNonLocal) + " " + curNonLocal.toString());
        String text = getNodeText(inlineFun);
        System.out.println(text);
    }

}
