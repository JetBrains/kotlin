package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.VarExprent;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectNode;
import org.jetbrains.java.decompiler.modules.decompiler.flow.FlattenStatementsHelper;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionNode;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionsGraph;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.DotExporter;
import org.jetbrains.java.decompiler.util.ListStack;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.util.*;

public final class ValidationHelper {
  private static final boolean VALIDATE = System.getProperty("VALIDATE_DECOMPILED_CODE", "false").equals("true");

  public static void validateStatement(RootStatement statement) {
    if (!VALIDATE) {
      return;
    }

    VBStyleCollection<Statement, Integer> statements = new VBStyleCollection<>();

    Deque<Statement> stack = new LinkedList<>();
    stack.push(statement.getDummyExit());
    stack.push(statement);

    while (!stack.isEmpty()) {
      Statement stat = stack.pop();

      statements.putWithKey(stat, stat.id);

      stack.addAll(stat.getStats());
    }

    for (Statement stat : statements) {
      for (StatEdge edge : stat.getAllSuccessorEdges()) {
        validateEdgeContext(statements, stat, edge);
      }

      for (StatEdge edge : stat.getAllPredecessorEdges()) {
        validateEdgeContext(statements, stat, edge);
      }

      for (StatEdge edge : stat.getLabelEdges()) {
        validateEdgeContext(statements, stat, edge);
      }

      if (stat.getExprents() != null) {
        for (Exprent exprent : stat.getExprents()) {
          validateExprent(exprent);
        }
      }

      if (!statements.contains(stat.getFirst()) && !(stat instanceof DummyExitStatement) && !(stat instanceof BasicBlockStatement)) {
        throw new IllegalStateException("Non-existing first statement: [" + stat + "] " + stat.getFirst());
      }

      for (Statement statStat : stat.getStats()) {
        if (statStat.getParent() != stat) {
          throw new IllegalStateException("Statement parent is not set correctly: " + statStat);
        }
      }

      validateSingleStatement(stat);

    }

    DirectGraph directGraph;
    try {
      FlattenStatementsHelper flatten = new FlattenStatementsHelper();
      directGraph = flatten.buildDirectGraph(statement);
    } catch (Throwable e) {
      throw new IllegalStateException("Failed to build direct graph", e);
    }

    validateDGraph(directGraph, statement);
  }

  private static void validateEdgeContext(VBStyleCollection<Statement, Integer> statements, Statement stat, StatEdge edge) {
    if (!statements.contains(edge.getSource())) {
      throw new IllegalStateException("Edge pointing from non-existing statement: [" + stat + "] " + edge);
    }

    if (!statements.contains(edge.getDestination())) {
      throw new IllegalStateException("Edge pointing to non-existing statement: [" + stat + "] " + edge);
    }

    if (edge.closure != null) {
      if (!statements.contains(edge.closure)) {
        throw new IllegalStateException("Edge with non-existing closure: [" + stat + "] " + edge);
      }
    }

    validateEdge(edge);
  }

  public static void validateEdge(StatEdge edge) {
    if (!VALIDATE) {
      return;
    }

    if (edge.labeled) {
      if (edge.closure == null) {
        // throw new IllegalStateException("Edge with label, but no closure: " + edge);
      }
    }

    if (!isSuccessor(edge.getSource(), edge)) {
      throw new IllegalStateException("Edge pointing from statement but it isn't a successor: " + edge.getSource() + " " + edge);
    }

    if (!edge.getDestination().getAllPredecessorEdges().contains(edge)) {
      throw new IllegalStateException("Edge pointing to statement but it isn't a predecessor: " + edge);
    }

    if (edge.labeled && edge.getType() == StatEdge.TYPE_BREAK) {
      if (!edge.getDestination().getLabelEdges().contains(edge)) {
        //ex = error(ex, "Edge with label, but the closure doesn't know: " + edge);
      }
    }

    switch (edge.getType()){
      case StatEdge.TYPE_REGULAR: {
        if (edge.closure != null) {
          // throw new IllegalStateException("Edge with closure, but it's a regular edge: " + edge);
        }
        break;
      }
      case StatEdge.TYPE_BREAK: {
        if (edge.closure == null) {
          throw new IllegalStateException("Break edge with break type, but no closure: " + edge);
        }

        if (edge.getSource() == edge.closure && !edge.phantomContinue) {
          throw new IllegalStateException("Break edge with closure pointing to itself: " + edge);
        }

        if (edge.getDestination() == edge.closure) {
          throw new IllegalStateException("Break edge with closure pointing to itself: " + edge);
        }

        if (edge.getSource() == edge.getDestination()) {
          throw new IllegalStateException("Break edge pointing to itself: " + edge);
        }

        // TODO: It seems there are break edge to dummy exits with
        //  potentially incorrect closures
        if (!(edge.getDestination() instanceof DummyExitStatement) && !MergeHelper.isDirectPath(edge.closure, edge.getDestination())) {
          throw new IllegalStateException("Break edge with closure with invalid direct path: " + edge);
        }

        // if (edge.closure.hasAnySuccessor() && edge.closure.getFirstSuccessor().getType() != StatEdge.TYPE_REGULAR) {
        //   throw new IllegalStateException("Break edge with closure with non-regular successor: " + edge + " " + edge.closure.getFirstSuccessor());
        // }

        break;
      }
      case StatEdge.TYPE_CONTINUE: {
        if (edge.closure == null) {
          throw new IllegalStateException("Continue edge with continue type, but no closure: " + edge);
        }

        if (edge.closure != edge.getDestination()) {
          throw new IllegalStateException("Continue edge with closure pointing to different destination: " + edge);
        }

        if (!(edge.getDestination() instanceof DoStatement)) {
          throw new IllegalStateException("Continue edge where closure isn't pointing to a do: " + edge);
        }

        break;
      }
    }
  }

  // not recursive
  public static void validateSingleStatement(Statement stat) {
    if (!VALIDATE) {
      return;
    }

    switch (stat.type) {
      case IF: validateIfStatement((IfStatement) stat); break;
      case TRY_CATCH: validateTrycatchStatement((CatchStatement) stat); break;
    }
  }

  public static void validateTrycatchStatement(CatchStatement catchStat) {
    if (catchStat.getStats().size() == 1 && catchStat.getResources().isEmpty()) {
      throw new IllegalStateException("Try statement with single statement: " + catchStat);
    }
  }

  public static void validateIfStatement(IfStatement ifStat) {
    if (!VALIDATE) {
      return;
    }

    final VBStyleCollection<Statement, Integer> stats = ifStat.getStats();

    if (ifStat.getFirst() == null) {
      throw new IllegalStateException("If statement without a first statement: " + ifStat);
    } else if (!stats.contains(ifStat.getFirst())) {
      throw new IllegalStateException("If statement does not contain own first statement: " + ifStat);
    }

    if (ifStat.getIfEdge() == null) {
      throw new IllegalStateException("If statement without an if edge: " + ifStat);
    }

    if (ifStat.getIfstat() != null) {
      if (ifStat.getIfEdge().getDestination() != ifStat.getIfstat()) {
        throw new IllegalStateException("If statement if edge destination is not ifStat: " + ifStat + " (destination is: " + ifStat.getIfEdge().getDestination() + " but ifStat is: " + ifStat.getIfstat() + ")");
      }

      if (!stats.contains(ifStat.getIfstat())) {
        throw new IllegalStateException("If statement does not contain own ifStat: " + ifStat);
      }
    }

    if (ifStat.iftype == IfStatement.IFTYPE_IF) {
      if (ifStat.getElseEdge() != null) {
        throw new IllegalStateException("If statement with unexpected else edge: " + ifStat);
      }
      if (ifStat.getSuccessorEdges(Statement.STATEDGE_DIRECT_ALL).isEmpty()){
        throw new IllegalStateException("If statement with no else edge and no successors: " + ifStat);
      } else if (ifStat.getSuccessorEdges(Statement.STATEDGE_DIRECT_ALL).size() > 1) {
        throw new IllegalStateException("If statement with more than one successor: " + ifStat + " (successors: " + ifStat.getSuccessorEdges(Statement.STATEDGE_DIRECT_ALL) + ")");
      }
    } else if (ifStat.iftype == IfStatement.IFTYPE_IFELSE) {
      if (ifStat.getElseEdge() == null) {
        throw new IllegalStateException("IfElse statement without else edge: " + ifStat);
      }

      if (ifStat.getIfstat() == null) {
        throw new IllegalStateException("IfElse statement without ifStat: " + ifStat);
      }

      if (ifStat.getElsestat() == null) {
        throw new IllegalStateException("IfElse statement without elseStat: " + ifStat);
      }

      if (ifStat.getElseEdge().getDestination() != ifStat.getElsestat()) {
        throw new IllegalStateException("IfElse statement else edge destination is not elseStat: " + ifStat);
      }

      if (!stats.contains(ifStat.getElsestat())) {
        throw new IllegalStateException("IfElse statement does not contain own elseStat: " + ifStat);
      }

      // This is a valid case to check, but much of fernflower is built around it so checking it is more trouble than it's worth
      // TODO: fix it properly sometime
//      if (!ifStat.getSuccessorEdges(StatEdge.TYPE_REGULAR).isEmpty()) {
//        throw new IllegalStateException("If-Else statement cannot have regular type successors: [" + ifStat + "] " + ifStat.getSuccessorEdges(StatEdge.TYPE_REGULAR));
//      }
    } else {
      throw new IllegalStateException("Unknown if type: " + ifStat);
    }

    if (ifStat.getIfEdge() != null && ifStat.getIfEdge().getSource() != ifStat.getFirst()) {
      throw new IllegalStateException("If statement if edge source is not first statement: [" + ifStat.getIfEdge() + "] " + ifStat + " (source is: " + ifStat.getIfEdge().getSource() + " but first is: " + ifStat.getFirst() + ")");
    }

    if (ifStat.getElseEdge() != null && ifStat.getElseEdge().getSource() != ifStat.getFirst()) {
      throw new IllegalStateException("IfElse statement else edge source is not first statement: " + ifStat + " (elseEdge: " + ifStat.getElseEdge() + ")");
    }

    if (stats.size() > 3){
      throw new IllegalStateException("If statement with more than 3 sub statements: " + ifStat);
    }

    for (Statement stat : stats) {
      if ( stat != ifStat.getFirst() && stat != ifStat.getIfstat() && stat != ifStat.getElsestat() ) {
        throw new IllegalStateException("If statement contains unknown sub statement: " + ifStat + " (subStatement: " + stat + ")");
      }
    }

    for (StatEdge edge : ifStat.getFirst().getSuccessorEdges(Statement.STATEDGE_DIRECT_ALL)) {
      if (ifStat.getIfEdge() != edge && ifStat.getElseEdge() != edge) {
        throw new IllegalStateException("If statement first contains unknown successor edge: " + ifStat + " (edge: " + edge + ")");
      }
    }
  }

  public static void validateDGraph(DirectGraph graph, RootStatement root) {
    if (!VALIDATE) {
      return;
    }

    try {
      Set<DirectNode> inaccessibleNodes = new HashSet<>(graph.nodes);

      ListStack<DirectNode> stack = new ListStack<>();
      stack.push(graph.first);
      inaccessibleNodes.remove(graph.first);

      while (!stack.isEmpty()) {
        DirectNode node = stack.pop();

        // check if predecessors have us as a successor
        for (DirectNode pred : node.preds()) {
          if (!pred.succs().contains(node)) {
            throw new IllegalStateException("Predecessor " + pred + " does not have " + node + " as a successor");
          }
        }

        // check if successors have us as a predecessor, and remove them from the inaccessible set
        for (DirectNode succ : node.succs()) {
          if (!succ.preds().contains(node)) {
            throw new IllegalStateException("Successor " + succ + " does not have " + node + " as a predecessor");
          }

          if (inaccessibleNodes.remove(succ)) {
            // if we find a new accessible node, add it to the stack
            stack.push(succ);
          }
        }
      }

      if (!inaccessibleNodes.isEmpty()) {
        throw new IllegalStateException("Inaccessible direct graph nodes: " + inaccessibleNodes);
      }
    } catch (Throwable e) {
      DotExporter.errorToDotFile(graph, root.mt, "erroring_dgraph");
      throw e;
    }
  }

  public static void validateAllVarVersionsAreNull(DirectGraph dgraph, RootStatement root) {
    if (!VALIDATE) {
      return;
    }

    try {
      for (DirectNode node : dgraph.nodes) {
        if (node.exprents != null) {
          for (Exprent exprent : node.exprents) {
            for (Exprent sub : exprent.getAllExprents(true, true)) {
              if (sub instanceof VarExprent) {
                VarExprent var = (VarExprent)sub;
                if (var.getVersion() != 0) {
                  throw new IllegalStateException("Var version is not zero: " + var.getIndex() + "_" + var.getVersion());
                }
              }
            }
          }
        }
      }
    } catch (Throwable e) {
      DotExporter.errorToDotFile(dgraph, root.mt, "erroring_dgraph");
      throw e;
    }
  }

  public static void notNull(Object o) {
    if (!VALIDATE) {
      return;
    }

    if (o == null) {
      throw new NullPointerException("Validation: null object");
    }
  }

  public static void validateExitExprent(ExitExprent exit) {
    if (!VALIDATE) {
      return;
    }

    if (exit.getExitType() == ExitExprent.Type.RETURN) {
      if (exit.getRetType().equals(VarType.VARTYPE_VOID)){
        if (exit.getValue() != null) {
          throw new IllegalStateException("Void return with value: " + exit);
        }
      } else {
        if (exit.getValue() == null) {
          throw new IllegalStateException("Non-void return without value: " + exit);
        }
      }
    }

    for (Exprent subExprents : exit.getAllExprents()) {
      validateExprent(subExprents);
    }
  }

  // recursive
  public static void validateExprent(Exprent exprent) {
    if (!VALIDATE) {
      return;
    }

    switch (exprent.type) {
      case EXIT: validateExitExprent((ExitExprent)exprent); break;
      default: {
        for (Exprent subExprents : exprent.getAllExprents()) {
          validateExprent(subExprents);
        }
      }
    }
  }

  public static void successorsExist(Statement stat) {
    if (!VALIDATE) {
      return;
    }

    if (stat.getAllSuccessorEdges().isEmpty()) {
      throw new IllegalStateException("Statement has no successors: " + stat);
    }
  }

  public static void oneSuccessor(Statement stat) {
    if (!VALIDATE) {
      return;
    }

    if (stat.getAllSuccessorEdges().size() != 1) {
      throw new IllegalStateException("Statement has more than one successor: [" + stat + "] " + stat.getAllSuccessorEdges());
    }
  }

  private static boolean isSuccessor(Statement source, StatEdge edge) {
    if (source.getAllSuccessorEdges().contains(edge)) return true;

    if (source.getParent() instanceof IfStatement) {
      IfStatement ifstat = (IfStatement) source.getParent();
      if (ifstat.getFirst() == source) {
        if (edge == ifstat.getIfEdge() || edge == ifstat.getElseEdge()) {
          return true;
        }
      }
    }

    return false;
  }

  public static void assertTrue(boolean condition, String message) {
    if (VALIDATE && !condition) {
      throw new IllegalStateException("Assertion failed: " + message);
    }
  }

  public static void validateVarVersionsGraph(
    VarVersionsGraph graph, RootStatement statement, HashMap<VarVersionPair, VarVersionPair> varAssignmentMap) {
    if (!VALIDATE) {
      return;
    }

    Set<VarVersionNode> roots = new HashSet<>();

    for (VarVersionNode node : graph.nodes) {
      if (node.preds.isEmpty()) {
        roots.add(node);
      }
    }

    Set<VarVersionNode> reached = graph.rootReachability(roots);

    if (graph.nodes.size() != reached.size()) {
      DotExporter.errorToDotFile(graph, statement.mt, "erroring_varVersionGraph", varAssignmentMap);
      throw new IllegalStateException("Highly cyclic varversions graph!");
    }
  }
}
