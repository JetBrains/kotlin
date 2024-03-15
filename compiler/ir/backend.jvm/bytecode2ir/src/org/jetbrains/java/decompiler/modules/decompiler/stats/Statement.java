/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.java.decompiler.modules.decompiler.stats;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.code.InstructionSequence;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.decompose.StrongConnectivityHelper;
import org.jetbrains.java.decompiler.modules.decompiler.ValidationHelper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.VarExprent;
import org.jetbrains.java.decompiler.struct.match.IMatchable;
import org.jetbrains.java.decompiler.struct.match.MatchEngine;
import org.jetbrains.java.decompiler.struct.match.MatchNode;
import org.jetbrains.java.decompiler.struct.match.MatchNode.RuleValue;
import org.jetbrains.java.decompiler.util.StartEndPair;
import org.jetbrains.java.decompiler.util.TextBuffer;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.util.*;
import java.util.Map.Entry;

public abstract class Statement implements IMatchable {
  public enum StatementType {
    ROOT("Root"), BASIC_BLOCK("Block"), SEQUENCE("Seq"), DUMMY_EXIT("Exit"),
    GENERAL("General"),
    IF("If"), DO("Do"), SWITCH("Switch"),
    SYNCHRONIZED("Monitor"), TRY_CATCH("Catch"), CATCH_ALL("CatchAll");

    private final String prettyId;

    StatementType(String prettyId) {
      this.prettyId = prettyId;
    }
  }
  // All edge types
  public static final int STATEDGE_ALL = 0x80000000;
  // All edge types minus exceptions
  // Exception edges are implicit from try contents to catch handlers, so they don't represent control flow
  public static final int STATEDGE_DIRECT_ALL = 0x40000000;

  private static final int[] EXCEPTION_EDGE_TYPES = new int[]{STATEDGE_ALL, StatEdge.TYPE_EXCEPTION};
  private static final int[] REGULAR_EDGE_TYPES = new int[]{STATEDGE_ALL, STATEDGE_DIRECT_ALL};

  public enum EdgeDirection {
    BACKWARD, // predecessors
    FORWARD, // successors
  }

  public enum LastBasicType {
    IF, SWITCH, GENERAL
  }


  // *****************************************************************************
  // public fields
  // *****************************************************************************

  public final StatementType type;

  public final int id;

  // *****************************************************************************
  // private fields
  // *****************************************************************************

  private final Map<Integer, List<StatEdge>> mapSuccEdges = new HashMap<>();
  private final Map<Integer, List<StatEdge>> mapPredEdges = new HashMap<>();

  // statement as graph
  protected final VBStyleCollection<Statement, Integer> stats = new VBStyleCollection<>();

  protected Statement parent;

  protected Statement first;

  protected List<Exprent> exprents;

  protected final HashSet<StatEdge> labelEdges = new HashSet<>();

  protected final List<Exprent> varDefinitions = new ArrayList<>();

  // copied statement, s. deobfuscating of irreducible CFGs
  private boolean copied = false;

  // relevant for the first stage of processing only
  // set to null after initializing of the statement structure

  protected Statement post;

  protected LastBasicType lastBasicType = LastBasicType.GENERAL;

  // Monitor flags
  protected boolean isMonitorEnter;
  protected boolean isLastAthrow;
  protected boolean containsMonitorExit;

  protected HashSet<Statement> continueSet = new HashSet<>();

  // *****************************************************************************
  // initializers
  // *****************************************************************************

  protected Statement(StatementType type, int id) {
    this.type = type;
    this.id = id;
  }

  protected Statement(StatementType type) {
    this(type, DecompilerContext.getCounterContainer().getCounterAndIncrement(CounterContainer.STATEMENT_COUNTER));
  }

  // *****************************************************************************
  // public methods
  // *****************************************************************************

  public void clearTempInformation() {

    post = null;
    continueSet = null;

    copied = false;
    // FIXME: used in FlattenStatementsHelper.flattenStatement()! check and remove
    //lastBasicType = LASTBASICTYPE_GENERAL;
    isMonitorEnter = false;
    containsMonitorExit = false;

    processMap(mapSuccEdges);
    processMap(mapPredEdges);
  }

  private static <T> void processMap(Map<Integer, List<T>> map) {
    map.remove(StatEdge.TYPE_EXCEPTION);

    List<T> lst = map.get(STATEDGE_DIRECT_ALL);
    if (lst != null) {
      map.put(STATEDGE_ALL, new ArrayList<>(lst));
    }
    else {
      map.remove(STATEDGE_ALL);
    }
  }

  public void collapseNodesToStatement(Statement stat) {

    Statement head = stat.getFirst();
    Statement post = stat.getPost();

    VBStyleCollection<Statement, Integer> setNodes = stat.getStats();

    // post edges
    if (post != null) {
      for (StatEdge edge : post.getEdges(STATEDGE_DIRECT_ALL, EdgeDirection.BACKWARD)) {
        if (stat.containsStatementStrict(edge.getSource())) {
          edge.getSource().changeEdgeType(EdgeDirection.FORWARD, edge, StatEdge.TYPE_BREAK);
          stat.addLabeledEdge(edge);
        }
      }
    }

    // regular head edges
    for (StatEdge prededge : head.getAllPredecessorEdges()) {

      if (prededge.getType() != StatEdge.TYPE_EXCEPTION &&
          stat.containsStatementStrict(prededge.getSource())) {
        prededge.getSource().changeEdgeType(EdgeDirection.FORWARD, prededge, StatEdge.TYPE_CONTINUE);
        stat.addLabeledEdge(prededge);
      }

      head.removePredecessor(prededge);
      prededge.getSource().changeEdgeNode(EdgeDirection.FORWARD, prededge, stat);
      stat.addPredecessor(prededge);
    }

    if (setNodes.containsKey(first.id)) {
      first = stat;
    }

    // exception edges
    Set<Statement> setHandlers = new HashSet<>(head.getNeighbours(StatEdge.TYPE_EXCEPTION, EdgeDirection.FORWARD));
    for (Statement node : setNodes) {
      setHandlers.retainAll(node.getNeighbours(StatEdge.TYPE_EXCEPTION, EdgeDirection.FORWARD));
    }

    if (!setHandlers.isEmpty()) {

      for (StatEdge edge : head.getEdges(StatEdge.TYPE_EXCEPTION, EdgeDirection.FORWARD)) {
        Statement handler = edge.getDestination();

        if (setHandlers.contains(handler)) {
          if (!setNodes.containsKey(handler.id)) {
            stat.addSuccessor(new StatEdge(stat, handler, edge.getExceptions()));
          }
        }
      }

      for (Statement node : setNodes) {
        for (StatEdge edge : node.getEdges(StatEdge.TYPE_EXCEPTION, EdgeDirection.FORWARD)) {
          if (setHandlers.contains(edge.getDestination())) {
            node.removeSuccessor(edge);
          }
        }
      }
    }

    if (post != null &&
        !stat.getNeighbours(StatEdge.TYPE_EXCEPTION, EdgeDirection.FORWARD).contains(post)) { // TODO: second condition redundant?
      stat.addSuccessor(new StatEdge(StatEdge.TYPE_REGULAR, stat, post));
    }


    // adjust statement collection
    for (Statement st : setNodes) {
      stats.removeWithKey(st.id);
    }

    stats.addWithKey(stat, stat.id);

    stat.setAllParent();
    stat.setParent(this);

    stat.buildContinueSet();
    // monitorenter and monitorexit
    stat.buildMonitorFlags();

    if (stat instanceof SwitchStatement) {
      // special case switch, sorting leaf nodes
      ((SwitchStatement)stat).sortEdgesAndNodes();
    }
  }

  public void setAllParent() {
    for (Statement st : stats) {
      st.setParent(this);
    }
  }

  public void addLabeledEdge(StatEdge edge) {
    if (edge.closure != null) {
      edge.closure.getLabelEdges().remove(edge);
    }

    edge.closure = this;
    this.getLabelEdges().add(edge);
  }

  private void addEdgeDirectInternal(EdgeDirection direction, StatEdge edge, int edgetype) {
    Map<Integer, List<StatEdge>> mapEdges = direction == EdgeDirection.BACKWARD ? mapPredEdges : mapSuccEdges;

    mapEdges.computeIfAbsent(edgetype, k -> new ArrayList<>()).add(edge);
  }

  @Deprecated // Only public so StatEdge can call these.
  public void addEdgeInternal(EdgeDirection direction, StatEdge edge) {
    int type = edge.getType();

    int[] arrtypes;
    if (type == StatEdge.TYPE_EXCEPTION) {
      arrtypes = EXCEPTION_EDGE_TYPES;
    } else {
      arrtypes = REGULAR_EDGE_TYPES;
      addEdgeDirectInternal(direction, edge, type);
    }

    for (int edgetype : arrtypes) {
      addEdgeDirectInternal(direction, edge, edgetype);
    }
  }

  private void removeEdgeDirectInternal(EdgeDirection direction, StatEdge edge, int edgetype) {

    Map<Integer, List<StatEdge>> mapEdges = direction == EdgeDirection.BACKWARD ? mapPredEdges : mapSuccEdges;

    List<StatEdge> lst = mapEdges.get(edgetype);
    if (lst != null) {
      int index = lst.indexOf(edge);
      if (index >= 0) {
        lst.remove(index);
      }
    }
  }

  @Deprecated // Only public so StatEdge can call these.
  public void removeEdgeInternal(EdgeDirection direction, StatEdge edge) {

    int type = edge.getType();

    int[] arrtypes;
    if (type == StatEdge.TYPE_EXCEPTION) {
      arrtypes = EXCEPTION_EDGE_TYPES;
    } else {
      arrtypes = REGULAR_EDGE_TYPES;
      removeEdgeDirectInternal(direction, edge, type);
    }

    for (int edgetype : arrtypes) {
      removeEdgeDirectInternal(direction, edge, edgetype);
    }
  }

  public void addPredecessor(StatEdge edge) {
    addEdgeInternal(EdgeDirection.BACKWARD, edge);
  }

  public void removePredecessor(StatEdge edge) {

    if (edge == null) {  // FIXME: redundant?
      return;
    }

    removeEdgeInternal(EdgeDirection.BACKWARD, edge);
  }

  public void addSuccessor(StatEdge edge) {
    addEdgeInternal(EdgeDirection.FORWARD, edge);

    if (edge.closure != null) {
      edge.closure.getLabelEdges().add(edge);
    }

    edge.getDestination().addPredecessor(edge);
  }

  public void removeSuccessor(StatEdge edge) {

    if (edge == null) {
      return;
    }

    removeEdgeInternal(EdgeDirection.FORWARD, edge);

    if (edge.closure != null) {
      edge.closure.getLabelEdges().remove(edge);
    }

    if (edge.getDestination() != null) {  // TODO: redundant?
      edge.getDestination().removePredecessor(edge);
    }
  }

  // TODO: make obsolete and remove
  public void removeAllSuccessors(Statement stat) {

    if (stat == null) {
      return;
    }

    for (StatEdge edge : getAllSuccessorEdges()) {
      if (edge.getDestination() == stat) {
        removeSuccessor(edge);
      }
    }
  }

  public HashSet<Statement> buildContinueSet() {
    continueSet.clear();

    for (Statement st : stats) {
      continueSet.addAll(st.buildContinueSet());
      if (st != first) {
        continueSet.remove(st.getBasichead());
      }
    }

    for (StatEdge edge : getEdges(StatEdge.TYPE_CONTINUE, EdgeDirection.FORWARD)) {
      continueSet.add(edge.getDestination().getBasichead());
    }

    if (this instanceof DoStatement) {
      continueSet.remove(first.getBasichead());
    }

    return continueSet;
  }

  public void buildMonitorFlags() {

    for (Statement st : stats) {
      st.buildMonitorFlags();
    }

    switch (type) {
      case BASIC_BLOCK:
        BasicBlockStatement bblock = (BasicBlockStatement)this;
        InstructionSequence seq = bblock.getBlock().getSeq();

        if (seq != null && seq.length() > 0) {
          for (int i = 0; i < seq.length(); i++) {
            if (seq.getInstr(i).opcode == CodeConstants.opc_monitorexit) {
              containsMonitorExit = true;
              break;
            }
          }
          isMonitorEnter = (seq.getLastInstr().opcode == CodeConstants.opc_monitorenter);
          isLastAthrow = (seq.getLastInstr().opcode == CodeConstants.opc_athrow);
        }
        break;

      case SYNCHRONIZED:
      case ROOT:
      case GENERAL:
        break;
      default:
        containsMonitorExit = false;
        isLastAthrow = false;
        for (Statement st : stats) {
          containsMonitorExit |= st.containsMonitorExit();
          isLastAthrow |= st.isLastAthrow;
        }
    }
  }

  public void markMonitorexitDead() {
    for (Statement st : this.stats) {
      st.markMonitorexitDead();
    }

    if (this instanceof BasicBlockStatement) {
      BasicBlockStatement bblock = (BasicBlockStatement)this;
      InstructionSequence seq = bblock.getBlock().getSeq();

      if (seq != null && !seq.isEmpty()) {
        for (int i = 0; i < seq.length(); i++) {
          if (seq.getInstr(i).opcode == CodeConstants.opc_monitorexit) {
            bblock.setRemovableMonitorexit(true);
            break;
          }
        }
      }
    }
  }


  public List<Statement> getReversePostOrderList() {
    return getReversePostOrderList(first);
  }

  public List<Statement> getReversePostOrderList(Statement stat) {
    List<Statement> res = new ArrayList<>();

    addToReversePostOrderListIterative(stat, res);

    return res;
  }

  public List<Statement> getPostReversePostOrderList() {
    return getPostReversePostOrderList(null);
  }

  public List<Statement> getPostReversePostOrderList(List<Statement> lstexits) {

    List<Statement> res = new ArrayList<>();

    if (lstexits == null) {
      StrongConnectivityHelper schelper = new StrongConnectivityHelper(this);
      lstexits = StrongConnectivityHelper.getExitReps(schelper.getComponents());
    }

    HashSet<Statement> setVisited = new HashSet<>();

    for (Statement exit : lstexits) {
      addToPostReversePostOrderList(exit, res, setVisited);
    }

    if (res.size() != stats.size()) {
      throw new RuntimeException("computing post reverse post order failed!");
    }

    return res;
  }

  public boolean containsStatement(Statement stat) {
    return this == stat || containsStatementStrict(stat);
  }

  public boolean containsStatementStrict(Statement stat) {
    if (stats.contains(stat)) {
      return true;
    }

    for (Statement st : stats) {
      if (st.containsStatementStrict(stat)) {
        return true;
      }
    }

    return false;
  }

  public boolean containsStatementById(int statId) {
    return this.id == statId || containsStatementStrictById(statId);
  }

  public boolean containsStatementStrictById(int statId) {
    for (Statement stat : stats) {
      if (stat.id == statId) {
        return true;
      }
    }

    for (Statement st : stats) {
      if (st.containsStatementStrictById(statId)) {
        return true;
      }
    }

    return false;
  }

  public TextBuffer toJava() {
    return toJava(0);
  }

  public TextBuffer toJava(int indent) {
    throw new RuntimeException("not implemented");
  }

  // TODO: make obsolete and remove
  public List<Object> getSequentialObjects() {
    return new ArrayList<>(stats);
  }

  public void initExprents() {
    // do nothing
  }

  public void replaceExprent(Exprent oldexpr, Exprent newexpr) {
    // do nothing
  }

  public Statement getSimpleCopy() {
    throw new RuntimeException("not implemented");
  }

  public void initSimpleCopy() {
    if (!stats.isEmpty()) {
      first = stats.get(0);
    }
  }

  public final void replaceWith(Statement stat) {
    this.parent.replaceStatement(this, stat);
  }

  public final BasicBlockStatement replaceWithEmpty() {
    BasicBlockStatement newStat = BasicBlockStatement.create();
    replaceWith(newStat);
    return newStat;
  }

  public void replaceStatement(Statement oldstat, Statement newstat) {
    if (!stats.containsKey(oldstat.id)) {
      throw new IllegalStateException("[" + this + "] Cannot replace " + oldstat + " with " + newstat + " because it wasn't found in " + stats);
    }

    for (StatEdge edge : oldstat.getAllPredecessorEdges()) {
      oldstat.removePredecessor(edge);
      edge.getSource().changeEdgeNode(EdgeDirection.FORWARD, edge, newstat);
      newstat.addPredecessor(edge);
    }

    for (StatEdge edge : oldstat.getAllSuccessorEdges()) {
      oldstat.removeSuccessor(edge);
      edge.setSource(newstat);
      newstat.addSuccessor(edge);
    }

    int statindex = stats.getIndexByKey(oldstat.id);
    stats.removeWithKey(oldstat.id);
    stats.addWithKeyAndIndex(statindex, newstat, newstat.id);

    newstat.setParent(this);
    newstat.post = oldstat.post;

    if (first == oldstat) {
      first = newstat;
    }

    List<StatEdge> lst = new ArrayList<>(oldstat.getLabelEdges());

    for (int i = lst.size() - 1; i >= 0; i--) {
      StatEdge edge = lst.get(i);
      if (edge.getSource() != newstat) {
        newstat.addLabeledEdge(edge);
      }
      else {
        if (this == edge.getDestination() || this.containsStatementStrict(edge.getDestination())) {
          edge.closure = null;
        }
        else {
          this.addLabeledEdge(edge);
        }
      }
    }

    replaceClosure(this, oldstat, newstat);

    oldstat.getLabelEdges().clear();
  }

  private static void replaceClosure(Statement stat, Statement oldstat, Statement newstat) {
    for (StatEdge edge : stat.getAllSuccessorEdges()) {
      if (edge.closure == oldstat) {
        edge.closure = newstat;
      }
    }

    for (Statement st : stat.getStats()) {
      replaceClosure(st, oldstat, newstat);
    }
  }

  /**
   * Gets the implicitly defined variables in this statement.
   *
   * @return A list of {@link VarExprent}s that are implicitly defined. Can be null or empty if none exist.
   */
  public List<VarExprent> getImplicitlyDefinedVars() {
    return null;
  }


  // *****************************************************************************
  // private methods
  // *****************************************************************************

  private static void addToReversePostOrderListIterative(Statement root, List<? super Statement> lst) {

    LinkedList<Statement> stackNode = new LinkedList<>();
    LinkedList<Integer> stackIndex = new LinkedList<>();
    HashSet<Statement> setVisited = new HashSet<>();

    stackNode.add(root);
    stackIndex.add(0);

    while (!stackNode.isEmpty()) {

      Statement node = stackNode.getLast();
      int index = stackIndex.removeLast();

      setVisited.add(node);

      List<StatEdge> lstEdges = node.mapSuccEdges.computeIfAbsent(STATEDGE_ALL, k -> new ArrayList<>());

      for (; index < lstEdges.size(); index++) {
        StatEdge edge = lstEdges.get(index);
        Statement succ = edge.getDestination();

        if (!setVisited.contains(succ) &&
            (edge.getType() == StatEdge.TYPE_REGULAR || edge.getType() == StatEdge.TYPE_EXCEPTION)) { // TODO: edge filter?

          stackIndex.add(index + 1);

          stackNode.add(succ);
          stackIndex.add(0);

          break;
        }
      }

      if (index == lstEdges.size()) {
        lst.add(0, node);

        stackNode.removeLast();
      }
    }
  }


  private static void addToPostReversePostOrderList(Statement stat, List<? super Statement> lst, HashSet<? super Statement> setVisited) {

    if (setVisited.contains(stat)) { // because of not considered exception edges, s. isExitComponent. Should be rewritten, if possible.
      return;
    }

    setVisited.add(stat);

    for (StatEdge prededge : stat.mapPredEdges.computeIfAbsent(StatEdge.TYPE_REGULAR, t -> new ArrayList<>())) {
      Statement pred = prededge.getSource();

      if (!setVisited.contains(pred)) {
        addToPostReversePostOrderList(pred, lst, setVisited);
      }
    }

    for (StatEdge prededge : stat.mapPredEdges.computeIfAbsent(StatEdge.TYPE_EXCEPTION, t -> new ArrayList<>())) {
      Statement pred = prededge.getSource();

      if (!setVisited.contains(pred)) {
        addToPostReversePostOrderList(pred, lst, setVisited);
      }
    }

    lst.add(0, stat);
  }

  // *****************************************************************************
  // getter and setter methods
  // *****************************************************************************

  public void changeEdgeNode(EdgeDirection direction, StatEdge edge, Statement value) {
    if (direction == EdgeDirection.BACKWARD) {
      edge.setSource(value);
    }
    else {
      edge.setDestination(value);
    }
  }

  public void changeEdgeType(EdgeDirection direction, StatEdge edge, int newtype) {

    int oldtype = edge.getType();
    if (oldtype == newtype) {
      return;
    }

    if (oldtype == StatEdge.TYPE_EXCEPTION || newtype == StatEdge.TYPE_EXCEPTION) {
      throw new RuntimeException("Invalid edge type!");
    }

    removeEdgeDirectInternal(direction, edge, oldtype);
    addEdgeDirectInternal(direction, edge, newtype);

    if (direction == EdgeDirection.FORWARD) {
      edge.getDestination().changeEdgeType(EdgeDirection.BACKWARD, edge, newtype);
    }

    edge.setType(newtype);
  }


  private List<StatEdge> getEdges(int type, EdgeDirection direction) {

    Map<Integer, List<StatEdge>> map = direction == EdgeDirection.BACKWARD ? mapPredEdges : mapSuccEdges;

    List<StatEdge> res;
    if ((type & (type - 1)) == 0) {
      res = map.get(type);
      res = res == null ? new ArrayList<>() : new ArrayList<>(res);
    }
    else {
      res = new ArrayList<>();
      for (int edgetype : StatEdge.TYPES) {
        if ((type & edgetype) != 0) {
          List<StatEdge> lst = map.get(edgetype);
          if (lst != null) {
            res.addAll(lst);
          }
        }
      }
    }

    return res;
  }

  public List<Statement> getNeighbours(int type, EdgeDirection direction) {

    Map<Integer, List<StatEdge>> map = direction == EdgeDirection.BACKWARD ? mapPredEdges : mapSuccEdges;

    List<Statement> res = new ArrayList<>();
    if ((type & (type - 1)) == 0) {
      List<StatEdge> statEdges = map.get(type);

      if (statEdges != null) {
        for (StatEdge edge : statEdges) {
          res.add(direction == EdgeDirection.FORWARD ? edge.getDestination() : edge.getSource());
        }
      }
    }
    else {
      res = new ArrayList<>();
      for (int edgetype : StatEdge.TYPES) {
        if ((type & edgetype) != 0) {
          List<StatEdge> lst = map.get(edgetype);

          if (lst != null) {
            for (StatEdge edge : lst) {
              res.add(direction == EdgeDirection.FORWARD ? edge.getDestination() : edge.getSource());
            }
          }
        }
      }
    }

    return res;
  }

  public Set<Statement> getNeighboursSet(int type, EdgeDirection direction) {
    return new HashSet<>(getNeighbours(type, direction));
  }

  public List<StatEdge> getSuccessorEdges(int type) {
    return getEdges(type, EdgeDirection.FORWARD);
  }

  // Do not mutate this map!
  public List<StatEdge> getSuccessorEdgeView(int type) {
    return this.mapSuccEdges.computeIfAbsent(type, k -> new ArrayList<>());
  }

  public List<StatEdge> getPredecessorEdges(int type) {
    return getEdges(type, EdgeDirection.BACKWARD);
  }

  public List<StatEdge> getAllSuccessorEdges() {
    return getEdges(STATEDGE_ALL, EdgeDirection.FORWARD);
  }

  public boolean hasAnySuccessor() {
    return hasSuccessor(STATEDGE_ALL);
  }

  public boolean hasSuccessor(int type) {
    Map<Integer, List<StatEdge>> map = mapSuccEdges;

    boolean res = false;
    if ((type & (type - 1)) == 0) {
      List<StatEdge> edges = map.get(type);
      res = edges != null && !edges.isEmpty();
    } else {
      for (int edgetype : StatEdge.TYPES) {
        if ((type & edgetype) != 0) {
          List<StatEdge> lst = map.get(edgetype);

          if (lst != null) {
            res = !lst.isEmpty();

            if (res) {
              return true;
            }
          }
        }
      }
    }

    return res;
  }

  public StatEdge getFirstSuccessor() {
    ValidationHelper.successorsExist(this);
    // TODO: does this make sense here?
//    ValidationHelper.oneSuccessor(this);

    List<StatEdge> res = this.mapSuccEdges.get(STATEDGE_ALL);
    if (res != null) {
      for (StatEdge e : res) {
        return e;
      }
    }

    throw new IllegalStateException("No successor exists for " + this);
  }

  public List<StatEdge> getAllPredecessorEdges() {
    return getEdges(STATEDGE_ALL, EdgeDirection.BACKWARD);
  }

  public Statement getFirst() {
    return first;
  }

  public void setFirst(Statement first) {
    this.first = first;
  }

  public Statement getPost() {
    return post;
  }

  public VBStyleCollection<Statement, Integer> getStats() {
    return stats;
  }

  public LastBasicType getLastBasicType() {
    return lastBasicType;
  }

  public HashSet<Statement> getContinueSet() {
    return continueSet;
  }

  public boolean containsMonitorExit() {
    return containsMonitorExit;
  }

  public boolean containsMonitorExitOrAthrow() {
    return this.containsMonitorExit || this.isLastAthrow;
  }

  public boolean isMonitorEnter() {
    return isMonitorEnter;
  }

  public BasicBlockStatement getBasichead() {
    if (this instanceof BasicBlockStatement) {
      return (BasicBlockStatement)this;
    } else {
      return first.getBasichead();
    }
  }

  public boolean isLabeled() {

    for (StatEdge edge : labelEdges) {
      if (edge.labeled && edge.explicit) {  // FIXME: consistent setting
        return true;
      }
    }
    return false;
  }

  // Whether this statement has a successor to a basic block or not. Conditions:
  // Basic blocks are always connected to the next basic block
  // single-if statements are connected to the next block (from implicit else)
  // Loops are connected to the next block if it's not an infinite while(true){}
  // TODO: many while(true) loops have breaks in their body. Would that not count?
  //       however allowing those seems to break tests.
  public boolean hasBasicSuccEdge() {

    // FIXME: default switch

    switch (this.type) {
      case BASIC_BLOCK: return true;
      case IF: return (((IfStatement) this).iftype == IfStatement.IFTYPE_IF);
      case DO: return ((DoStatement) this).getLooptype() != DoStatement.Type.INFINITE;
      default: return false;
    }
  }


  public Statement getParent() {
    return parent;
  }

  public void setParent(Statement parent) {
    this.parent = parent;
  }

  public RootStatement getTopParent() {
    Statement ret = this;

    while (ret.getParent() != null) {
      ret = ret.getParent();
    }

    if (!(ret instanceof RootStatement)) {
      throw new IllegalStateException("Top parent is not a root statement! Malformed IR?");
    }

    return (RootStatement) ret;
  }

  public HashSet<StatEdge> getLabelEdges() {  // FIXME: why HashSet?
    return labelEdges;
  }

  public List<Exprent> getVarDefinitions() {
    return varDefinitions;
  }

  public List<Exprent> getExprents() {
    return exprents;
  }

  public void setExprents(List<Exprent> exprents) {
    this.exprents = exprents;
  }

  public boolean isCopied() {
    return copied;
  }

  public void setCopied(boolean copied) {
    this.copied = copied;
  }

  // helper methods
  public String toString() {
    return "{" + type.prettyId + "}:" + id;
  }

  //TODO: Cleanup/cache?
  public void getOffset(BitSet values) {
    if (this instanceof DummyExitStatement && ((DummyExitStatement)this).bytecode != null)
      values.or(((DummyExitStatement)this).bytecode);
    if (this.getExprents() != null) {
      for (Exprent e : this.getExprents()) {
        e.getBytecodeRange(values);
      }
    } else {
      for (Object obj : this.getSequentialObjects()) {
        if (obj instanceof Statement) {
          ((Statement)obj).getOffset(values);
        } else if (obj instanceof Exprent) {
          ((Exprent)obj).getBytecodeRange(values);
        } else if (obj != null) {
          DecompilerContext.getLogger().writeMessage("Found unknown class from sequential objects! " + obj.getClass(), IFernflowerLogger.Severity.ERROR);
        }
      }
    }
  }

  private StartEndPair endpoints;
  public StartEndPair getStartEndRange() {
    if (endpoints == null) {
      BitSet set = new BitSet();
      getOffset(set);
      endpoints = new StartEndPair(set.nextSetBit(0), set.length() - 1);
    }
    return endpoints;
  }


  // *****************************************************************************
  // IMatchable implementation
  // *****************************************************************************

  @Override
  public IMatchable findObject(MatchNode matchNode, int index) {
    int node_type = matchNode.getType();

    if (node_type == MatchNode.MATCHNODE_STATEMENT && !this.stats.isEmpty()) {
      String position = (String) matchNode.getRuleValue(MatchProperties.STATEMENT_POSITION);
      if (position != null) {
        if (position.matches("-?\\d+")) {
          return this.stats.get((this.stats.size() + Integer.parseInt(position)) % this.stats.size()); // care for negative positions
        }
      } else if (index < this.stats.size()) { // use 'index' parameter
        return this.stats.get(index);
      }
    } else if (node_type == MatchNode.MATCHNODE_EXPRENT && this.exprents != null && !this.exprents.isEmpty()) {
      String position = (String) matchNode.getRuleValue(MatchProperties.EXPRENT_POSITION);
      if (position != null) {
        if (position.matches("-?\\d+")) {
          return this.exprents.get((this.exprents.size() + Integer.parseInt(position)) % this.exprents.size()); // care for negative positions
        }
      } else if (index < this.exprents.size()) { // use 'index' parameter
        return this.exprents.get(index);
      }
    }

    return null;
  }

  @Override
  public boolean match(MatchNode matchNode, MatchEngine engine) {
    if (matchNode.getType() != MatchNode.MATCHNODE_STATEMENT) {
      return false;
    }

    for (Entry<MatchProperties, RuleValue> rule : matchNode.getRules().entrySet()) {
      switch (rule.getKey()) {
        case STATEMENT_TYPE:
          if (this.type != rule.getValue().value) {
            return false;
          }
          break;
        case STATEMENT_STATSIZE:
          if (this.stats.size() != (Integer)rule.getValue().value) {
            return false;
          }
          break;
        case STATEMENT_EXPRSIZE:
          int exprsize = (Integer)rule.getValue().value;
          if (exprsize == -1) {
            if (this.exprents != null) {
              return false;
            }
          }
          else {
            if (this.exprents == null || this.exprents.size() != exprsize) {
              return false;
            }
          }
          break;
        case STATEMENT_RET:
          if (!engine.checkAndSetVariableValue((String)rule.getValue().value, this)) {
            return false;
          }
          break;
      }
    }

    return true;
  }
}