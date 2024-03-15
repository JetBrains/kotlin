// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.modules.decompiler.stats.*;

import java.util.List;

public class LowBreakHelper {

  public static void lowBreakLabels(Statement root) {

    lowBreakLabelsRec(root);

    liftBreakLabels(root);
  }

  private static void lowBreakLabelsRec(Statement stat) {

    while (true) {

      boolean found = false;

      for (StatEdge edge : stat.getLabelEdges()) {
        if (edge.getType() == StatEdge.TYPE_BREAK) {
          Statement minclosure = getMinClosure(stat, edge.getSource());
          if (minclosure != stat) {
            minclosure.addLabeledEdge(edge);
            edge.labeled = isBreakEdgeLabeled(edge.getSource(), minclosure);
            found = true;
            break;
          }
        }
      }

      if (!found) {
        break;
      }
    }

    for (Statement st : stat.getStats()) {
      lowBreakLabelsRec(st);
    }
  }

  public static boolean isBreakEdgeLabeled(Statement source, Statement closure) {

    if (closure instanceof DoStatement || closure instanceof SwitchStatement) {

      Statement parent = source.getParent();

      if (parent == closure) {
        return false;
      }
      else {
        return isBreakEdgeLabeled(parent, closure) || (parent instanceof DoStatement || parent instanceof SwitchStatement);
      }
    }
    else {
      return true;
    }
  }

  public static Statement getMinClosure(Statement closure, Statement source) {

    while (true) {

      Statement newclosure = null;

      switch (closure.type) {
        case SEQUENCE:
          Statement last = closure.getStats().getLast();

          if (isOkClosure(closure, source, last)) {
            newclosure = last;
          }
          break;
        case IF:
          IfStatement ifclosure = (IfStatement)closure;
          if (isOkClosure(closure, source, ifclosure.getIfstat())) {
            newclosure = ifclosure.getIfstat();
          }
          else if (isOkClosure(closure, source, ifclosure.getElsestat())) {
            newclosure = ifclosure.getElsestat();
          }
          break;
        case TRY_CATCH:
          for (Statement st : closure.getStats()) {
            if (isOkClosure(closure, source, st)) {
              newclosure = st;
              break;
            }
          }
          break;
        case SYNCHRONIZED:
          Statement body = ((SynchronizedStatement)closure).getBody();

          if (isOkClosure(closure, source, body)) {
            newclosure = body;
          }
      }

      if (newclosure == null) {
        break;
      }

      closure = newclosure;
    }

    return closure;
  }

  private static boolean isOkClosure(Statement closure, Statement source, Statement stat) {

    boolean ok = false;

    if (stat != null && stat.containsStatementStrict(source)) {

      List<StatEdge> lst = stat.getAllSuccessorEdges();

      ok = lst.isEmpty();
      if (!ok) {
        StatEdge edge = lst.get(0);
        ok = (edge.closure == closure && edge.getType() == StatEdge.TYPE_BREAK);
      }
    }

    return ok;
  }


  private static void liftBreakLabels(Statement stat) {

    for (Statement st : stat.getStats()) {
      liftBreakLabels(st);
    }


    while (true) {

      boolean found = false;

      for (StatEdge edge : stat.getLabelEdges()) {
        if (edge.explicit && edge.labeled && edge.getType() == StatEdge.TYPE_BREAK) {

          Statement newclosure = getMaxBreakLift(stat, edge);

          if (newclosure != null) {
            newclosure.addLabeledEdge(edge);
            edge.labeled = isBreakEdgeLabeled(edge.getSource(), newclosure);

            found = true;
            break;
          }
        }
      }

      if (!found) {
        break;
      }
    }
  }

  private static Statement getMaxBreakLift(Statement stat, StatEdge edge) {

    Statement closure = null;
    Statement newclosure = stat;

    while ((newclosure = getNextBreakLift(newclosure, edge)) != null) {
      closure = newclosure;
    }

    return closure;
  }

  private static Statement getNextBreakLift(Statement stat, StatEdge edge) {

    Statement closure = stat.getParent();

    while (closure != null && !closure.containsStatementStrict(edge.getDestination())) {

      boolean labeled = isBreakEdgeLabeled(edge.getSource(), closure);
      if (closure.isLabeled() || !labeled) {
        return closure;
      }

      closure = closure.getParent();
    }

    return null;
  }
}