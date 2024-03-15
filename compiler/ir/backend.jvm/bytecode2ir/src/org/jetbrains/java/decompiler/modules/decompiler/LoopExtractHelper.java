// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.modules.decompiler.exps.IfExprent;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement.EdgeDirection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


public final class LoopExtractHelper {


  public static boolean extractLoops(Statement root) {

    boolean res = (extractLoopsRec(root) != 0);

    if (res) {
      SequenceHelper.condenseSequences(root);
    }

    return res;
  }


  private static int extractLoopsRec(Statement stat) {

    boolean res = false;

    while (true) {

      boolean updated = false;

      for (Statement st : new ArrayList<>(stat.getStats())) {
        int extr = extractLoopsRec(st);
        res |= (extr != 0);

        if (extr == 2) {
          updated = true;
          break;
        }
      }

      if (!updated) {
        break;
      }
    }

    if (stat instanceof DoStatement) {
      if (extractLoop((DoStatement)stat)) {
        return 2;
      }
    }

    return res ? 1 : 0;
  }

  private static boolean extractLoop(DoStatement stat) {
    if (stat.getLooptype() != DoStatement.Type.INFINITE) {
      return false;
    }

    List<Statement> stats = new ArrayList<>();
    for (StatEdge edge : stat.getLabelEdges()) {
      if (edge.getType() != StatEdge.TYPE_CONTINUE && !(edge.getDestination() instanceof DummyExitStatement)) {
        if (edge.getType() == StatEdge.TYPE_BREAK && isExternStatement(stat, edge.getSource(), edge.getSource())) {
          stats.add(edge.getSource());
        }
        else {
          return false;
        }
      }
    }

    if (!stats.isEmpty()) { // In this case prioritize first to help the Loop enhancer
      if (stat.getParent().getStats().getLast() != stat) {
        return false;
      }
    }

    if (!extractFirstIf(stat, stats)) {
      return extractLastIf(stat, stats);
    }
    else {
      return true;
    }
  }

  private static boolean extractLastIf(DoStatement stat, List<Statement> stats) {

    // search for an if condition at the end of the loop
    Statement last = stat.getFirst();
    while (last instanceof SequenceStatement) {
      last = last.getStats().getLast();
    }

    if (last instanceof IfStatement) {
      IfStatement lastif = (IfStatement)last;
      if (lastif.iftype == IfStatement.IFTYPE_IF && lastif.getIfstat() != null) {
        Statement ifstat = lastif.getIfstat();
        if (lastif.getAllSuccessorEdges().isEmpty()) {
          return false;
        }
        StatEdge elseedge = lastif.getAllSuccessorEdges().get(0);

        if (elseedge.getType() == StatEdge.TYPE_CONTINUE && elseedge.closure == stat) {

          Set<Statement> set = stat.getNeighboursSet(StatEdge.TYPE_CONTINUE, EdgeDirection.BACKWARD);
          set.remove(last);

          if (set.isEmpty()) { // no direct continues in a do{}while loop
            if (isExternStatement(stat, ifstat, ifstat)) {
              Statement first = stat.getFirst();
              while (first instanceof SequenceStatement) {
                first = first.getFirst();
              }
              if (first instanceof DoStatement && ((DoStatement)first).getLooptype() == DoStatement.Type.INFINITE) {
                return false;
              }

              for (Statement s : stats) {
                if (!ifstat.containsStatement(s)) {
                  return false;
                }
              }
              extractIfBlock(stat, lastif);
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  // Extracts if statements out of the first if statement and modifies return -> break <label>
  private static boolean extractFirstIf(DoStatement stat, List<Statement> stats) {

    // search for an if condition at the entrance of the loop
    Statement first = stat.getFirst();
    while (first instanceof SequenceStatement) {
      first = first.getFirst();
    }

    // found an if statement
    if (first instanceof IfStatement) {
      IfStatement firstif = (IfStatement)first;

      if (firstif.getFirst().getExprents().isEmpty()) {

        if (firstif.iftype == IfStatement.IFTYPE_IF && firstif.getIfstat() != null) {
          Statement ifstat = firstif.getIfstat();

          if (isExternStatement(stat, ifstat, ifstat)) {
            for (Statement s : stats) {
              if (!ifstat.containsStatement(s)) {
                return false;
              }
            }

            // Extract if block
            extractIfBlock(stat, firstif);

            return true;
          } else {
            // not extern statement- last viability check to try and produce a viable loopable structure

            if (stat.getStats().size() == 1) {
              List<StatEdge> continues = ifstat.getSuccessorEdges(StatEdge.TYPE_CONTINUE);
              boolean newDest = false;
              Statement removeFrom = null;
              if (ifstat instanceof SequenceStatement) {
                removeFrom = ifstat.getStats().getLast();
                continues.addAll(ifstat.getStats().getLast().getSuccessorEdges(StatEdge.TYPE_CONTINUE));
                newDest = true;
              }

              if (continues.size() == 1 && continues.get(0).getDestination() == stat) {
                Statement check = stat;

                while (check.getSuccessorEdges(StatEdge.TYPE_REGULAR).isEmpty()) {
                  check = check.getParent();

                  // Reached top- can't go anywhere
                  if (check == null) {
                    return false;
                  }
                }

                extractIfBlockIntoLoop(stat, firstif, check.getSuccessorEdges(StatEdge.TYPE_REGULAR).get(0).getDestination());

                if (newDest) {
                  removeFrom.removeSuccessor(continues.get(0));
                }

                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }


  private static boolean isExternStatement(DoStatement loop, Statement block, Statement stat) {

    for (StatEdge edge : stat.getAllSuccessorEdges()) {
      if (loop.containsStatement(edge.getDestination()) &&
          !block.containsStatement(edge.getDestination())) {
        return false;
      }
    }

    for (Statement st : stat.getStats()) {
      if (!isExternStatement(loop, block, st)) {
        return false;
      }
    }

    return true;
  }


  // Moves the body of the if statement to be after the loop, and replaces the body with a break to get to get there
  private static void extractIfBlock(DoStatement loop, IfStatement ifstat) {
    // If body is the target we want to extract
    Statement target = ifstat.getIfstat();
    // Edge from head to if body
    StatEdge ifedge = ifstat.getIfEdge();

    // Remove if body
    ifstat.setIfstat(null);
    // Add break, remove if statement
    ifedge.getSource().changeEdgeType(EdgeDirection.FORWARD, ifedge, StatEdge.TYPE_BREAK);
    ifedge.closure = loop;
    ifstat.getStats().removeWithKey(target.id);

    // label the break edge
    loop.addLabeledEdge(ifedge);

    // Lift target statement from inside if statement to neighbor of loop
    // Makes a sequence with loop and target statement and replaces the loop with it
    SequenceStatement block = new SequenceStatement(Arrays.asList(loop, target));
    loop.getParent().replaceStatement(loop, block);
    block.setAllParent();

    // Add regular successor from the loop to the extracted block
    loop.addSuccessor(new StatEdge(StatEdge.TYPE_REGULAR, loop, target));

    // Add label edges to loop from created block
    for (StatEdge edge : new ArrayList<>(block.getLabelEdges())) {
      if (edge.getType() == StatEdge.TYPE_CONTINUE || edge == ifedge) {
        loop.addLabeledEdge(edge);
      }
    }

    // Find predecessor continues and replace their source from the block to the loop, makes continues from the loop body flow to the proper statement
    for (StatEdge edge : block.getPredecessorEdges(StatEdge.TYPE_CONTINUE)) {
      if (loop.containsStatementStrict(edge.getSource())) {
        block.removePredecessor(edge);
        edge.getSource().changeEdgeNode(EdgeDirection.FORWARD, edge, loop);
        loop.addPredecessor(edge);
      }
    }

    List<StatEdge> link = target.getPredecessorEdges(StatEdge.TYPE_BREAK);
    if (link.size() == 1) {
      link.get(0).canInline = false;
    }
  }

  // Moves the body of the if statement to be after the if statement in the loop.
  private static void extractIfBlockIntoLoop(DoStatement loop, IfStatement ifStat, Statement destination) {
    // If body is the target we want to extract
    Statement target = ifStat.getIfstat();
    // Edge from head to if body
    StatEdge ifedge = ifStat.getIfEdge();

    // Remove if body
    ifStat.setIfstat(null);
    // Add break, remove if statement
    ifedge.getDestination().removePredecessor(ifedge);
    ifedge.getSource().changeEdgeType(EdgeDirection.FORWARD, ifedge, StatEdge.TYPE_BREAK);
    ifedge.closure = loop;
    ifStat.getStats().removeWithKey(target.id);

    ifedge.setDestination(destination);
    destination.addPredecessor(ifedge);

    // label the break edge
    loop.addLabeledEdge(ifedge);
    ifStat.setIfEdge(ifedge);

    SequenceStatement block = new SequenceStatement(Arrays.asList(ifStat, target));
    ifStat.replaceWith( block);
    block.setAllParent();

    ifStat.addSuccessor(new StatEdge(StatEdge.TYPE_REGULAR, ifStat, target));

    IfExprent expr = ifStat.getHeadexprent();
    expr.setCondition(new FunctionExprent(FunctionType.BOOL_NOT, expr.getCondition(), null));
  }
}
