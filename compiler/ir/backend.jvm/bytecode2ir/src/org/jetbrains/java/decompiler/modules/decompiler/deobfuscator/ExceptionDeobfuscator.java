// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.deobfuscator;

import org.jetbrains.java.decompiler.code.*;
import org.jetbrains.java.decompiler.code.cfg.BasicBlock;
import org.jetbrains.java.decompiler.code.cfg.ControlFlowGraph;
import org.jetbrains.java.decompiler.code.cfg.ExceptionRangeCFG;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.modules.decompiler.decompose.GenericDominatorEngine;
import org.jetbrains.java.decompiler.modules.decompiler.decompose.IGraph;
import org.jetbrains.java.decompiler.modules.decompiler.decompose.IGraphNode;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.util.*;
import java.util.Map.Entry;

public final class ExceptionDeobfuscator {

  private static final class Range {
    private final BasicBlock handler;
    private final String uniqueStr;
    private final Set<BasicBlock> protectedRange;
    private final ExceptionRangeCFG rangeCFG;

    private Range(BasicBlock handler, String uniqueStr, Set<BasicBlock> protectedRange, ExceptionRangeCFG rangeCFG) {
      this.handler = handler;
      this.uniqueStr = uniqueStr;
      this.protectedRange = protectedRange;
      this.rangeCFG = rangeCFG;
    }
  }

  public static void restorePopRanges(ControlFlowGraph graph) {

    List<Range> lstRanges = new ArrayList<>();

    // aggregate ranges
    for (ExceptionRangeCFG range : graph.getExceptions()) {
      boolean found = false;
      for (Range arr : lstRanges) {
        if (arr.handler == range.getHandler() && InterpreterUtil.equalObjects(range.getUniqueExceptionsString(), arr.uniqueStr)) {
          arr.protectedRange.addAll(range.getProtectedRange());
          found = true;
          break;
        }
      }

      if (!found) {
        // doesn't matter, which range chosen
        lstRanges.add(new Range(range.getHandler(), range.getUniqueExceptionsString(), new HashSet<>(range.getProtectedRange()), range));
      }
    }

    // process aggregated ranges
    for (Range range : lstRanges) {

      if (range.uniqueStr != null) {

        BasicBlock handler = range.handler;
        InstructionSequence seq = handler.getSeq();

        Instruction firstinstr;
        if (seq.length() > 0) {
          firstinstr = seq.getInstr(0);

          if (firstinstr.opcode == CodeConstants.opc_pop ||
              firstinstr.opcode == CodeConstants.opc_astore) {
            Set<BasicBlock> setrange = new HashSet<>(range.protectedRange);

            for (Range range_super : lstRanges) { // finally or strict superset

              if (range != range_super) {

                Set<BasicBlock> setrange_super = new HashSet<>(range_super.protectedRange);

                if (!setrange.contains(range_super.handler) && !setrange_super.contains(handler)
                    && (range_super.uniqueStr == null || setrange_super.containsAll(setrange))) {

                  if (range_super.uniqueStr == null) {
                    setrange_super.retainAll(setrange);
                  }
                  else {
                    setrange_super.removeAll(setrange);
                  }

                  if (!setrange_super.isEmpty()) {

                    BasicBlock newblock = handler;

                    // split the handler
                    if (seq.length() > 1) {
                      newblock = new BasicBlock(++graph.last_id);
                      InstructionSequence newseq = new SimpleInstructionSequence();
                      newseq.addInstruction(firstinstr.clone(), -1);

                      newblock.setSeq(newseq);
                      graph.getBlocks().addWithKey(newblock, newblock.id);


                      List<BasicBlock> lstTemp = new ArrayList<>();
                      lstTemp.addAll(handler.getPreds());
                      lstTemp.addAll(handler.getPredExceptions());

                      // replace predecessors
                      for (BasicBlock pred : lstTemp) {
                        pred.replaceSuccessor(handler, newblock);
                      }

                      // replace handler
                      for (ExceptionRangeCFG range_ext : graph.getExceptions()) {
                        if (range_ext.getHandler() == handler) {
                          range_ext.setHandler(newblock);
                        }
                        else if (range_ext.getProtectedRange().contains(handler)) {
                          newblock.addSuccessorException(range_ext.getHandler());
                          range_ext.getProtectedRange().add(newblock);
                        }
                      }

                      newblock.addSuccessor(handler);
                      if (graph.getFirst() == handler) {
                        graph.setFirst(newblock);
                      }

                      // remove the first pop in the handler
                      seq.removeInstruction(0);
                    }

                    newblock.addSuccessorException(range_super.handler);
                    range_super.rangeCFG.getProtectedRange().add(newblock);

                    handler = range.rangeCFG.getHandler();
                    seq = handler.getSeq();
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  public static void insertEmptyExceptionHandlerBlocks(ControlFlowGraph graph) {

    Set<BasicBlock> setVisited = new HashSet<>();

    for (ExceptionRangeCFG range : graph.getExceptions()) {
      BasicBlock handler = range.getHandler();

      if (setVisited.contains(handler)) {
        continue;
      }
      setVisited.add(handler);

      BasicBlock emptyblock = new BasicBlock(++graph.last_id);
      graph.getBlocks().addWithKey(emptyblock, emptyblock.id);

      // only exception predecessors considered
      List<BasicBlock> lstTemp = new ArrayList<>(handler.getPredExceptions());

      // replace predecessors
      for (BasicBlock pred : lstTemp) {
        pred.replaceSuccessor(handler, emptyblock);
      }

      // replace handler
      for (ExceptionRangeCFG range_ext : graph.getExceptions()) {
        if (range_ext.getHandler() == handler) {
          range_ext.setHandler(emptyblock);
        }
        else if (range_ext.getProtectedRange().contains(handler)) {
          emptyblock.addSuccessorException(range_ext.getHandler());
          range_ext.getProtectedRange().add(emptyblock);
        }
      }

      emptyblock.addSuccessor(handler);
      if (graph.getFirst() == handler) {
        graph.setFirst(emptyblock);
      }
    }
  }

  public static void removeEmptyRanges(ControlFlowGraph graph) {

    List<ExceptionRangeCFG> lstRanges = graph.getExceptions();
    for (int i = lstRanges.size() - 1; i >= 0; i--) {
      ExceptionRangeCFG range = lstRanges.get(i);

      boolean isEmpty = true;
      for (BasicBlock block : range.getProtectedRange()) {
        if (!block.getSeq().isEmpty()) {
          isEmpty = false;
          break;
        }
      }

      if (isEmpty) {
        for (BasicBlock block : range.getProtectedRange()) {
          block.removeSuccessorException(range.getHandler());
        }

        lstRanges.remove(i);
        graph.addComment("$VF: Removed empty exception range");
      }
    }
  }

  public static void removeCircularRanges(final ControlFlowGraph graph) {

    GenericDominatorEngine engine = new GenericDominatorEngine(new IGraph() {
      @Override
      public List<? extends IGraphNode> getReversePostOrderList() {
        return graph.getReversePostOrder();
      }

      @Override
      public Set<? extends IGraphNode> getRoots() {
        return new HashSet<>(Collections.singletonList(graph.getFirst()));
      }
    });

    engine.initialize();

    List<ExceptionRangeCFG> lstRanges = graph.getExceptions();
    for (int i = lstRanges.size() - 1; i >= 0; i--) {
      ExceptionRangeCFG range = lstRanges.get(i);

      BasicBlock handler = range.getHandler();
      List<BasicBlock> rangeList = range.getProtectedRange();

      if (rangeList.contains(handler)) {  // TODO: better removing strategy

        List<BasicBlock> lstRemBlocks = getReachableBlocksRestricted(range.getHandler(), range, engine);

        if (lstRemBlocks.size() < rangeList.size() || rangeList.size() == 1) {
          for (BasicBlock block : lstRemBlocks) {
            block.removeSuccessorException(handler);
            rangeList.remove(block);
          }
        }

        if (rangeList.isEmpty()) {
          lstRanges.remove(i);
        }
      }
    }
  }

  private static List<BasicBlock> getReachableBlocksRestricted(BasicBlock start, ExceptionRangeCFG range, GenericDominatorEngine engine) {

    List<BasicBlock> lstRes = new ArrayList<>();

    LinkedList<BasicBlock> stack = new LinkedList<>();
    Set<BasicBlock> setVisited = new HashSet<>();

    stack.addFirst(start);

    while (!stack.isEmpty()) {
      BasicBlock block = stack.removeFirst();

      setVisited.add(block);

      if (range.getProtectedRange().contains(block) && engine.isDominator(block, start)) {
        lstRes.add(block);

        List<BasicBlock> lstSuccs = new ArrayList<>(block.getSuccs());
        lstSuccs.addAll(block.getSuccExceptions());

        for (BasicBlock succ : lstSuccs) {
          if (!setVisited.contains(succ)) {
            stack.add(succ);
          }
        }
      }
    }

    return lstRes;
  }

  public static boolean hasObfuscatedExceptions(ControlFlowGraph graph) {
    Map<BasicBlock, Set<BasicBlock>> mapRanges = new HashMap<>();
    for (ExceptionRangeCFG range : graph.getExceptions()) {
      mapRanges.computeIfAbsent(range.getHandler(), k -> new HashSet<>()).addAll(range.getProtectedRange());
    }

    for (Entry<BasicBlock, Set<BasicBlock>> ent : mapRanges.entrySet()) {
      Set<BasicBlock> setEntries = new HashSet<>();

      for (BasicBlock block : ent.getValue()) {
        Set<BasicBlock> setTemp = new HashSet<>(block.getPreds());
        setTemp.removeAll(ent.getValue());

        if (!setTemp.isEmpty()) {
          setEntries.add(block);
        }
      }

      if (ent.getValue().contains(graph.getFirst())) {
        setEntries.add(graph.getFirst());
      }

      if (!setEntries.isEmpty()) {
        if (setEntries.size() > 1 /*|| ent.getValue().contains(first)*/) {
          return true;
        }
      }
    }

    return false;
  }

  public static boolean handleMultipleEntryExceptionRanges(ControlFlowGraph graph) {
    GenericDominatorEngine engine = new GenericDominatorEngine(new IGraph() {
      @Override
      public List<? extends IGraphNode> getReversePostOrderList() {
        return graph.getReversePostOrder();
      }

      @Override
      public Set<? extends IGraphNode> getRoots() {
        return new HashSet<>(Collections.singletonList(graph.getFirst()));
      }
    });

    engine.initialize();

    boolean found;

    while (true) {
      found = false;
      boolean splitted = false;

      for (ExceptionRangeCFG range : graph.getExceptions()) {
        Set<BasicBlock> setEntries = getRangeEntries(range);

        if (setEntries.size() > 1) { // multiple-entry protected range
          found = true;

          if (splitExceptionRange(range, setEntries, graph, engine)) {
            splitted = true;
            graph.addComment("$VF: Handled exception range with multiple entry points by splitting it");
            break;
          }
        }
      }

      if (!splitted) {
        break;
      }
    }

    return !found;
  }

  private static Set<BasicBlock> getRangeEntries(ExceptionRangeCFG range) {
    Set<BasicBlock> setEntries = new HashSet<>();
    Set<BasicBlock> setRange = new HashSet<>(range.getProtectedRange());

    for (BasicBlock block : range.getProtectedRange()) {
      Set<BasicBlock> setPreds = new HashSet<>(block.getPreds());
      setPreds.removeAll(setRange);

      if (!setPreds.isEmpty()) {
        setEntries.add(block);
      }
    }

    return setEntries;
  }

  private static boolean splitExceptionRange(ExceptionRangeCFG range,
                                             Set<BasicBlock> setEntries,
                                             ControlFlowGraph graph,
                                             GenericDominatorEngine engine) {
    for (BasicBlock entry : setEntries) {
      List<BasicBlock> lstSubrangeBlocks = getReachableBlocksRestricted(entry, range, engine);
      if (!lstSubrangeBlocks.isEmpty() && lstSubrangeBlocks.size() < range.getProtectedRange().size()) {
        // add new range
        ExceptionRangeCFG subRange = new ExceptionRangeCFG(lstSubrangeBlocks, range.getHandler(), range.getExceptionTypes());
        graph.getExceptions().add(subRange);
        // shrink the original range
        range.getProtectedRange().removeAll(lstSubrangeBlocks);
        return true;
      }
      else {
        // should not happen
        DecompilerContext.getLogger().writeMessage("Inconsistency found while splitting protected range", IFernflowerLogger.Severity.WARN);
      }
    }

    return false;
  }

  public static void insertDummyExceptionHandlerBlocks(ControlFlowGraph graph, BytecodeVersion bytecode_version) {
    Map<BasicBlock, Set<ExceptionRangeCFG>> mapRanges = new HashMap<>();
    for (ExceptionRangeCFG range : graph.getExceptions()) {
      mapRanges.computeIfAbsent(range.getHandler(), k -> new HashSet<>()).add(range);
    }

    for (Entry<BasicBlock, Set<ExceptionRangeCFG>> ent : mapRanges.entrySet()) {
      BasicBlock handler = ent.getKey();
      Set<ExceptionRangeCFG> ranges = ent.getValue();

      if (ranges.size() == 1) {
        continue;
      }

      for (ExceptionRangeCFG range : ranges) {

        // add some dummy instructions to prevent optimizing away the empty block
        SimpleInstructionSequence seq = new SimpleInstructionSequence();
        seq.addInstruction(Instruction.create(CodeConstants.opc_bipush, false, CodeConstants.GROUP_GENERAL, bytecode_version, new int[]{0}, 1), -1);
        seq.addInstruction(Instruction.create(CodeConstants.opc_pop, false, CodeConstants.GROUP_GENERAL, bytecode_version, null, 1), -1);

        BasicBlock dummyBlock = new BasicBlock(++graph.last_id);
        dummyBlock.setSeq(seq);

        graph.getBlocks().addWithKey(dummyBlock, dummyBlock.id);

        // only exception predecessors from this range considered
        List<BasicBlock> lstPredExceptions = new ArrayList<>(handler.getPredExceptions());
        lstPredExceptions.retainAll(range.getProtectedRange());

        // replace predecessors
        for (BasicBlock pred : lstPredExceptions) {
          pred.replaceSuccessor(handler, dummyBlock);
        }

        // replace handler
        range.setHandler(dummyBlock);
        // add common exception edges
        Set<BasicBlock> commonHandlers = new HashSet<>(handler.getSuccExceptions());
        for (BasicBlock pred : lstPredExceptions) {
          commonHandlers.retainAll(pred.getSuccExceptions());
        }
        // TODO: more sanity checks?
        for (BasicBlock commonHandler : commonHandlers) {
          ExceptionRangeCFG commonRange = graph.getExceptionRange(commonHandler, handler);

          dummyBlock.addSuccessorException(commonHandler);
          commonRange.getProtectedRange().add(dummyBlock);
        }

        dummyBlock.addSuccessor(handler);

        graph.addComment("$VF: Inserted dummy exception handlers to handle obfuscated exceptions");
      }
    }
  }
}