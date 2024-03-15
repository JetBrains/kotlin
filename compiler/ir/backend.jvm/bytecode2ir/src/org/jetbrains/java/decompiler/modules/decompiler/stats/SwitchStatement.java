// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.stats;

import org.jetbrains.java.decompiler.code.SwitchInstruction;
import org.jetbrains.java.decompiler.code.cfg.BasicBlock;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.decompiler.DecHelper;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.ValidationHelper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.StartEndPair;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.*;
import java.util.stream.Collectors;

public final class SwitchStatement extends Statement {

  // *****************************************************************************
  // private fields
  // *****************************************************************************

  private List<Statement> caseStatements = new ArrayList<>();

  private List<List<StatEdge>> caseEdges = new ArrayList<>();

  private List<List<Exprent>> caseValues = new ArrayList<>();

  private List<Exprent> caseGuards = new ArrayList<>();

  private final Set<Statement> scopedCaseStatements = new HashSet<>();

  private StatEdge defaultEdge;

  private final List<Exprent> headexprent = new ArrayList<>(1);

  // *****************************************************************************
  // constructors
  // *****************************************************************************

  // Phantom when converted to a switch expression. Spooky!
  // We need to do this because switch expressions can have code in their case values, so we need to preserve the statement graph.
  // The resulting statement isn't shown in the actual decompile (unless enabled specifically!)
  private boolean phantom;

  private SwitchStatement() {
    super(StatementType.SWITCH);

    headexprent.add(null);
  }

  private SwitchStatement(Statement head, Statement poststat) {

    this();

    first = head;
    stats.addWithKey(head, head.id);

    // find post node
    Set<Statement> lstNodes = new HashSet<>(head.getNeighbours(StatEdge.TYPE_REGULAR, EdgeDirection.FORWARD));

    // cluster nodes
    if (poststat != null) {
      post = poststat;
      lstNodes.remove(post);
    }

    defaultEdge = head.getSuccessorEdges(Statement.STATEDGE_DIRECT_ALL).get(0);

    //We need to use set above in case we have multiple edges to the same node. But HashSets iterator is not ordered, so sort
    List<Statement> sorted = new ArrayList<>(lstNodes);
    sorted.sort(Comparator.comparingInt(o -> o.id));
    for (Statement st : sorted) {
      stats.addWithKey(st, st.id);
    }
  }

  // *****************************************************************************
  // public methods
  // *****************************************************************************

  public static Statement isHead(Statement head) {

    if (head instanceof BasicBlockStatement && head.getLastBasicType() == LastBasicType.SWITCH) {

      List<Statement> lst = new ArrayList<>();
      if (DecHelper.isChoiceStatement(head, lst)) {
        Statement post = lst.remove(0);

        for (Statement st : lst) {
          if (st.isMonitorEnter()) {
            return null;
          }
        }

        if (DecHelper.checkStatementExceptions(lst)) {
          return new SwitchStatement(head, post);
        }
      }
    }

    return null;
  }

  @Override
  public TextBuffer toJava(int indent) {

    TextBuffer buf = new TextBuffer();
    buf.append(ExprProcessor.listToJava(varDefinitions, indent));
    buf.append(first.toJava(indent));

    boolean showPhantom = DecompilerContext.getOption(IFernflowerPreferences.SHOW_HIDDEN_STATEMENTS);

    // Is phantom and we don't want to show- just return what we have so far
    if (this.isPhantom() && !showPhantom) {
      return buf;
    }

    if (isLabeled()) {
      buf.appendIndent(indent).append("label").append(this.id).append(":").appendLineSeparator();
    }

    buf.appendIndent(indent);
    if (this.isPhantom()) {
      buf.append("/*");
    }

    buf.append(headexprent.get(0).toJava(indent)).append(" {").appendLineSeparator();

    VarType switch_type = headexprent.get(0).getExprType();

    for (int i = 0; i < caseStatements.size(); i++) {

      Statement stat = caseStatements.get(i);
      List<StatEdge> edges = caseEdges.get(i);
      List<Exprent> values = caseValues.get(i);
      Exprent guard = caseGuards.size() > i ? caseGuards.get(i) : null;

      for (int j = 0; j < edges.size(); j++) {
        if (edges.get(j) == defaultEdge) {
          buf.appendIndent(indent + 1).append("default:");
          if (this.scopedCaseStatements.contains(stat) && j == edges.size() - 1) {
            buf.append(" {");
          }

          buf.appendLineSeparator();
        } else {
          Exprent value = values.get(j);
          if (value == null) { // TODO: how can this be null? Is it trying to inject a synthetic case value in switch-on-string processing? [TestSwitchDefaultBefore]
            continue;
          }

          buf.appendIndent(indent + 1).append("case ");

          if (value instanceof ConstExprent && !value.getExprType().equals(VarType.VARTYPE_NULL)) {
            value = value.copy();
            ((ConstExprent)value).setConstType(switch_type);
          } if (value instanceof FieldExprent && ((FieldExprent)value).isStatic()) { // enum values
            buf.append(((FieldExprent)value).getName());
          } else if (value instanceof FunctionExprent && ((FunctionExprent) value).getFuncType() == FunctionType.INSTANCEOF) {
            // Pattern matching variables
            List<Exprent> operands = ((FunctionExprent) value).getLstOperands();
            buf.append(operands.get(1).toJava(indent));
            buf.append(" ");
            // We're pasting the var type, don't do it again
            ((VarExprent)operands.get(2)).setDefinition(false);
            buf.append(operands.get(2).toJava(indent));
          } else {
            buf.append(value.toJava(indent));
          }

          if (guard != null) {
            buf.append(" when ").append(guard.toJava());
          }

          buf.append(":");
          if (this.scopedCaseStatements.contains(stat) && j == edges.size() - 1) {
            buf.append(" {");
          }
          buf.appendLineSeparator();
        }
      }

      buf.append(ExprProcessor.jmpWrapper(stat, indent + 2, false));

      if (this.scopedCaseStatements.contains(stat)) {
        buf.appendIndent(indent + 1);
        buf.append("}");
        buf.appendLineSeparator();
      }
    }

    buf.appendIndent(indent).append("}").appendLineSeparator();

    if (this.isPhantom()) {
      buf.append("*/");
    }

    return buf;
  }

  // Needed for flatten statements
  public Statement findCaseBranchContaining(int id) {
    for (Statement st : this.caseStatements) {
      if (st.containsStatementById(id)) {
        return st;
      }
    }

    return null;
  }

  @Override
  public void initExprents() {
    SwitchHeadExprent swexpr = (SwitchHeadExprent)first.getExprents().remove(first.getExprents().size() - 1);
    swexpr.setCaseValues(caseValues);

    headexprent.set(0, swexpr);
  }

  @Override
  public List<Object> getSequentialObjects() {

    List<Object> lst = new ArrayList<>(stats);
    lst.add(1, headexprent.get(0));
    // make sure guards can be simplified by other helpers
    for (Exprent caseGuard : getCaseGuards()) {
      if (caseGuard != null) {
        lst.add(caseGuard);
      }
    }

    for (List<Exprent> caseList : this.caseValues) {
      lst.addAll(caseList);
    }

    return lst;
  }

  // Returns true if this switch is a pattern matching switch.
  public boolean isPattern() {
    // Simple test, if there's a guard then it is for sure pattern matching
    if (!this.caseGuards.isEmpty()) {
      return true;
    }

    for (List<Exprent> l : this.caseValues) {
      for (Exprent e : l) {
        // If we have instanceofs in our case values, we're a pattern matching switch
        if (e instanceof FunctionExprent && ((FunctionExprent)e).getFuncType() == FunctionType.INSTANCEOF) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public List<VarExprent> getImplicitlyDefinedVars() {
    List<VarExprent> vars = new ArrayList<>();

    List<Exprent> caseList = this.caseValues.stream()
      .flatMap(List::stream) // List<List<Exprent>> -> List<Exprent>
      .collect(Collectors.toList());
    // guards can also contain pattern variables
    caseList.addAll(this.caseGuards);
    // guards may also contain nested variables, like `a instanceof B b && b == ...`
    caseList = caseList.stream()
      .filter(Objects::nonNull)
      .flatMap(x -> x.getAllExprents(true, true).stream())
      .collect(Collectors.toList());

    for (Exprent caseContent : caseList) {
      if (caseContent == null) {
        continue;
      }

      if (caseContent instanceof FunctionExprent) {
        FunctionExprent func = ((FunctionExprent) caseContent);

        // Pattern match variable is implicitly defined
        if (func.getFuncType() == FunctionType.INSTANCEOF && func.getLstOperands().size() > 2) {
          vars.add((VarExprent) func.getLstOperands().get(2));
        }
      }
    }

    return vars;
  }

  @Override
  public void replaceExprent(Exprent oldexpr, Exprent newexpr) {
    if (headexprent.get(0) == oldexpr) {
      headexprent.set(0, newexpr);
    } else {
      int idx = caseGuards.indexOf(oldexpr);
      if (idx > -1) {
        caseGuards.set(idx, newexpr);
      }
    }
  }

  @Override
  public void replaceStatement(Statement oldstat, Statement newstat) {

    boolean changedAny = false;
    for (int i = 0; i < caseStatements.size(); i++) {
      if (caseStatements.get(i) == oldstat) {
        caseStatements.set(i, newstat);
        changedAny = true;
      }
    }

    ValidationHelper.assertTrue(changedAny, "Replaced statement in switch without changing any case statements!");

    super.replaceStatement(oldstat, newstat);
  }

  @Override
  public Statement getSimpleCopy() {
    return new SwitchStatement();
  }

  @Override
  public void initSimpleCopy() {
    first = stats.get(0);
    defaultEdge = first.getSuccessorEdges(Statement.STATEDGE_DIRECT_ALL).get(0);

    sortEdgesAndNodes();
  }

  @Override
  public StartEndPair getStartEndRange() {
    StartEndPair[] sepairs = new StartEndPair[caseStatements.size() + 1];
    int i = 0;
    sepairs[i++] = super.getStartEndRange();
    for (Statement st : caseStatements) {
      sepairs[i++] = st.getStartEndRange();
    }
    return StartEndPair.join(sepairs);
  }

  // *****************************************************************************
  // private methods
  // *****************************************************************************

  public void sortEdgesAndNodes() {

    // skip for pattern switches
    if (caseValues.stream().flatMap(Collection::stream).anyMatch(u -> !(u instanceof ConstExprent) || ((ConstExprent) u).isNull())
      || caseGuards.stream().anyMatch(Objects::nonNull)) {
      return;
    }

    HashMap<StatEdge, Integer> mapEdgeIndex = new HashMap<>();

    List<StatEdge> lstFirstSuccs = first.getSuccessorEdges(STATEDGE_DIRECT_ALL);
    for (int i = 0; i < lstFirstSuccs.size(); i++) {
      mapEdgeIndex.put(lstFirstSuccs.get(i), i == 0 ? lstFirstSuccs.size() : i);
    }

    // case values
    BasicBlockStatement bbstat = (BasicBlockStatement)first;
    int[] values = ((SwitchInstruction)bbstat.getBlock().getLastInstruction()).getValues();

    List<Statement> nodes = new ArrayList<>(stats.size() - 1);
    List<List<Integer>> edges = new ArrayList<>(stats.size() - 1);

    // collect regular edges
    for (int i = 1; i < stats.size(); i++) {

      Statement stat = stats.get(i);

      List<Integer> lst = new ArrayList<>();
      for (StatEdge edge : stat.getPredecessorEdges(StatEdge.TYPE_REGULAR)) {
        if (edge.getSource() == first) {
          lst.add(mapEdgeIndex.get(edge));
        }
      }
      Collections.sort(lst);

      nodes.add(stat);
      edges.add(lst);
    }

    // collect exit edges
    List<StatEdge> lstExitEdges = first.getSuccessorEdges(StatEdge.TYPE_BREAK | StatEdge.TYPE_CONTINUE);
    while (!lstExitEdges.isEmpty()) {
      StatEdge edge = lstExitEdges.get(0);

      List<Integer> lst = new ArrayList<>();
      for (int i = lstExitEdges.size() - 1; i >= 0; i--) {
        StatEdge edgeTemp = lstExitEdges.get(i);
        if (edgeTemp.getDestination() == edge.getDestination() && edgeTemp.getType() == edge.getType()) {
          lst.add(mapEdgeIndex.get(edgeTemp));
          lstExitEdges.remove(i);
        }
      }
      Collections.sort(lst);

      nodes.add(null);
      edges.add(lst);
    }

    // sort edges (bubblesort)
    for (int i = 0; i < edges.size() - 1; i++) {
      for (int j = edges.size() - 1; j > i; j--) {
        if (edges.get(j - 1).get(0) > edges.get(j).get(0)) {
          edges.set(j, edges.set(j - 1, edges.get(j)));
          nodes.set(j, nodes.set(j - 1, nodes.get(j)));
        }
      }
    }

    // sort statement cliques
    for (int index = 0; index < nodes.size(); index++) {
      Statement stat = nodes.get(index);

      if (stat != null) {
        HashSet<Statement> setPreds = new HashSet<>(stat.getNeighbours(StatEdge.TYPE_REGULAR, EdgeDirection.BACKWARD));
        setPreds.remove(first);

        if (!setPreds.isEmpty()) {
          Statement pred =
            setPreds.iterator().next(); // assumption: at most one predecessor node besides the head. May not hold true for obfuscated code.
          for (int j = 0; j < nodes.size(); j++) {
            if (j != (index - 1) && nodes.get(j) == pred) {
              nodes.add(j + 1, stat);
              edges.add(j + 1, edges.get(index));

              if (j > index) {
                nodes.remove(index);
                edges.remove(index);
                index--;
              }
              else {
                nodes.remove(index + 1);
                edges.remove(index + 1);
              }
              break;
            }
          }
        }
      }
    }

    // translate indices back into edges
    List<List<StatEdge>> lstEdges = new ArrayList<>(edges.size());
    List<List<Exprent>> lstValues = new ArrayList<>(edges.size());

    for (List<Integer> lst : edges) {
      List<StatEdge> lste = new ArrayList<>(lst.size());
      List<Exprent> lstv = new ArrayList<>(lst.size());

      List<StatEdge> lstSuccs = first.getSuccessorEdges(STATEDGE_DIRECT_ALL);
      for (Integer in : lst) {
        int index = in == lstSuccs.size() ? 0 : in;

        lste.add(lstSuccs.get(index));
        lstv.add(index == 0 ? null : new ConstExprent(values[index - 1], false, null));
      }
      lstEdges.add(lste);
      lstValues.add(lstv);
    }

    // replace null statements with dummy basic blocks
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i) == null) {
        BasicBlockStatement bstat = new BasicBlockStatement(new BasicBlock(
          DecompilerContext.getCounterContainer().getCounterAndIncrement(CounterContainer.STATEMENT_COUNTER)));

        StatEdge sample_edge = lstEdges.get(i).get(0);

        bstat.addSuccessor(new StatEdge(sample_edge.getType(), bstat, sample_edge.getDestination(), sample_edge.closure));

        for (StatEdge edge : lstEdges.get(i)) {

          edge.getSource().changeEdgeType(EdgeDirection.FORWARD, edge, StatEdge.TYPE_REGULAR);
          edge.closure.getLabelEdges().remove(edge);

          edge.getDestination().removePredecessor(edge);
          edge.getSource().changeEdgeNode(EdgeDirection.FORWARD, edge, bstat);
          bstat.addPredecessor(edge);
        }

        nodes.set(i, bstat);
        stats.addWithKey(bstat, bstat.id);
        bstat.setParent(this);
      }
    }

    caseStatements = nodes;
    caseEdges = lstEdges;
    caseValues = lstValues;
  }

  public List<Exprent> getHeadexprentList() {
    return headexprent;
  }

  public Exprent getHeadexprent() {
    return headexprent.get(0);
  }

  public List<List<StatEdge>> getCaseEdges() {
    return caseEdges;
  }

  public List<Statement> getCaseStatements() {
    return caseStatements;
  }

  public StatEdge getDefaultEdge() {
    return defaultEdge;
  }

  public List<List<Exprent>> getCaseValues() {
    return caseValues;
  }

  public boolean isPhantom() {
    return phantom;
  }

  public void setPhantom(boolean phantom) {
    this.phantom = phantom;
  }

  public List<Exprent> getCaseGuards() {
    return caseGuards;
  }

  public void scopeCaseStatement(Statement stat) {
    if (!this.getCaseStatements().contains(stat)) {
      throw new IllegalStateException("Tried to scope a case statement that isn't in the switch!");
    }

    this.scopedCaseStatements.add(stat);
  }
}
