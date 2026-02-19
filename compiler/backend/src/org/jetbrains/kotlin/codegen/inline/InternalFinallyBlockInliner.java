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
import java.util.stream.Collectors;

import static org.jetbrains.kotlin.codegen.CodegenUtilKt.linkWithLabel;
import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtilsKt.*;
import static org.jetbrains.kotlin.codegen.inline.MethodInlinerUtilKt.getNextMeaningful;

public class InternalFinallyBlockInliner extends CoveringTryCatchNodeProcessor {

    private static class FinallyBlockInfo {

        final AbstractInsnNode startIns;
        final AbstractInsnNode endInsExclusive;

        final LabelNode closestLabelBeforeStart;
        final LabelNode closestLabelAfterEnd;

        private FinallyBlockInfo(
                @NotNull AbstractInsnNode inclusiveStart,
                @NotNull AbstractInsnNode exclusiveEnd,
                @NotNull LabelNode closestLabelBeforeStart,
                @NotNull LabelNode closestLabelAfterEnd
        ) {
            this.startIns = inclusiveStart;
            this.endInsExclusive = exclusiveEnd;
            this.closestLabelBeforeStart = closestLabelBeforeStart;
            this.closestLabelAfterEnd = closestLabelAfterEnd;
        }

        // TODO seems incorrect and maybe useless: exclusive ends shall not be checked, misses "marker" instruction checks
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
            AbstractInsnNode instrInsertFinallyBefore = markedReturn.getPrevious(); // marker of non-local return
            AbstractInsnNode nextPrev = instrInsertFinallyBefore.getPrevious();
            assert markedReturn.getNext() instanceof LabelNode : "Label should be occurred after non-local return";
            LabelNode newFinallyEnd = (LabelNode) markedReturn.getNext();
            AbstractInsnNode instructionAfterReturn = newFinallyEnd.getNext();
            Integer lineNumberAfterReturn = getLineNumberOrNull(instructionAfterReturn);
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
            List<TryCatchBlockNodeInfo> nestedUnsplitBlocksWithoutFinally = new ArrayList<>();
            List<LocalVarNodeWrapper> newLocalVarIntervals = new ArrayList<>();
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
                LabelNode newFinallyStart = linkWithLabel(new LabelNode());
                LabelNode insertedBlockEnd = linkWithLabel(new LabelNode());

                boolean generateAloadAstore = nonLocalReturnType != Type.VOID_TYPE && !finallyInfo.isEmpty();
                if (generateAloadAstore) {
                    finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ISTORE), nextTempNonLocalVarIndex);
                }
                finallyBlockCopy.visitLabel(newFinallyStart.getLabel());

                //Keep some information about label nodes, we need it to understand whether it's jump inside finally block or outside
                // in first case we do call VISIT on instruction otherwise recreating jump instruction (see below)
                Set<LabelNode> labelsInsideFinally = rememberOriginalLabelNodes(finallyInfo);
                List<LocalVarNodeWrapper> varsInsideFinally =
                        getLocalVarsMetaInfo().copyIntervalsForRange(instructions, finallyInfo.closestLabelBeforeStart,
                                                                     finallyInfo.closestLabelAfterEnd);
                //Writing finally block body to temporary node
                AbstractInsnNode currentIns = finallyInfo.startIns;
                while (currentIns != finallyInfo.endInsExclusive) {
                    boolean isInsOrJumpInsideFinally =
                            !(currentIns instanceof JumpInsnNode) ||
                            labelsInsideFinally.contains(((JumpInsnNode) currentIns).label);

                    copyInstruction(finallyBlockCopy, currentIns, isInsOrJumpInsideFinally, originalDepthIndex);
                    currentIns = currentIns.getNext();
                }
                Map<LabelNode, LabelNode> labelsInsideFinallyOldToNew = labelsInsideFinally.stream()
                        .collect(Collectors.toMap(l -> l, l -> (LabelNode) l.getLabel().info));

                if (generateAloadAstore) {
                    finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ILOAD), nextTempNonLocalVarIndex);
                    nextTempNonLocalVarIndex += nonLocalReturnType.getSize(); //TODO: do more wise indexing
                }

                finallyBlockCopy.visitLabel(insertedBlockEnd.getLabel());

                // after all finally-blocks are inlined, the resulting interval is removed from current local vars
                // then, we need to add new intervals there. As `varsInsideFinally` contains copies of intervals, we can just remap them
                varsInsideFinally.forEach(i -> {
                    i.remapLabel(finallyInfo.closestLabelBeforeStart, newFinallyStart);
                    labelsInsideFinallyOldToNew.forEach(i::remapLabel);
                    i.remapLabel(finallyInfo.closestLabelAfterEnd, insertedBlockEnd);
                });
                newLocalVarIntervals.addAll(varsInsideFinally);

                //Copying finally body before non-local return instruction
                insertNodeBefore(finallyBlockCopy, inlineFun, instrInsertFinallyBefore);
                instructions.resetLabels(); // we do not need mapping from old to new labels except `labelsInsideFinallyOldToNew`

                // apply line number for inlined copy of finally block if needed
                AbstractInsnNode copiedFinallyStart = finallyBlockCopy.instructions.getFirst();
                Integer finallyLineNumber = getFirstFinallyOperationLineNumberOrNull(finallyInfo.startIns, finallyInfo.endInsExclusive);
                Integer finallyCopyLineNumber = getFirstFinallyOperationLineNumberOrNull(copiedFinallyStart, instrInsertFinallyBefore);
                if (finallyLineNumber != null && !finallyLineNumber.equals(finallyCopyLineNumber)) {
                    LabelNode label = new LabelNode();
                    inlineFun.instructions.insertBefore(copiedFinallyStart, label);
                    inlineFun.instructions.insertBefore(copiedFinallyStart, new LineNumberNode(finallyLineNumber, label));
                }

                nestedUnsplitBlocksWithoutFinally.addAll(clusterBlocks);

                updateExceptionTable(
                        properFinallySplit ? nestedUnsplitBlocksWithoutFinally : clusterBlocks, newFinallyStart, newFinallyEnd,
                        tryCatchBlockInlinedInFinally, labelsInsideFinallyOldToNew, insertedBlockEnd
                );
                nestedUnsplitBlocksWithoutFinally.clear();
            }

            // if the insertion of finally-blocks has changed the line number of the following code, restore it
            if (lineNumberAfterReturn != null && !lineNumberAfterReturn.equals(getLineNumberOrNull(instructionAfterReturn))) {
                LabelNode label = new LabelNode();
                inlineFun.instructions.insertBefore(instructionAfterReturn, label);
                inlineFun.instructions.insertBefore(instructionAfterReturn, new LineNumberNode(lineNumberAfterReturn, label));
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

                newLocalVarIntervals.forEach(varInterval -> {
                    // to keep debugger helper synthetic vars info consistent, we need to merge new intervals with current ones, if possible
                    LabelNode startLabelInChain = firstLabelInChain(varInterval.getStartLabel());
                    Optional<LocalVarNodeWrapper> toMerge = getLocalVarsMetaInfo().getCurrentIntervals().stream()
                            .filter(wrapper -> wrapper.getNode().name.equals(varInterval.getNode().name))
                            .filter(wrapper -> firstLabelInChain(wrapper.getEndLabel()).equals(startLabelInChain))
                            .findFirst();
                    if (toMerge.isPresent()) {
                        toMerge.get().getNode().end = varInterval.getEndLabel();
                    } else {
                        getLocalVarsMetaInfo().addNewInterval(varInterval);
                    }
                });
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
            } else {
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
            @NotNull LabelNode newFinallyStart,
            @NotNull LabelNode newFinallyEnd,
            @NotNull List<TryCatchBlockNodePosition> tryCatchBlockPresentInFinally,
            @NotNull Map<LabelNode, LabelNode> labelsInsideFinallyOldToNew,
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
                            new TryCatchBlockNode(getNewLabel(tryCatchBlockNode.start, labelsInsideFinallyOldToNew),
                                                  getNewLabel(tryCatchBlockNode.end, labelsInsideFinallyOldToNew),
                                                  getNewOrOldLabel(tryCatchBlockNode.handler, labelsInsideFinallyOldToNew),
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

                    assert Objects.equals(startNode.getType(), endNode.getType()) : "Different handler types : " +
                                                                                   startNode.getType() +
                                                                                   " " +
                                                                                   endNode.getType();

                    getTryBlocksMetaInfo().split(endNode,
                                                 new SimpleInterval(
                                                         getNewLabel(endNode.getNode().end, labelsInsideFinallyOldToNew),
                                                         getNewLabel(startNode.getStartLabel(), labelsInsideFinallyOldToNew)),
                                                 false);
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
                            .split(endNode, new SimpleInterval(
                                    getNewLabel(endNode.getNode().end, labelsInsideFinallyOldToNew),
                                    insertedBlockEnd),
                               false);
                }

                handler2Cluster.clear();
            }
        }
        assert handler2Cluster.isEmpty() : "Unmatched clusters " + handler2Cluster.size();

        SimpleInterval splitBy = new SimpleInterval(newFinallyStart, newFinallyEnd);
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

    private static LabelNode getNewOrOldLabel(LabelNode oldHandler, @NotNull Map<LabelNode, LabelNode> labelsInsideFinallyOldToNew) {
        return labelsInsideFinallyOldToNew.getOrDefault(oldHandler, oldHandler);
    }

    private static LabelNode getNewLabel(LabelNode old, @NotNull Map<LabelNode, LabelNode> oldToNew) {
        assert oldToNew.containsKey(old);
        return oldToNew.get(old);
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
        LabelNode startFinallyChain = tryCatchBlock.getNode().end;
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

        // there is always some label after the end by construction (as the initial "endFinallyChainExclusive" is a label)
        LabelNode labelAfterEnd = Objects.requireNonNull(findClosestLabel(endFinallyChainExclusive));
        // we shall not use `startFinallyChain` as finallyInfo.startInsn, as findTryCatchBlocksInlinedInFinally() would give wrong result
        AbstractInsnNode startFinallyInclusive = startFinallyChain.getNext();
        FinallyBlockInfo finallyInfo =
                new FinallyBlockInfo(startFinallyInclusive, endFinallyChainExclusive, startFinallyChain, labelAfterEnd);

        checkFinally(finallyInfo);
        return finallyInfo;
    }

    // If the given instruction is a label, returns it; otherwise, finds and returns the first next label in the instructions chain
    private static LabelNode findClosestLabel(AbstractInsnNode insn) {
        while (insn != null) {
            if (insn instanceof LabelNode) {
                return (LabelNode) insn;
            }
            insn = insn.getNext();
        }
        return null;
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
        System.out.println("Will process instruction at : " + inlineFun.instructions.indexOf(curNonLocal) + " " + curNonLocal);
        String text = getNodeText(inlineFun);
        System.out.println(text);
    }
}
