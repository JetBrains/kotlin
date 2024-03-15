// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.code;

import org.jetbrains.java.decompiler.code.*;
import org.jetbrains.java.decompiler.code.cfg.BasicBlock;
import org.jetbrains.java.decompiler.code.cfg.ControlFlowGraph;
import org.jetbrains.java.decompiler.code.cfg.ExceptionRangeCFG;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.util.DotExporter;

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
            if (DecompilerContext.getOption(IFernflowerPreferences.REMOVE_EMPTY_RANGES)) {
              block.removeSuccessorException(range.getHandler());
              lstRanges.remove(i);

              deletedRanges = true;
            }
            else {
              return false;
            }
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

  public static void extendSynchronizedRangeToMonitorexit(ControlFlowGraph graph) {
    while(true) {

      boolean range_extended = false;

      for (ExceptionRangeCFG range : graph.getExceptions()) {

        Set<BasicBlock> setPreds = new HashSet<>();
        for (BasicBlock block : range.getProtectedRange()) {
          setPreds.addAll(block.getPreds());
        }
        for (BasicBlock basicBlock : range.getProtectedRange()) {
          setPreds.remove(basicBlock);
        }

        if(setPreds.size() != 1) {
          continue; // multiple predecessors, obfuscated range
        }

        BasicBlock predBlock = setPreds.iterator().next();
        InstructionSequence predSeq = predBlock.getSeq();
        if(predSeq.isEmpty() || predSeq.getLastInstr().opcode != CodeConstants.opc_monitorenter) {
          continue; // not a synchronized range
        }

        boolean monitorexit_in_range = false;
        Set<BasicBlock> setProtectedBlocks = new HashSet<>(range.getProtectedRange());
        setProtectedBlocks.add(range.getHandler());

        for (BasicBlock block : setProtectedBlocks) {
          InstructionSequence blockSeq = block.getSeq();
          for (int i = 0; i < blockSeq.length(); i++) {
            if (blockSeq.getInstr(i).opcode == CodeConstants.opc_monitorexit) {
              monitorexit_in_range = true;
              break;
            }
          }

          if(monitorexit_in_range) {
            break;
          }
        }

        if(monitorexit_in_range) {
          continue; // protected range already contains monitorexit
        }

        Set<BasicBlock> setSuccs = new HashSet<>();
        for (BasicBlock block : range.getProtectedRange()) {
          setSuccs.addAll(block.getSuccs());
        }
        for (BasicBlock basicBlock : range.getProtectedRange()) {
          setSuccs.remove(basicBlock);
        }

        if(setSuccs.size() != 1) {
          continue; // non-unique successor
        }

        BasicBlock succBlock = setSuccs.iterator().next();
        InstructionSequence succSeq = succBlock.getSeq();

        int succ_monitorexit_index = -1;
        for (int i = 0; i < succSeq.length(); i++) {
          if (succSeq.getInstr(i).opcode == CodeConstants.opc_monitorexit) {
            succ_monitorexit_index = i;
            break;
          }
        }

        if(succ_monitorexit_index < 0) {
          continue; // monitorexit not found in the single successor block
        }

        BasicBlock handlerBlock = range.getHandler();
        if(handlerBlock.getSuccs().size() != 1) {
          continue; // non-unique handler successor
        }
        BasicBlock succHandler = handlerBlock.getSuccs().get(0);
        InstructionSequence succHandlerSeq = succHandler.getSeq();
        if(succHandlerSeq.isEmpty() || succHandlerSeq.getLastInstr().opcode != CodeConstants.opc_athrow) {
          continue; // not a standard synchronized range
        }

        int handler_monitorexit_index = -1;
        for (int i = 0; i < succHandlerSeq.length(); i++) {
          if (succHandlerSeq.getInstr(i).opcode == CodeConstants.opc_monitorexit) {
            handler_monitorexit_index = i;
            break;
          }
        }

        if(handler_monitorexit_index < 0) {
          continue; // monitorexit not found in the handler successor block
        }

        // checks successful, prerequisites satisfied, now extend the range
        // FIXME: what is this splitting doing, and why does it cause the loop to never finish?
//        if(succ_monitorexit_index < succSeq.length() - 1) { // split block
//
//          SimpleInstructionSequence seq = new SimpleInstructionSequence();
//          for(int counter = 0; counter < succ_monitorexit_index; counter++) {
//            seq.addInstruction(succSeq.getInstr(0), -1);
//            succSeq.removeInstruction(0);
//          }
//
//          // build a separate block
//          BasicBlock newblock = new BasicBlock(++graph.last_id);
//          newblock.setSeq(seq);
//
//          // insert new block
//
//          for (BasicBlock block : new ArrayList<>(succBlock.getPreds())) {
//            block.replaceSuccessor(succBlock, newblock);
//          }
//
//          newblock.addSuccessor(succBlock);
//          graph.getBlocks().addWithKey(newblock, newblock.id);
//
//          succBlock = newblock;
//        }

        // copy exception edges and extend protected ranges (successor block)
        BasicBlock rangeExitBlock = succBlock.getPreds().get(0);
        for (int j = 0; j < rangeExitBlock.getSuccExceptions().size(); j++) {
          BasicBlock hd = rangeExitBlock.getSuccExceptions().get(j);
          succBlock.addSuccessorException(hd);

          ExceptionRangeCFG rng = graph.getExceptionRange(hd, rangeExitBlock);
          rng.getProtectedRange().add(succBlock);
        }

        // copy instructions (handler successor block)
        InstructionSequence handlerSeq = handlerBlock.getSeq();
        for(int counter = 0; counter < handler_monitorexit_index; counter++) {
          handlerSeq.addInstruction(succHandlerSeq.getInstr(0), -1);
          succHandlerSeq.removeInstruction(0);
        }

        range_extended = true;
        graph.addComment("$VF: Extended synchronized range to monitorexit");
        break;
      }

      if(!range_extended) {
        break;
      }
    }

    // Extend range to monitorexit when the monitorexit block is a successor
    for (ExceptionRangeCFG range : graph.getExceptions()) {
      Set<BasicBlock> predecessors = new HashSet<>();

      // Problems arise if we're too deeply nested- we don't handle that case yet!
      boolean exceptionsOk = true;
      for (BasicBlock block : range.getProtectedRange()) {
        if (block.getSuccExceptions().size() > 1) {
          exceptionsOk = false;
          break;
        }
      }

      if (!exceptionsOk) {
        continue;
      }

      // Find all predecessors of the range
      for (BasicBlock block : range.getProtectedRange()) {
        predecessors.addAll(block.getPreds());
      }

      for (BasicBlock block : range.getProtectedRange()) {
        predecessors.remove(block);
      }

      // Can't have more than one entrypoint!
      if (predecessors.size() != 1) {
        continue;
      }

      BasicBlock pred = predecessors.iterator().next();
      if (pred.getSeq().isEmpty() || pred.getSeq().getLastInstr().opcode != CodeConstants.opc_monitorenter) {
        continue; // not a synchronized range
      }

      Set<BasicBlock> successors = new HashSet<>();
      for (BasicBlock block : range.getProtectedRange()) {
        successors.addAll(block.getSuccs());
      }

      for (BasicBlock block : range.getProtectedRange()) {
        successors.remove(block);
      }

      for (BasicBlock successor : successors) {
        if (!successor.getSeq().isEmpty() && successor.getSeq().getLastInstr().opcode == CodeConstants.opc_monitorexit) {
          // If the range doesn't have the monitorexit instruction, add it to the range
          if (!range.getProtectedRange().contains(successor)) {
            range.getProtectedRange().add(successor);

            // Add exception edges to the range
            successor.addSuccessorException(range.getHandler());
          }
        }
      }
    }

  }


  public static void incorporateValueReturns(ControlFlowGraph graph) {

    for (BasicBlock block : graph.getBlocks()) {
      InstructionSequence seq = block.getSeq();

      int len = seq.length();
      if (len > 0 && len < 3) {

        boolean ok = false;

        if (seq.getLastInstr().opcode >= CodeConstants.opc_ireturn && seq.getLastInstr().opcode <= CodeConstants.opc_return) {
          if (len == 1) {
            ok = true;
          }
          else if (seq.getLastInstr().opcode != CodeConstants.opc_return) {
            switch (seq.getInstr(0).opcode) {
              case CodeConstants.opc_iload:
              case CodeConstants.opc_lload:
              case CodeConstants.opc_fload:
              case CodeConstants.opc_dload:
              case CodeConstants.opc_aload:
              case CodeConstants.opc_aconst_null:
              case CodeConstants.opc_bipush:
              case CodeConstants.opc_sipush:
              case CodeConstants.opc_lconst_0:
              case CodeConstants.opc_lconst_1:
              case CodeConstants.opc_fconst_0:
              case CodeConstants.opc_fconst_1:
              case CodeConstants.opc_fconst_2:
              case CodeConstants.opc_dconst_0:
              case CodeConstants.opc_dconst_1:
              case CodeConstants.opc_ldc:
              case CodeConstants.opc_ldc_w:
              case CodeConstants.opc_ldc2_w:
                ok = true;
            }
          }
        }

        if (ok) {

          if (!block.getPreds().isEmpty()) {

            HashSet<BasicBlock> setPredHandlersUnion = new HashSet<>();
            HashSet<BasicBlock> setPredHandlersIntersection = new HashSet<>();

            boolean firstpred = true;
            for (BasicBlock pred : block.getPreds()) {
              if (firstpred) {
                setPredHandlersIntersection.addAll(pred.getSuccExceptions());
                firstpred = false;
              }
              else {
                setPredHandlersIntersection.retainAll(pred.getSuccExceptions());
              }

              setPredHandlersUnion.addAll(pred.getSuccExceptions());
            }

            // add exception ranges from predecessors
            for (BasicBlock basicBlock : block.getSuccExceptions()) {
              setPredHandlersIntersection.remove(basicBlock);
            }
            BasicBlock predecessor = block.getPreds().get(0);

            for (BasicBlock handler : setPredHandlersIntersection) {
              ExceptionRangeCFG range = graph.getExceptionRange(handler, predecessor);

              range.getProtectedRange().add(block);
              block.addSuccessorException(handler);
            }

            // remove redundant ranges
            HashSet<BasicBlock> setRangesToBeRemoved = new HashSet<>(block.getSuccExceptions());
            setRangesToBeRemoved.removeAll(setPredHandlersUnion);

            for (BasicBlock handler : setRangesToBeRemoved) {
              ExceptionRangeCFG range = graph.getExceptionRange(handler, block);

              if (range.getProtectedRange().size() > 1) {
                range.getProtectedRange().remove(block);
                block.removeSuccessorException(handler);
              }
            }
          }


          if (block.getPreds().size() == 1 && block.getPredExceptions().isEmpty()) {

            BasicBlock bpred = block.getPreds().get(0);
            if (bpred.getSuccs().size() == 1) {

              // add exception ranges of predecessor
              for (BasicBlock succ : bpred.getSuccExceptions()) {
                if (!block.getSuccExceptions().contains(succ)) {
                  ExceptionRangeCFG range = graph.getExceptionRange(succ, bpred);

                  range.getProtectedRange().add(block);
                  block.addSuccessorException(succ);
                }
              }

              // remove superfluous ranges from successors
              for (BasicBlock succ : new HashSet<>(block.getSuccExceptions())) {
                if (!bpred.getSuccExceptions().contains(succ)) {
                  ExceptionRangeCFG range = graph.getExceptionRange(succ, block);

                  if (range.getProtectedRange().size() > 1) {
                    range.getProtectedRange().remove(block);
                    block.removeSuccessorException(succ);
                  }
                }
              }
            }
          }
        }
      }
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
