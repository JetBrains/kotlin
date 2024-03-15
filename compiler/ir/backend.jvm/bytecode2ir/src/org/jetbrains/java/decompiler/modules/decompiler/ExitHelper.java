// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.code.cfg.BasicBlock;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.modules.decompiler.exps.ConstExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement.EdgeDirection;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class ExitHelper {
  public static boolean condenseExits(RootStatement root) {
    int changed = integrateExits(root);

    if (changed > 0) {
      cleanUpUnreachableBlocks(root);
      SequenceHelper.condenseSequences(root);
    }

    return (changed > 0);
  }

  private static void cleanUpUnreachableBlocks(Statement stat) {
    boolean found;
    do {
      found = false;

      for (int i = 0; i < stat.getStats().size(); i++) {
        Statement st = stat.getStats().get(i);

        cleanUpUnreachableBlocks(st);

        if (st instanceof SequenceStatement && st.getStats().size() > 1) {

          Statement last = st.getStats().getLast();
          Statement secondlast = st.getStats().get(st.getStats().size() - 2);

          if (last.getExprents() == null || !last.getExprents().isEmpty()) {
            if (!secondlast.hasBasicSuccEdge()) {

              Set<Statement> set = last.getNeighboursSet(Statement.STATEDGE_DIRECT_ALL, EdgeDirection.BACKWARD);
              set.remove(secondlast);

              if (set.isEmpty()) {
                last.setExprents(new ArrayList<>());
                found = true;
                break;
              }
            }
          }
        }
      }
    }
    while (found);
  }

  // Turns break edges into returns where possible.
  //
  // Example:
  //
  // label1: {
  //   if (...) {
  //     break label1;
  //   }
  //   ...
  // }
  // return;
  //
  // will turn into
  //
  // if (...) {
  //   return;
  // }
  // ...
  //
  private static int integrateExits(Statement stat) {
    int ret = 0;
    Statement dest;

    if (stat.getExprents() == null) {
      while (true) {
        int changed = 0;

        for (Statement st : stat.getStats()) {
          changed = integrateExits(st);
          if (changed > 0) {
            ret = 1;
            break;
          }
        }

        if (changed == 0) {
          break;
        }
      }

      if (stat instanceof IfStatement) {
        IfStatement ifst = (IfStatement)stat;
        if (ifst.getIfstat() == null) {
          StatEdge ifedge = ifst.getIfEdge();
          dest = isExitEdge(ifedge);
          if (dest != null) {
            BasicBlockStatement bstat = new BasicBlockStatement(new BasicBlock(
              DecompilerContext.getCounterContainer().getCounterAndIncrement(CounterContainer.STATEMENT_COUNTER)));
            bstat.setExprents(DecHelper.copyExprentList(dest.getExprents()));

            ifst.getFirst().removeSuccessor(ifedge);
            StatEdge newedge = new StatEdge(StatEdge.TYPE_REGULAR, ifst.getFirst(), bstat);
            ifst.getFirst().addSuccessor(newedge);
            ifst.setIfEdge(newedge);
            ifst.setIfstat(bstat);
            ifst.getStats().addWithKey(bstat, bstat.id);
            bstat.setParent(ifst);

            StatEdge oldexitedge = dest.getFirstSuccessor();
            StatEdge newexitedge = new StatEdge(StatEdge.TYPE_BREAK, bstat, oldexitedge.getDestination());
            bstat.addSuccessor(newexitedge);
            oldexitedge.closure.addLabeledEdge(newexitedge);
            ret = 1;
          }
        }
      }
    }


    if (stat.getAllSuccessorEdges().size() == 1 &&
        stat.getAllSuccessorEdges().get(0).getType() == StatEdge.TYPE_BREAK &&
        stat.getLabelEdges().isEmpty()) {
      Statement parent = stat.getParent();
      if (stat != parent.getFirst() || !(parent instanceof IfStatement ||
                                        parent instanceof SwitchStatement)) {

        StatEdge destedge = stat.getAllSuccessorEdges().get(0);
        dest = isExitEdge(destedge);
        if (dest != null) {
          stat.removeSuccessor(destedge);

          BasicBlockStatement bstat = new BasicBlockStatement(new BasicBlock(
            DecompilerContext.getCounterContainer().getCounterAndIncrement(CounterContainer.STATEMENT_COUNTER)));
          bstat.setExprents(DecHelper.copyExprentList(dest.getExprents()));

          StatEdge oldexitedge = dest.getAllSuccessorEdges().get(0);
          StatEdge newexitedge = new StatEdge(StatEdge.TYPE_BREAK, bstat, oldexitedge.getDestination());
          bstat.addSuccessor(newexitedge);
          oldexitedge.closure.addLabeledEdge(newexitedge);

          SequenceStatement block = new SequenceStatement(Arrays.asList(stat, bstat));
          block.setAllParent();

          parent.replaceStatement(stat, block);
          // LabelHelper.lowContinueLabels not applicable because of forward continue edges
          // LabelHelper.lowContinueLabels(block, new HashSet<StatEdge>());
          // do it by hand
          for (StatEdge prededge : block.getPredecessorEdges(StatEdge.TYPE_CONTINUE)) {
            block.removePredecessor(prededge);
            prededge.getSource().changeEdgeNode(EdgeDirection.FORWARD, prededge, stat);
            stat.addPredecessor(prededge);
            stat.addLabeledEdge(prededge);
          }

          stat.addSuccessor(new StatEdge(StatEdge.TYPE_REGULAR, stat, bstat));

          for (StatEdge edge : dest.getAllPredecessorEdges()) {
            if (!edge.explicit && stat.containsStatementStrict(edge.getSource()) &&
                MergeHelper.isDirectPath(edge.getSource().getParent(), bstat)) {

              dest.removePredecessor(edge);
              edge.getSource().changeEdgeNode(EdgeDirection.FORWARD, edge, bstat);
              bstat.addPredecessor(edge);

              if (!stat.containsStatementStrict(edge.closure)) {
                stat.addLabeledEdge(edge);
              }
            }
          }

          ret = 2;
        }
      }
    }

    return ret;
  }

  private static Statement isExitEdge(StatEdge edge) {
    Statement dest = edge.getDestination();

    if (edge.getType() == StatEdge.TYPE_BREAK && dest instanceof BasicBlockStatement && edge.explicit && (edge.labeled || isOnlyEdge(edge)) && edge.canInline) {
      List<Exprent> data = dest.getExprents();

      if (data != null && data.size() == 1) {
        if (data.get(0) instanceof ExitExprent) {
          return dest;
        }
      }
    }

    return null;
  }

  private static boolean isOnlyEdge(StatEdge edge) {
    Statement stat = edge.getDestination();

    for (StatEdge ed : stat.getAllPredecessorEdges()) {
      if (ed != edge) {
        if (ed.getType() == StatEdge.TYPE_REGULAR) {
          Statement source = ed.getSource();

          if (source instanceof BasicBlockStatement || (source instanceof IfStatement &&
                                                           ((IfStatement)source).iftype == IfStatement.IFTYPE_IF) ||
              (source instanceof DoStatement && ((DoStatement)source).getLooptype() != DoStatement.Type.INFINITE)) {
            return false;
          }
        }
        else {
          return false;
        }
      }
    }

    return true;
  }

  // Removes return statements from the ends of methods when they aren't returning a value
  public static boolean removeRedundantReturns(RootStatement root) {
    boolean res = false;
    DummyExitStatement dummyExit = root.getDummyExit();

    for (StatEdge edge : dummyExit.getAllPredecessorEdges()) {
      if (!edge.explicit) {
        Statement source = edge.getSource();
        List<Exprent> lstExpr = source.getExprents();
        if (lstExpr != null && !lstExpr.isEmpty()) {
          Exprent expr = lstExpr.get(lstExpr.size() - 1);
          if (expr instanceof ExitExprent) {
            ExitExprent ex = (ExitExprent)expr;
            if (ex.getExitType() == ExitExprent.Type.RETURN && ex.getValue() == null) {
              // remove redundant return
              dummyExit.addBytecodeOffsets(ex.bytecode);
              lstExpr.remove(lstExpr.size() - 1);
              res = true;
            }
          }
        }
      }
    }

    return res;
  }

  // Fixes chars being returned when ints are required
  public static boolean adjustReturnType(RootStatement root, MethodDescriptor desc) {
    boolean res = false;
    // Get all statements with returns
    for (StatEdge retEdge : root.getDummyExit().getAllPredecessorEdges()) {
      Statement ret = retEdge.getSource();

      // Get all exprent in statement
      List<Exprent> exprents = ret.getExprents();
      if (exprents != null && !exprents.isEmpty()) {
        // Get return exprent
        Exprent expr = exprents.get(exprents.size() - 1);
        if (expr instanceof ExitExprent) {
          ExitExprent ex = (ExitExprent) expr;

          List<Exprent> exitExprents = ex.getAllExprents(true);

          // If any of the return expression has constants, adjust them to the return type of the method
          for (Exprent exprent : exitExprents) {
            if (exprent instanceof ConstExprent) {
              ((ConstExprent)exprent).adjustConstType(desc.ret);
              res = true;
            }
          }
        }
      }
    }

    return res;
  }
}