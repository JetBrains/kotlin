// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.code;

import org.jetbrains.java.decompiler.code.*;
import org.jetbrains.java.decompiler.code.cfg.BasicBlock;
import org.jetbrains.java.decompiler.code.cfg.ControlFlowGraph;
import org.jetbrains.java.decompiler.code.cfg.ExceptionRangeCFG;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import java.util.*;

public final class DeadCodeHelper {

  public static void removeDeadBlocks(ControlFlowGraph graph) {

    LinkedList<BasicBlock> stack = new LinkedList<>();
    HashSet<BasicBlock> setStacked = new HashSet<>();

    stack.add(graph.getFirst());
    setStacked.add(graph.getFirst());

    while (!stack.isEmpty()) {
      BasicBlock block = stack.removeFirst();

      List<BasicBlock> lstSuccs = new ArrayList<>(block.getSuccs());
      lstSuccs.addAll(block.getSuccExceptions());

      for (BasicBlock succ : lstSuccs) {
        if (!setStacked.contains(succ)) {
          stack.add(succ);
          setStacked.add(succ);
        }
      }
    }

    HashSet<BasicBlock> setAllBlocks = new HashSet<>(graph.getBlocks());
    setAllBlocks.removeAll(setStacked);

    for (BasicBlock block : setAllBlocks) {
      graph.removeBlock(block);
    }
  }

  public static void removeEmptyBlocks(ControlFlowGraph graph) {

    List<BasicBlock> blocks = graph.getBlocks();

    boolean cont;
    do {
      cont = false;

      for (int i = blocks.size() - 1; i >= 0; i--) {
        BasicBlock block = blocks.get(i);

        if (removeEmptyBlock(graph, block, false)) {
          cont = true;
          break;
        }
      }
    }
    while (cont);
  }

  private static boolean removeEmptyBlock(ControlFlowGraph graph, BasicBlock block, boolean merging) {

    boolean deletedRanges = false;

    if (block.getSeq().isEmpty()) {

      if (block.getSuccs().size() > 1) {
        if (block.getPreds().size() > 1) {
          // ambiguous block
          throw new RuntimeException("ERROR: empty block with multiple predecessors and successors found");
        }
        else if (!merging) {
          throw new RuntimeException("ERROR: empty block with multiple successors found");
        }
      }

      HashSet<BasicBlock> setExits = new HashSet<>(graph.getLast().getPreds());

      if (block.getPredExceptions().isEmpty() &&
          (!setExits.contains(block) || block.getPreds().size() == 1)) {

        if (setExits.contains(block)) {
          BasicBlock pred = block.getPreds().get(0);

          // FIXME: flag in the basic block
          if (pred.getSuccs().size() != 1 || (!pred.getSeq().isEmpty()
                                              && pred.getSeq().getLastInstr().group == CodeConstants.GROUP_SWITCH)) {
            return false;
          }
        }

        HashSet<BasicBlock> setPreds = new HashSet<>(block.getPreds());
        HashSet<BasicBlock> setSuccs = new HashSet<>(block.getSuccs());

        // collect common exception ranges of predecessors and successors
        HashSet<BasicBlock> setCommonExceptionHandlers = null;
        for (int i = 0; i < 2; ++i) {
          for (BasicBlock pred : i == 0 ? setPreds : setSuccs) {
            if (setCommonExceptionHandlers == null) {
              setCommonExceptionHandlers = new HashSet<>(pred.getSuccExceptions());
            }
            else {
              setCommonExceptionHandlers.retainAll(pred.getSuccExceptions());
            }
          }
        }

        // check the block to be in each of the common ranges
        if (setCommonExceptionHandlers != null && !setCommonExceptionHandlers.isEmpty()) {
          for (BasicBlock handler : setCommonExceptionHandlers) {
            if (!block.getSuccExceptions().contains(handler)) {
              return false;
            }
          }
        }

        // remove ranges consisting of this one block
        List<ExceptionRangeCFG> lstRanges = graph.getExceptions();
        for (int i = lstRanges.size() - 1; i >= 0; i--) {
          ExceptionRangeCFG range = lstRanges.get(i);
          List<BasicBlock> lst = range.getProtectedRange();

          if (lst.size() == 1 && lst.get(0) == block) {
            return false;
          }
        }


        // connect remaining nodes
        if (merging) {
          BasicBlock pred = block.getPreds().get(0);
          pred.removeSuccessor(block);

          List<BasicBlock> lstSuccs = new ArrayList<>(block.getSuccs());
          for (BasicBlock succ : lstSuccs) {
            block.removeSuccessor(succ);
            pred.addSuccessor(succ);
          }
        }
        else {
          for (BasicBlock pred : setPreds) {
            for (BasicBlock succ : setSuccs) {
              pred.replaceSuccessor(block, succ);
            }
          }
        }

        // finally exit edges
        Set<BasicBlock> setFinallyExits = graph.getFinallyExits();
        if (setFinallyExits.contains(block)) {
          setFinallyExits.remove(block);
          setFinallyExits.add(setPreds.iterator().next());
        }

        // replace first if necessary
        if (graph.getFirst() == block) {
          if (setSuccs.size() != 1) {
            throw new RuntimeException("multiple or no entry blocks!");
          }
          else {
            graph.setFirst(setSuccs.iterator().next());
          }
        }

        // remove this block
        graph.removeBlock(block);

        if (deletedRanges) {
          removeDeadBlocks(graph);
        }
      }
    }

    return deletedRanges;
  }


  public static boolean isDominator(ControlFlowGraph graph, BasicBlock block, BasicBlock dom) {

    HashSet<BasicBlock> marked = new HashSet<>();

    if (block == dom) {
      return true;
    }

    LinkedList<BasicBlock> lstNodes = new LinkedList<>();
    lstNodes.add(block);

    while (!lstNodes.isEmpty()) {

      BasicBlock node = lstNodes.remove(0);
      if (marked.contains(node)) {
        continue;
      }
      else {
        marked.add(node);
      }

      if (node == graph.getFirst()) {
        return false;
      }

      for (int i = 0; i < node.getPreds().size(); i++) {
        BasicBlock pred = node.getPreds().get(i);
        if (pred != dom && !marked.contains(pred)) {
          lstNodes.add(pred);
        }
      }

      for (int i = 0; i < node.getPredExceptions().size(); i++) {
        BasicBlock pred = node.getPredExceptions().get(i);
        if (pred != dom && !marked.contains(pred)) {
          lstNodes.add(pred);
        }
      }
    }

    return true;
  }

  public static void removeGotos(ControlFlowGraph graph) {
    Set<BasicBlock> ignored = findCircularJumps(graph);

    for (BasicBlock block : graph.getBlocks()) {
      Instruction instr = block.getLastInstruction();

      if (instr != null && instr.opcode == CodeConstants.opc_goto) {
        // Part of an empty circular jump sequence. This needs to be condensed later, so we do not touch it
        if (ignored.contains(block)) {
          continue;
        }

        block.getSeq().removeLast();
      }
    }

    removeEmptyBlocks(graph);
  }

  // Finds any circular jumps within the graph. Examples include:
  //
  // Single instruction infinte loop
  // 0: goto 0;
  //
  // Multiple instruction infinite loop
  // 0: goto 2;
  // 1: goto 0;
  // 2: goto 1;
  //
  private static Set<BasicBlock> findCircularJumps(ControlFlowGraph graph) {
    Set<BasicBlock> ret = new HashSet<>();

    // Temp list
    Set<BasicBlock> blocks = new HashSet<>();

    // Iterate through all blocks
    for (BasicBlock block : graph.getBlocks()) {
      if (ret.contains(block)) {
        continue;
      }

      blocks.add(block);

      // Jump traversal
      BasicBlock check = block;
      while (true) {
        Instruction instr = check.getLastInstruction();

        if (instr != null && instr.opcode == CodeConstants.opc_goto) {
          if (check.getSuccs().size() == 1) {
            check = check.getSuccs().get(0);

            if (blocks.contains(check)) {
              ret.addAll(blocks);
              break; // Circular jump found
            }

            blocks.add(check);
          } else {
            break; // More than 1 successor, stop
          }
        } else {
          break; // No goto, stop
        }
      }

      // Clear temp list for next block
      blocks.clear();
    }

    return ret;
  }

  public static void connectDummyExitBlock(ControlFlowGraph graph) {

    BasicBlock exit = graph.getLast();
    for (BasicBlock block : new HashSet<>(exit.getPreds())) {
      exit.removePredecessor(block);
      block.addSuccessor(exit);
    }
  }

  public static void mergeBasicBlocks(ControlFlowGraph graph) {

    while (true) {

      boolean merged = false;
      int originBlocksCount = graph.getBlocks().size();

      for (BasicBlock block : graph.getBlocks()) {

        InstructionSequence seq = block.getSeq();

        if (block.getSuccs().size() == 1) {
          BasicBlock next = block.getSuccs().get(0);

          if (next != graph.getLast() && (seq.isEmpty() || seq.getLastInstr().group != CodeConstants.GROUP_SWITCH)) {

            if (next.getPreds().size() == 1 && next.getPredExceptions().isEmpty()
                && next != graph.getFirst()) {
              // TODO: implement a dummy start block
              boolean sameRanges = true;
              for (ExceptionRangeCFG range : graph.getExceptions()) {
                if (range.getProtectedRange().contains(block) ^
                    range.getProtectedRange().contains(next)) {
                  sameRanges = false;
                  break;
                }
              }

              if (sameRanges) {
                seq.addSequence(next.getSeq());
                block.getInstrOldOffsets().addAll(next.getInstrOldOffsets());
                next.getSeq().clear();

                removeEmptyBlock(graph, next, true);

                merged = true;
                break;
              }
            }
          }
        }
      }

      if (!merged || graph.getBlocks().size() == originBlocksCount) {
        break;
      }
    }
  }
}
