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
import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.util.Textifier;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtilsKt.*;
import static org.jetbrains.kotlin.codegen.inline.MethodInlinerUtilKt.getNextMeaningful;

public class InternalFinallyBlockInliner extends CoveringTryCatchNodeProcessor {

    private static class FinallyBlockInfo {

        final AbstractInsnNode startIns;

        final AbstractInsnNode endInsExclusive;

        private FinallyBlockInfo(
                @NotNull AbstractInsnNode inclusiveStart,
                @NotNull AbstractInsnNode exclusiveEnd
        ) {
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

    public static void processInlineFunFinallyBlocks(
            @NotNull MethodNode inlineFun,
            int lambdaTryCatchBlockNodes,
            int finallyParamOffset,
            boolean properFinallySplit
    ) {
        int index = 0;
        List<TryCatchBlockNodeInfo> inlineFunTryBlockInfo = new ArrayList<>();
        for (TryCatchBlockNode block : inlineFun.tryCatchBlocks) {
            inlineFunTryBlockInfo.add(new TryCatchBlockNodeInfo(block, index++ < lambdaTryCatchBlockNodes));
        }

        List<LocalVarNodeWrapper> localVars = new ArrayList<>();
        for (LocalVariableNode var : inlineFun.localVariables) {
            localVars.add(new LocalVarNodeWrapper(var));
        }

        if (hasFinallyBlocks(inlineFunTryBlockInfo)) {
            new InternalFinallyBlockInliner(inlineFun, inlineFunTryBlockInfo, localVars, finallyParamOffset, properFinallySplit)
                    .processInlineFunFinallyBlocks();
        }
    }

    @NotNull
    private final MethodNode inlineFun;
    private final boolean properFinallySplit;


    //lambdaTryCatchBlockNodes is number of TryCatchBlockNodes that was inlined with lambdas into function
    //due to code generation specific they placed before function TryCatchBlockNodes
    private InternalFinallyBlockInliner(
            @NotNull MethodNode inlineFun,
            @NotNull List<TryCatchBlockNodeInfo> inlineFunTryBlockInfo,
            @NotNull List<LocalVarNodeWrapper> localVariableInfo,
            int finallyParamOffset,
            boolean properFinallySplit
    ) {
        super(finallyParamOffset);
        this.inlineFun = inlineFun;
        this.properFinallySplit = properFinallySplit;
        for (TryCatchBlockNodeInfo block : inlineFunTryBlockInfo) {
            getTryBlocksMetaInfo().addNewInterval(block);
        }

        for (LocalVarNodeWrapper wrapper : localVariableInfo) {
            getLocalVarsMetaInfo().addNewInterval(wrapper);
        }
    }

    private int initAndGetVarIndexForNonLocalReturnValue() {
        MaxLocalsCalculator tempCalcNode = new MaxLocalsCalculator(
                Opcodes.API_VERSION,
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
            processInstruction(curIns, false);

            //At this point only global return is possible, local one already substituted with: goto endLabel
            if (!isReturnOpcode(curIns.getOpcode()) ||
                !isMarkedReturn(curIns)) {
                curIns = curIns.getPrevious();
                continue;
            }

            List<TryCatchBlockNodeInfo> currentCoveringNodesFromInnermost =
                    sortTryCatchBlocks(new ArrayList<>(getTryBlocksMetaInfo().getCurrentIntervals()));
            checkCoveringBlocksInvariant(Lists.reverse(currentCoveringNodesFromInnermost));

            if (currentCoveringNodesFromInnermost.isEmpty() ||
                currentCoveringNodesFromInnermost.get(currentCoveringNodesFromInnermost.size() - 1).getOnlyCopyNotProcess()) {
                curIns = curIns.getPrevious();
                continue;
            }

            AbstractInsnNode markedReturn = curIns;
            AbstractInsnNode instrInsertFinallyBefore = markedReturn.getPrevious();
            AbstractInsnNode nextPrev = instrInsertFinallyBefore.getPrevious();
            assert markedReturn.getNext() instanceof LabelNode : "Label should be occurred after non-local return";
            LabelNode newFinallyEnd = (LabelNode) markedReturn.getNext();
            Type nonLocalReturnType = getReturnType(markedReturn.getOpcode());

            //Generally there could be several tryCatch blocks (group) on one code interval (same start and end labels, but maybe different handlers) -
            // all of them refer to one try/*catches*/finally or try/catches.
            // Each group that corresponds to try/*catches*/finally contains tryCatch block with default handler.
            // For each such group we should insert corresponding finally before non-local return.
            // So we split all try blocks on current instructions to groups and process them independently
            List<TryBlockCluster<TryCatchBlockNodeInfo>> clustersFromInnermost = TryBlockClusteringKt.doClustering(
                    currentCoveringNodesFromInnermost);
            Iterator<TryBlockCluster<TryCatchBlockNodeInfo>> tryCatchBlockIterator = clustersFromInnermost.iterator();

            checkClusterInvariant(clustersFromInnermost);

            int originalDepthIndex = 0;
            List<TryCatchBlockNodeInfo> nestedUnsplitBlocksWithoutFinally = new ArrayList();
            while (tryCatchBlockIterator.hasNext()) {
                TryBlockCluster<TryCatchBlockNodeInfo> clusterToFindFinally = tryCatchBlockIterator.next();
                List<TryCatchBlockNodeInfo> clusterBlocks = clusterToFindFinally.getBlocks();
                TryCatchBlockNodeInfo nodeWithDefaultHandlerIfExists = clusterBlocks.get(clusterBlocks.size() - 1);

                FinallyBlockInfo finallyInfo =
                        findFinallyBlockBody(nodeWithDefaultHandlerIfExists, getTryBlocksMetaInfo().getAllIntervals());
                if (finallyInfo == null)  {
                    nestedUnsplitBlocksWithoutFinally.addAll(clusterToFindFinally.getBlocks());
                    continue;
                }

                if (nodeWithDefaultHandlerIfExists.getOnlyCopyNotProcess()) {
                    //lambdas finally generated before non-local return instruction,
                    //so it's a gap in try/catch handlers
                    throw new RuntimeException("Lambda try blocks should be skipped");
                }

                originalDepthIndex++;

                instructions.resetLabels();

                List<TryCatchBlockNodePosition> tryCatchBlockInlinedInFinally = findTryCatchBlocksInlinedInFinally(finallyInfo);

                //Creating temp node for finally block copy with some additional instruction
                MethodNode finallyBlockCopy = createEmptyMethodNode();
                Label newFinallyStart = new Label();
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

                    copyInstruction(finallyBlockCopy, currentIns, isInsOrJumpInsideFinally, originalDepthIndex);

                    currentIns = currentIns.getNext();
                }

                if (generateAloadAstore) {
                    finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ILOAD), nextTempNonLocalVarIndex);
                    nextTempNonLocalVarIndex += nonLocalReturnType.getSize(); //TODO: do more wise indexing
                }

                finallyBlockCopy.visitLabel(insertedBlockEnd);

                //Copying finally body before non-local return instruction
                insertNodeBefore(finallyBlockCopy, inlineFun, instrInsertFinallyBefore);

                nestedUnsplitBlocksWithoutFinally.addAll(clusterBlocks);

                updateExceptionTable(
                        properFinallySplit ? nestedUnsplitBlocksWithoutFinally : clusterBlocks, newFinallyStart, newFinallyEnd,
                        tryCatchBlockInlinedInFinally, labelsInsideFinally, (LabelNode) insertedBlockEnd.info
                );
                nestedUnsplitBlocksWithoutFinally.clear();
            }

            //skip just inserted finally
            curIns = markedReturn.getPrevious();
            while (curIns != null && curIns != nextPrev) {
                processInstruction(curIns, false);
                curIns = curIns.getPrevious();
            }

            //finally block inserted so we need split update localVarTable in lambda
            if (instrInsertFinallyBefore.getPrevious() != nextPrev && curIns != null) {
                LabelNode startNode = new LabelNode();
                LabelNode endNode = new LabelNode();
                instructions.insert(curIns, startNode);
                //TODO: note that on return expression we have no variables
                instructions.insert(markedReturn, endNode);
                getLocalVarsMetaInfo().splitCurrentIntervals(new SimpleInterval(startNode, endNode), true);
            }
        }

        substituteTryBlockNodes(inlineFun);
        substituteLocalVarTable(inlineFun);
    }

    private static void copyInstruction(
            @NotNull MethodNode finallyBlockCopy,
            @NotNull AbstractInsnNode currentIns,
            boolean isInsOrJumpInsideFinally,
            int depthShift
    ) {
        if (isInsOrJumpInsideFinally) {
            if (isFinallyMarker(currentIns.getNext())) {
                Integer constant = getConstant(currentIns);
                finallyBlockCopy.visitLdcInsn(constant + depthShift);
            }
            else {
                currentIns.accept(finallyBlockCopy); //VISIT
            }
        }
        else {
            //keep original jump: add currentIns clone
            finallyBlockCopy.instructions.add(new JumpInsnNode(currentIns.getOpcode(), ((JumpInsnNode) currentIns).label));
        }
    }

    private static void checkCoveringBlocksInvariant(@ReadOnly @NotNull List<TryCatchBlockNodeInfo> currentCoveringNodesFromOuterMost) {
        boolean isWasOnlyLocal = false;
        for (TryCatchBlockNodeInfo info : currentCoveringNodesFromOuterMost) {
            assert !isWasOnlyLocal || info.getOnlyCopyNotProcess() : "There are some problems with try-catch structure";
            isWasOnlyLocal = info.getOnlyCopyNotProcess();
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
        Set<LabelNode> labelsInsideFinally = new HashSet<>();
        for (AbstractInsnNode currentIns = finallyInfo.startIns;
             currentIns != finallyInfo.endInsExclusive;
             currentIns = currentIns.getNext()) {
            if (currentIns instanceof LabelNode) {
                labelsInsideFinally.add((LabelNode) currentIns);
            }
        }
        return labelsInsideFinally;
    }

    private void updateExceptionTable(
            @NotNull List<TryCatchBlockNodeInfo> updatingClusterBlocks,
            @NotNull Label newFinallyStart,
            @NotNull LabelNode newFinallyEnd,
            @NotNull List<TryCatchBlockNodePosition> tryCatchBlockPresentInFinally,
            @NotNull Set<LabelNode> labelsInsideFinally,
            @NotNull LabelNode insertedBlockEnd
    ) {

        //copy tryCatchFinallies that totally in finally block
        List<TryBlockCluster<TryCatchBlockNodePosition>> clusters = TryBlockClusteringKt.doClustering(tryCatchBlockPresentInFinally);
        Map<LabelNode, TryBlockCluster<TryCatchBlockNodePosition>> handler2Cluster = new HashMap<>();

        IntervalMetaInfo<TryCatchBlockNodeInfo> tryBlocksMetaInfo = getTryBlocksMetaInfo();
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


                    assert inlineFun.instructions.indexOf(additionalTryCatchBlock.start) <=
                           inlineFun.instructions.indexOf(additionalTryCatchBlock.end);

                    tryBlocksMetaInfo.addNewInterval(new TryCatchBlockNodeInfo(additionalTryCatchBlock, true));
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

                    assert Objects.equal(startNode.getType(), endNode.getType()) : "Different handler types : " +
                                                                                   startNode.getType() +
                                                                                   " " +
                                                                                   endNode.getType();

                    getTryBlocksMetaInfo()
                            .split(endNode, new SimpleInterval((LabelNode) endNode.getNode().end.getLabel().info,
                                                               (LabelNode) startNode.getStartLabel().getLabel().info), false);
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
                    getTryBlocksMetaInfo()
                            .split(endNode, new SimpleInterval((LabelNode) endNode.getNode().end.getLabel().info,
                                                               (LabelNode) insertedBlockEnd.getLabel().info), false);
                }

                handler2Cluster.clear();
            }
        }
        assert handler2Cluster.isEmpty() : "Unmatched clusters " + handler2Cluster.size();

        SimpleInterval splitBy = new SimpleInterval((LabelNode) newFinallyStart.info, newFinallyEnd);
        // Inserted finally shouldn't be handled by corresponding catches,
        // so we should split original interval by inserted finally one
        for (TryCatchBlockNodeInfo block : updatingClusterBlocks) {
            //update exception mapping
            SplitPair<TryCatchBlockNodeInfo> split = tryBlocksMetaInfo.splitAndRemoveIntervalFromCurrents(block, splitBy, false);
            checkFinally(split.getNewPart());
            checkFinally(split.getPatchedPart());
            //block patched in split method
            assert !block.isEmpty() : "Finally block should be non-empty";
            //TODO add assert
        }

        sortTryCatchBlocks(tryBlocksMetaInfo.getAllIntervals());
    }

    private static LabelNode getNewOrOldLabel(LabelNode oldHandler, @NotNull Set<LabelNode> labelsInsideFinally) {
        if (labelsInsideFinally.contains(oldHandler)) {
            return (LabelNode) oldHandler.getLabel().info;
        }

        return oldHandler;
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
            @ReadOnly @NotNull List<TryCatchBlockNodeInfo> tryCatchBlocks
    ) {
        List<TryCatchBlockNodeInfo> sameDefaultHandler = new ArrayList<>();
        LabelNode defaultHandler = null;
        boolean afterStartBlock = false;
        for (TryCatchBlockNodeInfo block : tryCatchBlocks) {
            if (tryCatchBlock == block) {
                afterStartBlock = true;
            }

            if (afterStartBlock) {
                if (block.getNode().type == null &&
                    (firstLabelInChain(tryCatchBlock.getNode().start) == firstLabelInChain(block.getNode().start) &&
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
        AbstractInsnNode meaningful = getNextMeaningful(startFinallyChain);
        assert meaningful != null : "Can't find meaningful in finally block" + startFinallyChain;

        Integer finallyDepth = getConstant(meaningful);
        AbstractInsnNode endFinallyChainExclusive = nextIntervalWithSameDefaultHandler.getNode().start;
        AbstractInsnNode current = meaningful.getNext();
        while (endFinallyChainExclusive != current) {
            current = current.getNext();
            if (isFinallyEnd(current)) {
                Integer currentDepth = getConstant(current.getPrevious());
                if (currentDepth.equals(finallyDepth)) {
                    endFinallyChainExclusive = current.getNext();
                    break;
                }
            }
        }

        FinallyBlockInfo finallyInfo = new FinallyBlockInfo(startFinallyChain.getNext(), endFinallyChainExclusive);

        checkFinally(finallyInfo);

        return finallyInfo;
    }

    private void checkFinally(FinallyBlockInfo finallyInfo) {
        checkFinally(finallyInfo.startIns, finallyInfo.endInsExclusive);
    }

    private void checkFinally(IntervalWithHandler intervalWithHandler) {
        checkFinally(intervalWithHandler.getStartLabel(), intervalWithHandler.getEndLabel());
    }

    private void checkFinally(AbstractInsnNode startIns, AbstractInsnNode endInsExclusive) {
        if (inlineFun.instructions.indexOf(startIns) >= inlineFun.instructions.indexOf(endInsExclusive)) {
            throw new AssertionError("Inconsistent finally: block end occurs before start " + traceInterval(endInsExclusive, startIns));
        }
    }

    @NotNull
    private List<TryCatchBlockNodePosition> findTryCatchBlocksInlinedInFinally(@NotNull FinallyBlockInfo finallyInfo) {
        List<TryCatchBlockNodePosition> result = new ArrayList<>();
        Map<TryCatchBlockNodeInfo, TryCatchBlockNodePosition> processedBlocks = new HashMap<>();

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

    @Override
    public int instructionIndex(@NotNull AbstractInsnNode inst) {
        return inlineFun.instructions.indexOf(inst);
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
        substituteTryBlockNodes(inlineFun);
        System.out.println("Will process instruction at : " + inlineFun.instructions.indexOf(curNonLocal) + " " + curNonLocal.toString());
        String text = getNodeText(inlineFun);
        System.out.println(text);
    }
}
