// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.modules.decompiler.stats.DoStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.struct.StructClass;

import java.util.*;
import java.util.stream.Collectors;


public class EliminateLoopsHelper {

  public static boolean eliminateLoops(Statement root, StructClass cl) {

    boolean ret = eliminateLoopsRec(root);

    if(ret) {
      SequenceHelper.condenseSequences(root);

      Set<Integer> setReorderedIfs = new HashSet<>();

      // TODO: what problems does SSA being null cause?
      while(SimplifyExprentsHelper.simplifyStackVarsStatement(root, setReorderedIfs, null, cl, false)) {
        SequenceHelper.condenseSequences(root);
      }
    }

    return ret;
  }

  private static boolean eliminateLoopsRec(Statement stat) {

    for (Statement st : stat.getStats()) {
      if (eliminateLoopsRec(st)) {
        return true;
      }
    }

    if (stat instanceof DoStatement && isLoopRedundant((DoStatement)stat)) {
      return true;
    }

    return false;
  }

  private static boolean isLoopRedundant(DoStatement loop) {

    if (loop.getLooptype() != DoStatement.Type.INFINITE) {
      return false;
    }

    // get parent loop if exists
    Statement parentloop = loop.getParent();
    while (parentloop != null && !(parentloop instanceof DoStatement)) {
      parentloop = parentloop.getParent();
    }

    if (parentloop == null || parentloop.getBasichead() != loop.getBasichead()) {
      return false;
    }

    // collect relevant break edges
    List<StatEdge> lstBreakEdges = new ArrayList<>();
    for (StatEdge edge : loop.getLabelEdges()) {
      if (edge.getType() == StatEdge.TYPE_BREAK) { // all break edges are explicit because of LOOP_DO type
        lstBreakEdges.add(edge);
      }
    }


    Statement loopcontent = loop.getFirst();

    boolean firstok = !loopcontent.hasAnySuccessor();
    if (!firstok) {
      StatEdge edge = loopcontent.getFirstSuccessor();
      firstok = (edge.closure == loop && edge.getType() == StatEdge.TYPE_BREAK);
      if (firstok) {
        lstBreakEdges.remove(edge);
      }
    }


    if (!lstBreakEdges.isEmpty()) {
      if (firstok) {

        HashMap<Integer, Boolean> statLabeled = new HashMap<>();
        List<Statement> lstEdgeClosures = new ArrayList<>();

        for (StatEdge edge : lstBreakEdges) {
          Statement minclosure = LowBreakHelper.getMinClosure(loopcontent, edge.getSource());
          lstEdgeClosures.add(minclosure);
        }

        int precount = loop.isLabeled() ? 1 : 0;
        for (Statement st : lstEdgeClosures) {
          if (!statLabeled.containsKey(st.id)) {
            boolean btemp = st.isLabeled();
            precount += btemp ? 1 : 0;
            statLabeled.put(st.id, btemp);
          }
        }

        for (int i = 0; i < lstBreakEdges.size(); i++) {
          Statement st = lstEdgeClosures.get(i);
          statLabeled.put(st.id, LowBreakHelper.isBreakEdgeLabeled(lstBreakEdges.get(i).getSource(), st) | statLabeled.get(st.id));
        }

        for (int i = 0; i < lstBreakEdges.size(); i++) {
          lstEdgeClosures.set(i, getMaxBreakLift(lstEdgeClosures.get(i), lstBreakEdges.get(i), statLabeled, loop));
        }

        statLabeled.clear();
        for (Statement st : lstEdgeClosures) {
          statLabeled.put(st.id, st.isLabeled());
        }

        for (int i = 0; i < lstBreakEdges.size(); i++) {
          Statement st = lstEdgeClosures.get(i);
          statLabeled.put(st.id, LowBreakHelper.isBreakEdgeLabeled(lstBreakEdges.get(i).getSource(), st) | statLabeled.get(st.id));
        }

        long postcount = statLabeled.values().stream().filter(Boolean::booleanValue).count();

        if (precount <= postcount) {
          return false;
        } else {
          for (int i = 0; i < lstBreakEdges.size(); i++) {
            lstEdgeClosures.get(i).addLabeledEdge(lstBreakEdges.get(i));
          }
        }
      } else {
        return false;
      }
    }

    // Continues

    // Find all continue edges leaving the loop
    Set<StatEdge> continues = new HashSet<>();
    TryWithResourcesProcessor.findEdgesLeaving(loopcontent, loop, continues);

    List<StatEdge> edges = continues.stream()
      .filter(edge -> edge.getType() == StatEdge.TYPE_CONTINUE) // Only consider continue edges found
      .collect(Collectors.toList()); // Set -> List

    if (!edges.isEmpty()) {
      // Don't eliminate if we have continues leading to the parent loop!
      for (StatEdge edge : edges) {
        if (edge.getDestination() == parentloop) {
          return false;
        }
      }
    }

    // TODO: is this needed? fixes cases where loop is eliminated but it has a successor!
//    if (!loop.getSuccessorEdges(StatEdge.TYPE_REGULAR).isEmpty()) {
//      return false;
//    }

    eliminateLoop(loop, parentloop);

    return true;
  }

  private static Statement getMaxBreakLift(Statement stat, StatEdge edge, HashMap<Integer, Boolean> statLabeled, Statement max) {

    Statement closure = stat;
    Statement newclosure = stat;

    while ((newclosure = getNextBreakLift(newclosure, edge, statLabeled, max)) != null) {
      closure = newclosure;
    }

    return closure;
  }

  private static Statement getNextBreakLift(Statement stat, StatEdge edge, HashMap<Integer, Boolean> statLabeled, Statement max) {

    Statement closure = stat.getParent();

    while (closure != null && closure != max && !closure.containsStatementStrict(edge.getDestination())) {

      boolean edge_labeled = LowBreakHelper.isBreakEdgeLabeled(edge.getSource(), closure);
      boolean stat_labeled = statLabeled.containsKey(closure.id) ? statLabeled.get(closure.id) : closure.isLabeled();

      if (stat_labeled || !edge_labeled) {
        return closure;
      }

      closure = closure.getParent();
    }

    return null;
  }

  private static void eliminateLoop(Statement loop, Statement parentloop) {

    // remove the last break edge, if exists
    Statement loopcontent = loop.getFirst();
    // TODO: originally was getAllSuccessorEdges
    if (loopcontent.hasSuccessor(StatEdge.TYPE_BREAK)) {
      loopcontent.removeSuccessor(loopcontent.getSuccessorEdges(StatEdge.TYPE_BREAK).get(0));
    }

    // move continue edges to the parent loop
    List<StatEdge> lst = new ArrayList<>(loop.getLabelEdges());

    for (StatEdge edge : lst) {
      edge.changeDestination(parentloop);

      parentloop.addLabeledEdge(edge);
    }

    // replace loop with its content
    loop.replaceWith(loopcontent);
  }
}
