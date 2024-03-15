package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public final class SwitchExpressionHelper {

  public static boolean processSwitchExpressions(Statement root) {
    boolean ret = processSwitchExpressionsRec(root);

    if (ret) {
      SequenceHelper.condenseSequences(root);
    }

    return ret;
  }

  private static boolean processSwitchExpressionsRec(Statement stat) {
    boolean ret = false;
    for (Statement st : new ArrayList<>(stat.getStats())) {
      ret |= processSwitchExpressionsRec(st);
    }

    if (stat instanceof SwitchStatement) {
      ret |= processStatement((SwitchStatement) stat);
    }

    return ret;
  }

  private static boolean processStatement(SwitchStatement stat) {
    if (stat.isPhantom()) {
      return false;
    }

    // At this stage, there are no variable definitions
    // So we need to figure out which variable, if any, this switch statement is an expression of and make it generate.

    Exprent condition = ((SwitchHeadExprent) stat.getHeadexprent()).getValue();
    if (condition instanceof InvocationExprent) {
      InvocationExprent invoc = (InvocationExprent) condition;
      if (invoc.getName().equals("hashCode") && invoc.getClassname().equals("java/lang/String")) {
        return false; // We don't want to make switch expressions yet as switch processing hasn't happened
      }
    }

    // analyze all case statements with breaks to find the var we want. if it's found in all statements with breaks, and no other var is also found, we can make switch expressions
    Pair<Statement, List<Statement>> nextData = findNextData(stat);
    if (nextData == null) {
      return false;
    }

    List<Statement> breakJumps = nextData.b;

    Set<Statement> check = new HashSet<>();
    check.addAll(breakJumps);
    check.addAll(stat.getCaseStatements());

    for (Statement st : check) {
      if (!st.hasBasicSuccEdge()) {
        continue;
      }

      List<StatEdge> breaks = st.getSuccessorEdges(StatEdge.TYPE_BREAK);

      if (breaks.isEmpty()) {
        return false; // TODO: handle switch expression with fallthrough!
      }


      StatEdge firstBreak = breaks.get(0);

      // If the closure isn't our statement and we're not returning, break
      if (!stat.containsStatement(firstBreak.closure) && !(firstBreak.getDestination() instanceof DummyExitStatement)) {
        return false;
      }
    }

    Set<StatEdge> temp = new HashSet<>();
    List<Statement> caseStatements = stat.getCaseStatements();
    for (int i = 0; i < caseStatements.size(); i++) {
      Statement st = caseStatements.get(i);
      temp.clear();

      // Find all edges leaving the switch statements
      TryWithResourcesProcessor.findEdgesLeaving(st, stat, temp, true);

      boolean sawBreak = false;
      for (StatEdge edge : temp) {
        // Filter breaks
        if (edge.getType() == StatEdge.TYPE_BREAK) {
          // Breaks must go to the next statement, unless it goes to the exit (i.e. throws)
          if (edge.getDestination() != nextData.a && !(edge.getDestination() instanceof DummyExitStatement)) {
            return false;
          }

          // Edges must be explicit, unless it's the last case statement, which can contain an implicit break
          if (!edge.explicit && i != caseStatements.size() - 1) {
            return false;
          }

          // Record that we saw a break
          sawBreak = true;
        }
      }

      // We need at least one break, otherwise there is fallthrough- can't create switch expressions here
      if (!sawBreak) {
        return false;
      }
    }

    Map<Statement, List<VarVersionPair>> assignments = mapAssignments(breakJumps);

    // Must have found a return if it's null
    if (assignments == null) {
      return false;
    }

    boolean foundDefault = stat.getCaseEdges().stream()
      .flatMap(List::stream) // List<List<StatEdge>> -> List<StatEdge>
      .anyMatch(e -> e == stat.getDefaultEdge()); // Has default edge

//    for (Statement statement : assignments.keySet()) {
//      if (stat.getDefaultEdge().getDestination().containsStatement(statement)) {
//        foundDefault = true;
//        break;
//      }
//    }

    // Need default always!
    if (!foundDefault) {
      return false;
    }

    VarExprent relevantVar = findRelevantVar(assignments);

    // No var found
    if (relevantVar == null) {
      return false;
    }

    Set<StatEdge> edges = new HashSet<>();
    for (Statement caseStat : stat.getCaseStatements()) {
      TryWithResourcesProcessor.findEdgesLeaving(caseStat, caseStat, edges);

      for (StatEdge edge : edges) {
        // There's a continue- can't be a switch expression
        if (edge.getType() == StatEdge.TYPE_CONTINUE) {
          return false;
        }
      }

      edges.clear();
    }

    List<StatEdge> sucs = stat.getSuccessorEdges(StatEdge.TYPE_REGULAR);

    // TODO: should we be using getbasichead?
    if (!sucs.isEmpty()) {

      Statement suc = sucs.get(0).getDestination();
      if (!(suc instanceof BasicBlockStatement)) { // make basic block if it isn't found
        Statement oldSuc = suc;

        suc = BasicBlockStatement.create();
        SequenceStatement seq = new SequenceStatement(stat, suc);

        seq.setParent(stat.getParent());

        stat.replaceWith(seq);

        seq.setAllParent();

        // Replace successors with the new basic block
        for (Statement st : stat.getCaseStatements()) {
          for (StatEdge edge : st.getAllSuccessorEdges()) {
            if (edge.getDestination() == oldSuc) {
              st.removeSuccessor(edge);

              st.addSuccessor(new StatEdge(edge.getType(), st, suc, stat));
            }
          }
        }

        // Control flow from new basic block to the next one
        suc.addSuccessor(new StatEdge(StatEdge.TYPE_REGULAR, suc, oldSuc, seq));
      }

      stat.setPhantom(true);

      for (Statement st : stat.getCaseStatements()) {
        Map<Exprent, YieldExprent> replacements = new HashMap<>();

        findReplacements(st, relevantVar.getVarVersionPair(), replacements);

        // Replace exprents that we found
        if (!replacements.isEmpty()) {
          // Replace the assignments with yields, this allows 2 things:
          // 1) Not having to replace the assignments later on
          // 2) Preventing the variable assignment tracker from putting variable definitions too early because the assignments are no longer in the phantom statement
          replace(st, replacements);
        }
      }

      List<Exprent> exprents = suc.getExprents();

      VarExprent vExpr = new VarExprent(relevantVar.getIndex(), relevantVar.getVarType(), relevantVar.getProcessor());
      vExpr.setStack(true); // We want to inline
      AssignmentExprent toAdd = new AssignmentExprent(vExpr, new SwitchExprent(stat, relevantVar.getExprType(), false, false), null);

      exprents.add(0, toAdd);

      // move exprents from switch head to successor
      List<Exprent> firstExprents = stat.getFirst().getExprents();
      if (firstExprents != null && !firstExprents.isEmpty()) {
        int i = 0;
        for (Iterator<Exprent> iterator = firstExprents.iterator(); iterator.hasNext(); ) {
          Exprent ex = iterator.next();
          if (ex instanceof AssignmentExprent && ((AssignmentExprent) ex).getLeft() instanceof VarExprent) {
            if (((VarExprent) ((AssignmentExprent) ex).getLeft()).isStack()) {
              exprents.add(i, ex);
              i++;
              iterator.remove();
            }
          }
        }
      }

      return true;
    }

    return false;
  }

  private static void findReplacements(Statement stat, VarVersionPair var, Map<Exprent, YieldExprent> replacements) {
    if (stat.getExprents() != null) {
      for (Exprent e : stat.getExprents()) {
        // Check for "var10000 = <value>" within the exprents
        if (e instanceof AssignmentExprent) {
          AssignmentExprent assign = ((AssignmentExprent) e);

          if (assign.getLeft() instanceof VarExprent) {
            if (((VarExprent) assign.getLeft()).getIndex() == var.var) {
              // Make yield with the right side of the assignment
              replacements.put(assign, new YieldExprent(assign.getRight(), assign.getExprType()));
            }
          }
        }
      }
    }

    for (Statement st : stat.getStats()) {
      findReplacements(st, var, replacements);
    }
  }

  private static void replace(Statement stat, Map<Exprent, YieldExprent> replacements) {
    for (Map.Entry<Exprent, YieldExprent> entry : replacements.entrySet()) {
      stat.replaceExprent(entry.getKey(), entry.getValue());
    }

    for (Statement st : stat.getStats()) {
      replace(st, replacements);
    }
  }

  // Find data for switch expression creation, <next statement, {break sources}>
  private static Pair<Statement, List<Statement>> findNextData(SwitchStatement stat) {
    List<StatEdge> edges = stat.getSuccessorEdges(StatEdge.TYPE_REGULAR);
    Statement check = stat.getParent();
    while (edges.isEmpty()) {
      edges = check.getSuccessorEdges(StatEdge.TYPE_REGULAR);
      check = check.getParent();

      if (check == null) {
        return null;
      }
    }

    StatEdge edge = edges.get(0);
    Statement next = edge.getDestination();

    List<StatEdge> breaks = next.getPredecessorEdges(StatEdge.TYPE_BREAK);

    // Add returns
    breaks.addAll((stat.getTopParent()).getDummyExit().getPredecessorEdges(StatEdge.TYPE_BREAK));

    // Remove breaks that didn't come from our switch statement nodes
    breaks.removeIf(e -> !stat.containsStatement(e.getSource()));

    // Return all sources
    return Pair.of(next, breaks.stream().map(StatEdge::getSource).collect(Collectors.toList()));
  }

  // Find relevant assignments within blocks
  // List is null if there is a throw.
  private static Map<Statement, List<VarVersionPair>> mapAssignments(List<Statement> breakJumps) {
    Map<Statement, List<VarVersionPair>> map = new HashMap<>();

    for (Statement breakJump : breakJumps) {
      List<Exprent> exprents = breakJump.getExprents();

      if (exprents != null && !exprents.isEmpty()) {
        if (exprents.size() == 1 && exprents.get(0) instanceof ExitExprent) {
          ExitExprent exit = ((ExitExprent) exprents.get(0));

          // Special case throws
          if (exit.getExitType() == ExitExprent.Type.THROW) {
            map.put(breakJump, null);
            continue;
          } else {
            return null;
          }
        }

        List<VarVersionPair> list = new ArrayList<>();
        // Iterate in reverse, as we want the last assignment to be the one that we set the switch expression to
        for (int i = exprents.size() - 1; i >= 0; i--) {
          Exprent exprent = exprents.get(i);
          if (exprent instanceof AssignmentExprent) {
            AssignmentExprent assign = (AssignmentExprent) exprent;

            if (assign.getLeft() instanceof VarExprent) {
              VarExprent var = ((VarExprent) assign.getLeft());

              list.add(var.getVarVersionPair());
              continue;
            }
          }
          break;
        }

        map.put(breakJump, list);
      }
    }

    return map;
  }

  // Null if none found
  private static VarExprent findRelevantVar(Map<Statement, List<VarVersionPair>> assignments) {
    List<List<VarVersionPair>> values = new ArrayList<>(assignments.values());

    boolean consistentlyMoreThan1 = true;

    // Find any blocks with no assignments at all
    for (List<VarVersionPair> value : values) {
      if (value == null) {
        continue;
      }

      if (value.isEmpty()) {
        return null;
      }

      if (value.size() == 1) {
        consistentlyMoreThan1 = false;
      }
    }

    if (consistentlyMoreThan1) {
      return null;
    }

    List<VarVersionPair> firstNotNull = null;
    for (List<VarVersionPair> value : values) {
      if (value == null) {
        continue;
      }

      firstNotNull = value;
      break;
    }

    // Only made up of exceptions- quite funny but we should probably make a switch expression here?
    if (firstNotNull == null) {
      return null;
    }

    VarVersionPair check = firstNotNull.get(0);

    for (List<VarVersionPair> value : values) {
      if (value == null) {
        continue;
      }

      if (!value.get(0).equals(check)) {
        return null;
      }
    }

    for (Map.Entry<Statement, List<VarVersionPair>> entry : assignments.entrySet()) {
      if (entry.getValue() == firstNotNull) {
        List<Exprent> exprents = entry.getKey().getExprents();
        Exprent exprent = exprents.get(exprents.size() - 1);
        return ((VarExprent) ((AssignmentExprent) exprent).getLeft());
      }
    }

    return null;
  }

  public static boolean hasSwitchExpressions(RootStatement statement) {
    return statement.mt.getBytecodeVersion().hasSwitchExpressions() && DecompilerContext.getOption(IFernflowerPreferences.SWITCH_EXPRESSIONS);
  }
}
