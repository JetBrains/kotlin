// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.flow;

import org.jetbrains.java.decompiler.api.GraphFlattener;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.util.DotExporter;
import org.jetbrains.java.decompiler.util.ListStack;

import java.util.*;
import java.util.Map.Entry;


public class FlattenStatementsHelper implements GraphFlattener {
  private static final int SWITCH_CONST = 1000000;

  // statement.id, node.id(direct), node.id(continue)
  private final Map<Integer, String[]> mapDestinationNodes = new HashMap<>();

  // node.id(source), statement.id(destination), edge type
  private final List<Edge> listEdges = new ArrayList<>();

  // node.id(exit), [node.id(source), statement.id(destination)]
  private final Map<String, List<String[]>> mapShortRangeFinallyPathIds = new HashMap<>();

  // node.id(exit), [node.id(source), statement.id(destination)]
  private final Map<String, List<String[]>> mapLongRangeFinallyPathIds = new HashMap<>();

  // positive if branches
  private final Map<String, Integer> mapPosIfBranch = new HashMap<>();

  private final ListStack<List<DirectNode>> tryNodesStack = new ListStack<>();

  private DirectGraph graph;

  private RootStatement root;

  public DirectGraph buildDirectGraph(RootStatement root) {

    this.root = root;

    graph = new DirectGraph();

    flattenStatement();

    // dummy exit node
    Statement dummyexit = root.getDummyExit();
    DirectNode node = this.createDirectNode(dummyexit);
    mapDestinationNodes.put(dummyexit.id, new String[]{node.id, null});

    setEdges();

    graph.first = graph.nodes.getWithKey(mapDestinationNodes.get(root.id)[0]);
    graph.sortReversePostOrder();

    graph.mapDestinationNodes.putAll(mapDestinationNodes);

    return graph;
  }

  private DirectNode createDirectNode(Statement stat) {
    final DirectNode directNode = this.createDirectNode(stat, DirectNodeType.DIRECT);
    if (stat instanceof BasicBlockStatement) {
      directNode.block = (BasicBlockStatement) stat;
    }

    return directNode;
  }

  private DirectNode createDirectNode(Statement stat, DirectNodeType type) {
    DirectNode node = DirectNode.forStat(type, stat);
    this.graph.nodes.addWithKey(node, node.id);

    if (!this.tryNodesStack.isEmpty()) {
      this.tryNodesStack.peek().add(node);
      // the try itself will put all nodes in the next try too
    }

    return node;
  }

  private void flattenStatement() {

    class StatementStackEntry {
      public final Statement statement;
      public final LinkedList<StackEntry> stackFinally;
      public final List<Exprent> tailExprents;

      public int statementIndex;
      public int edgeIndex;
      public List<StatEdge> succEdges;

      StatementStackEntry(Statement statement, LinkedList<StackEntry> stackFinally, List<Exprent> tailExprents) {
        this.statement = statement;
        this.stackFinally = stackFinally;
        this.tailExprents = tailExprents;
      }
    }

    LinkedList<StatementStackEntry> lstStackStatements = new LinkedList<>();

    lstStackStatements.add(new StatementStackEntry(root, new LinkedList<>(), null));

    mainloop:
    while (!lstStackStatements.isEmpty()) {

      StatementStackEntry statEntry = lstStackStatements.removeFirst();

      Statement stat = statEntry.statement;

      LinkedList<StackEntry> stackFinally = statEntry.stackFinally;
      int statementBreakIndex = statEntry.statementIndex;

      DirectNode node, nd;

      List<StatEdge> lstSuccEdges = new ArrayList<>();
      DirectNode sourcenode = null;

      if (statEntry.succEdges == null) {

        switch (stat.type) {
          case BASIC_BLOCK:
            node = this.createDirectNode(stat);

            if (stat.getExprents() != null) {
              node.exprents = stat.getExprents();
            }
            mapDestinationNodes.put(stat.id, new String[]{node.id, null});

            lstSuccEdges.addAll(stat.getSuccessorEdges(Statement.STATEDGE_DIRECT_ALL));
            sourcenode = node;

            List<Exprent> tailExprentList = statEntry.tailExprents;

            if (tailExprentList != null) {
              DirectNode tail = this.createDirectNode(stat, DirectNodeType.TAIL);
              graph.nodes.putWithKey(tail, tail.id);
              tail.exprents = tailExprentList;

              mapDestinationNodes.put(-stat.id, new String[]{tail.id, null});
              listEdges.add(new Edge(node.id, -stat.id, StatEdge.TYPE_REGULAR));

              sourcenode = tail;
            }

            // 'if' statement: record positive branch
            if (stat.getLastBasicType() == Statement.LastBasicType.IF) {
              if (lstSuccEdges.isEmpty()) {
                throw new IllegalStateException("Empty successor list for node " + sourcenode.id);
              }

              mapPosIfBranch.put(sourcenode.id, lstSuccEdges.get(0).getDestination().id);
            }

            List<StatEdge> basicPreds = stat.getAllPredecessorEdges();

            // TODO: sourcenode instead of stat.id?
            if (basicPreds.size() == 1) {
              StatEdge predEdge = basicPreds.get(0);

              // Look if this basic block is the successor of a sequence, and connect the sequence to the block if so
              if (predEdge.getType() == StatEdge.TYPE_REGULAR) {
                if (predEdge.getSource() instanceof SequenceStatement) {
                  addEdgeIfPossible(predEdge.getSource().getBasichead().id, stat);
                }
              }
            }

            break;
          case CATCH_ALL:
          case TRY_CATCH:
            if (statementBreakIndex == 0) {
              DirectNode firstnd = this.createDirectNode(stat, DirectNodeType.TRY);

              if (stat instanceof CatchStatement) {
                CatchStatement catchStat = (CatchStatement) stat;
                List<Exprent> resources = catchStat.getResources();
                if (!resources.isEmpty()) {
                  firstnd.exprents = resources;
                }
              }

              mapDestinationNodes.put(stat.id, new String[]{firstnd.id, null});

              LinkedList<StatementStackEntry> lst = new LinkedList<>();

              for (Statement st : stat.getStats()) {
                listEdges.add(new Edge(firstnd.id, st.id, StatEdge.TYPE_REGULAR));

                LinkedList<StackEntry> stack = stackFinally;
                if (stat instanceof CatchAllStatement && ((CatchAllStatement) stat).isFinally()) {
                  stack = new LinkedList<>(stackFinally);

                  if (st == stat.getFirst()) { // try block
                    stack.add(new StackEntry((CatchAllStatement) stat, false));
                  } else { // handler
                    stack.add(new StackEntry((CatchAllStatement) stat, true, StatEdge.TYPE_BREAK,
                      root.getDummyExit(), st, st, firstnd, firstnd, true));
                  }
                }

                lst.add(new StatementStackEntry(st, stack, null));
                if (st == stat.getFirst()) { // try block
                  lst.add(statEntry);
                  statEntry.statementIndex = 1;
                }
              }

              lstStackStatements.addAll(0, lst);

              this.tryNodesStack.add(new ArrayList<>());
            } else {
              // just finished try block
              List<DirectNode> tryNodes = this.tryNodesStack.pop();
              List<Statement> statements = stat.getStats();

              int end = stat instanceof CatchAllStatement && ((CatchAllStatement) stat).isFinally()
                ? statements.size() - 1
                : statements.size();

              for (int i = 1; i < end; i++) {
                for (DirectNode tryNode : tryNodes) {
                  listEdges.add(new Edge(tryNode.id, statements.get(i).id, StatEdge.TYPE_EXCEPTION));
                }
              }

              if (!this.tryNodesStack.isEmpty()) {
                this.tryNodesStack.peek().addAll(tryNodes);
              }
            }
            break;
          case DO:
            if (statementBreakIndex == 0) { // First time encountering this statement

              statEntry.statementIndex = 1;
              lstStackStatements.addFirst(statEntry);
              lstStackStatements.addFirst(new StatementStackEntry(stat.getFirst(), stackFinally, null));

              if (!stat.hasBasicSuccEdge()) { // infinite loop
                if (stat.hasSuccessor(StatEdge.TYPE_REGULAR)) {
                  // Infinite loop having a regular successor is invalid, but can occur
                  Statement dest = stat.getSuccessorEdges(StatEdge.TYPE_REGULAR).get(0).getDestination();

                  if (dest.getAllPredecessorEdges().size() == 1) {
                    // If the successor only has one backedge, it is the current loop
                    List<StatEdge> prededges = stat.getPredecessorEdges(StatEdge.TYPE_REGULAR);

                    if (!prededges.isEmpty()) {
                      StatEdge prededge = prededges.get(0);

                      // Find destinations of loop's predecessor

                      addEdgeIfPossible(prededge.getSource().id, dest);
                    }
                  }
                }
              }

              continue mainloop;
            }

            nd = graph.nodes.getWithKey(mapDestinationNodes.get(stat.getFirst().id)[0]);

            DoStatement dostat = (DoStatement) stat;
            DoStatement.Type looptype = dostat.getLooptype();

            if (looptype == DoStatement.Type.INFINITE) {
              mapDestinationNodes.put(stat.id, new String[]{nd.id, nd.id});
              break;
            }

            lstSuccEdges.add(stat.getSuccessorEdges(Statement.STATEDGE_DIRECT_ALL).get(0));  // exactly one edge

            switch (looptype) {
              case WHILE:
              case DO_WHILE:
                node = this.createDirectNode(stat, DirectNodeType.CONDITION);
                node.exprents = dostat.getConditionExprentList();

                listEdges.add(new Edge(node.id, stat.getFirst().id, StatEdge.TYPE_REGULAR));

                if (looptype == DoStatement.Type.WHILE) {
                  mapDestinationNodes.put(stat.id, new String[]{node.id, node.id});
                } else {
                  mapDestinationNodes.put(stat.id, new String[]{nd.id, node.id});

                  boolean found = false;
                  for (Edge edge : listEdges) {
                    if (edge.statid.equals(stat.id) && edge.edgetype == StatEdge.TYPE_CONTINUE) {
                      found = true;
                      break;
                    }
                  }
                  if (!found) {
                    listEdges.add(new Edge(nd.id, stat.id, StatEdge.TYPE_CONTINUE));
                  }
                }
                sourcenode = node;
                break;
              case FOR: {
                DirectNode nodeinit = this.createDirectNode(stat, DirectNodeType.INIT);
                if (dostat.getInitExprent() != null) {
                  nodeinit.exprents = dostat.getInitExprentList();
                }

                DirectNode nodecond = this.createDirectNode(stat, DirectNodeType.CONDITION);
                nodecond.exprents = dostat.getConditionExprentList();

                DirectNode nodeinc = this.createDirectNode(stat, DirectNodeType.INCREMENT);
                nodeinc.exprents = dostat.getIncExprentList();

                mapDestinationNodes.put(stat.id, new String[]{nodeinit.id, nodeinc.id});
                mapDestinationNodes.put(-stat.id, new String[]{nodecond.id, null});

                listEdges.add(new Edge(nodecond.id, stat.getFirst().id, StatEdge.TYPE_REGULAR));
                listEdges.add(new Edge(nodeinit.id, -stat.id, StatEdge.TYPE_REGULAR));
                listEdges.add(new Edge(nodeinc.id, -stat.id, StatEdge.TYPE_REGULAR));

                boolean found = false;
                for (Edge edge : listEdges) {
                  if (edge.statid.equals(stat.id) && edge.edgetype == StatEdge.TYPE_CONTINUE) {
                    found = true;
                    break;
                  }
                }

                if (!found) {
                  listEdges.add(new Edge(nd.id, stat.id, StatEdge.TYPE_CONTINUE));
                }

                sourcenode = nodecond;
                break;
              }
              case FOR_EACH: {
                // for (init : inc)
                //
                // is essentially
                //
                // for (inc; ; init)
                // TODO: that ordering does not make sense

                DirectNode inc = this.createDirectNode(stat, DirectNodeType.INCREMENT);
                inc.exprents = dostat.getIncExprentList();

                // Init is foreach variable definition
                DirectNode init = this.createDirectNode(stat, DirectNodeType.FOREACH_VARDEF);
                init.exprents = dostat.getInitExprentList();

                mapDestinationNodes.put(stat.id, new String[]{inc.id, init.id});
                mapDestinationNodes.put(-stat.id, new String[]{init.id, null});

                listEdges.add(new Edge(init.id, stat.getFirst().id, StatEdge.TYPE_REGULAR));
                listEdges.add(new Edge(inc.id, -stat.id, StatEdge.TYPE_REGULAR));

                boolean found = false;
                for (Edge edge : listEdges) {
                  if (edge.statid.equals(stat.id) && edge.edgetype == StatEdge.TYPE_CONTINUE) {
                    found = true;
                    break;
                  }
                }

                if (!found) {
                  listEdges.add(new Edge(nd.id, stat.id, StatEdge.TYPE_CONTINUE));
                }

                sourcenode = init;
                break;
              }
            }
            break;
          case SYNCHRONIZED:
          case SWITCH:
          case IF:
          case SEQUENCE:
          case ROOT:
            int statsize = stat.getStats().size();
            if (stat instanceof SynchronizedStatement) {
              statsize = 2;  // exclude the handler if synchronized
            }

            if (statementBreakIndex <= statsize) {
              List<Exprent> tailexprlst = null;

              switch (stat.type) {
                case SYNCHRONIZED:
                  tailexprlst = ((SynchronizedStatement) stat).getHeadexprentList();
                  break;
                case SWITCH:
                  tailexprlst = ((SwitchStatement) stat).getHeadexprentList();
                  break;
                case IF:
                  tailexprlst = ((IfStatement) stat).getHeadexprentList();
              }

              for (int i = statementBreakIndex; i < statsize; i++) {
                statEntry.statementIndex = i + 1;

                lstStackStatements.addFirst(statEntry);
                lstStackStatements.addFirst(
                  new StatementStackEntry(stat.getStats().get(i), stackFinally,
                    (i == 0 && tailexprlst != null && tailexprlst.get(0) != null) ? tailexprlst : null));

                continue mainloop;
              }

              node = graph.nodes.getWithKey(mapDestinationNodes.get(stat.getFirst().id)[0]);
              mapDestinationNodes.put(stat.id, new String[]{node.id, null});

              // Try to intercept the edges leaving the switch head and replace with relevant case nodes
              if (stat instanceof SwitchStatement) {
                SwitchStatement switchSt = (SwitchStatement) stat;

                Statement first = stat.getFirst();

                List<Edge> headEdges = new ArrayList<>();
                // Find edges out of the switch head (tail)
                for (Edge edge : this.listEdges) {
                  if (edge.sourceid.equals(first.id + "_tail")) {

                    if (switchSt.getStats().containsKey(edge.statid)) {
                      headEdges.add(edge);
                    }
                  }
                }

                if (!headEdges.isEmpty()) {
                  // else, already processed

                  for (Edge edge : headEdges) {
                    Statement caseSt = switchSt.findCaseBranchContaining(edge.statid);
                    int index = switchSt.getCaseStatements().indexOf(caseSt);

                    // Possible in the case of default statements leaving switch
                    if (index == -1) {
                      continue;
                    }

                    List<Exprent> values = switchSt.getCaseValues().get(index);

                    // Default case val can be null
                    List<Exprent> finalVals = null;
                    if (values != null) {
                      finalVals = new ArrayList<>();
                      for (Exprent value : values) {
                        if (value != null) {
                          finalVals.add(value);
                        }
                      }
                    }

                    // Build node out of the case exprents
                    DirectNode casend = DirectNode.forStat(DirectNodeType.CASE, caseSt);
                    casend.exprents = finalVals;
                    graph.nodes.addWithKey(casend, casend.id);

                    this.mapDestinationNodes.put(caseSt.id - SWITCH_CONST, new String[]{casend.id, null});

                    // Remove old edge
                    listEdges.remove(edge);

                    // head->case
                    listEdges.add(new Edge(edge.sourceid, caseSt.id - SWITCH_CONST, StatEdge.TYPE_REGULAR));
                    // case->dest
                    listEdges.add(new Edge(casend.id, edge.statid, StatEdge.TYPE_REGULAR));
                  }
                }
              }

              if (stat instanceof IfStatement && ((IfStatement)stat).iftype == IfStatement.IFTYPE_IF && !stat.getAllSuccessorEdges().isEmpty()) {
                lstSuccEdges.add(stat.getSuccessorEdges(Statement.STATEDGE_DIRECT_ALL).get(0));  // exactly one edge
                sourcenode = tailexprlst.get(0) == null ? node : graph.nodes.getWithKey(node.id + "_tail");
              }

              // Adds an edge from the last if statement to the current if statement, if the current if statement's head statement has no predecessor
              // This was made to mask a failure in EliminateLoopsHelper and isn't used currently (over the current test set) but could theoretically still happen!
              if (stat instanceof IfStatement && ((IfStatement) stat).iftype == IfStatement.IFTYPE_IF && !stat.getPredecessorEdges(StatEdge.TYPE_REGULAR).isEmpty()) {
                if (stat.getFirst().getPredecessorEdges(StatEdge.TYPE_REGULAR).isEmpty()) {
                  StatEdge edge = stat.getPredecessorEdges(StatEdge.TYPE_REGULAR).get(0);

                  Statement source = edge.getSource();
                  if (source instanceof IfStatement && ((IfStatement) source).iftype == IfStatement.IFTYPE_IF && !source.getAllSuccessorEdges().isEmpty()) {
                    DirectNode srcnd = graph.nodes.getWithKey(source.getFirst().id + "_tail");

                    if (srcnd != null) {
                      // old ifstat->head
                      Edge newEdge = new Edge(srcnd.id, stat.id, edge.getType());

                      // Add if it doesn't exist already
                      if (!listEdges.contains(newEdge)) {
                        listEdges.add(newEdge);
                      }
                    }
                  }
                }
              }
            }
        }
      }

      // no successor edges
      if (sourcenode != null) {

        if (statEntry.succEdges != null) {
          lstSuccEdges = statEntry.succEdges;
        }

        for (int edgeindex = statEntry.edgeIndex; edgeindex < lstSuccEdges.size(); edgeindex++) {

          StatEdge edge = lstSuccEdges.get(edgeindex);

          LinkedList<StackEntry> stack = new LinkedList<>(stackFinally);

          int edgetype = edge.getType();
          Statement destination = edge.getDestination();

          DirectNode finallyShortRangeSource = sourcenode;
          DirectNode finallyLongRangeSource = sourcenode;
          Statement finallyShortRangeEntry = null;
          Statement finallyLongRangeEntry = null;

          boolean isFinallyMonitorExceptionPath = false;

          boolean isFinallyExit = false;

          while (true) {

            StackEntry entry = null;
            if (!stack.isEmpty()) {
              entry = stack.getLast();
            }

            boolean created = true;

            if (entry == null) {
              saveEdge(sourcenode, destination, edgetype, isFinallyExit ? finallyShortRangeSource : null, finallyLongRangeSource,
                finallyShortRangeEntry, finallyLongRangeEntry, isFinallyMonitorExceptionPath);
            } else {
              CatchAllStatement catchall = entry.catchstatement;

              if (entry.state) { // finally handler statement
                if (edgetype == StatEdge.TYPE_FINALLYEXIT) {

                  stack.removeLast();
                  destination = entry.destination;
                  edgetype = entry.edgetype;

                  finallyShortRangeSource = entry.finallyShortRangeSource;
                  finallyLongRangeSource = entry.finallyLongRangeSource;
                  finallyShortRangeEntry = entry.finallyShortRangeEntry;
                  finallyLongRangeEntry = entry.finallyLongRangeEntry;

                  isFinallyExit = true;
                  isFinallyMonitorExceptionPath = (catchall.getMonitor() != null) & entry.isFinallyExceptionPath;

                  created = false;
                } else {
                  if (!catchall.containsStatementStrict(destination)) {
                    stack.removeLast();
                    created = false;
                  } else {
                    saveEdge(sourcenode, destination, edgetype, isFinallyExit ? finallyShortRangeSource : null, finallyLongRangeSource,
                      finallyShortRangeEntry, finallyLongRangeEntry, isFinallyMonitorExceptionPath);
                  }
                }
              } else { // finally protected try statement
                if (!catchall.containsStatementStrict(destination)) {

                  // FIXME: this is a hack, the edges need to be more properly defined from the finally handler to it's destination
                  //  Otherwise problems can occur where variable usage scopes aren't correct!
                  // Edge from finally handler head to destination
                  listEdges.add(new Edge(sourcenode.id, destination.id, edgetype));

                  saveEdge(sourcenode, catchall.getHandler(), StatEdge.TYPE_REGULAR, isFinallyExit ? finallyShortRangeSource : null,
                    finallyLongRangeSource, finallyShortRangeEntry, finallyLongRangeEntry, isFinallyMonitorExceptionPath);

                  stack.removeLast();
                  stack.add(new StackEntry(catchall, true, edgetype, destination, catchall.getHandler(),
                    finallyLongRangeEntry == null ? catchall.getHandler() : finallyLongRangeEntry,
                    sourcenode, finallyLongRangeSource, false));

                  statEntry.edgeIndex = edgeindex + 1;
                  statEntry.succEdges = lstSuccEdges;
                  lstStackStatements.addFirst(statEntry);
                  lstStackStatements.addFirst(new StatementStackEntry(catchall.getHandler(), stack, null));

                  continue mainloop;
                } else {
                  saveEdge(sourcenode, destination, edgetype, isFinallyExit ? finallyShortRangeSource : null, finallyLongRangeSource,
                    finallyShortRangeEntry, finallyLongRangeEntry, isFinallyMonitorExceptionPath);
                }
              }
            }

            if (created) {
              break;
            }
          }
        }
      }
    }
  }

  private void addEdgeIfPossible(Integer predEdge, Statement stat) {
    String[] lastbasicdests = mapDestinationNodes.get(predEdge);

    if (lastbasicdests != null) {
      listEdges.add(new Edge(graph.nodes.getWithKey(lastbasicdests[0]).id, stat.id, StatEdge.TYPE_REGULAR));
    }
  }

  private boolean hasAnyEdgeTo(List<Edge> listEdges, Statement stat) {
    for (Edge edge : listEdges) {
      if (edge.statid == stat.id) {
        return true;
      }
    }

    return false;
  }

  private void saveEdge(DirectNode sourcenode,
                        Statement destination,
                        int edgetype,
                        DirectNode finallyShortRangeSource,
                        DirectNode finallyLongRangeSource,
                        Statement finallyShortRangeEntry,
                        Statement finallyLongRangeEntry,
                        boolean isFinallyMonitorExceptionPath) {

    if (edgetype != StatEdge.TYPE_FINALLYEXIT) {
      listEdges.add(new Edge(sourcenode.id, destination.id, edgetype));
    }

    if (finallyShortRangeSource != null) {
      boolean isContinueEdge = (edgetype == StatEdge.TYPE_CONTINUE);

      mapShortRangeFinallyPathIds.computeIfAbsent(sourcenode.id, k -> new ArrayList<>()).add(new String[]{
        finallyShortRangeSource.id,
        String.valueOf(destination.id),
        String.valueOf(finallyShortRangeEntry.id),
        isFinallyMonitorExceptionPath ? "1" : null,
        isContinueEdge ? "1" : null});

      mapLongRangeFinallyPathIds.computeIfAbsent(sourcenode.id, k -> new ArrayList<>()).add(new String[]{
        finallyLongRangeSource.id,
        String.valueOf(destination.id),
        String.valueOf(finallyLongRangeEntry.id),
        isContinueEdge ? "1" : null});
    }
  }

  private void setEdges() {

    for (Edge edge : listEdges) {

      String sourceid = edge.sourceid;
      Integer statid = edge.statid;

      DirectNode source = graph.nodes.getWithKey(sourceid);

      String[] strings = mapDestinationNodes.get(statid);
      if (strings == null) {
        DotExporter.toDotFile(graph, root.mt, "errorDGraph");

        throw new IllegalStateException("Could not find destination nodes for stat id " + statid + " from source " + sourceid);
      }
      // TODO: continue edge type?
      DirectNode dest = graph.nodes.getWithKey(strings[edge.edgetype == StatEdge.TYPE_CONTINUE ? 1 : 0]);

      DirectEdge diedge = edge.edgetype == StatEdge.TYPE_EXCEPTION
        ? DirectEdge.exception(source, dest)
        : DirectEdge.of(source, dest);

      source.addSuccessor(diedge);

      if (mapPosIfBranch.containsKey(sourceid) && !statid.equals(mapPosIfBranch.get(sourceid))) {
        graph.mapNegIfBranch.put(sourceid, dest.id);
      }
    }

    for (int i = 0; i < 2; i++) {
      for (Entry<String, List<String[]>> ent : (i == 0 ? mapShortRangeFinallyPathIds : mapLongRangeFinallyPathIds).entrySet()) {

        List<FinallyPathWrapper> newLst = new ArrayList<>();

        List<String[]> lst = ent.getValue();
        for (String[] arr : lst) {

          boolean isContinueEdge = arr[i == 0 ? 4 : 3] != null;

          DirectNode dest = graph.nodes.getWithKey(mapDestinationNodes.get(Integer.parseInt(arr[1]))[isContinueEdge ? 1 : 0]);
          DirectNode enter = graph.nodes.getWithKey(mapDestinationNodes.get(Integer.parseInt(arr[2]))[0]);

          newLst.add(new FinallyPathWrapper(arr[0], dest.id, enter.id));

          if (i == 0 && arr[3] != null) {
            graph.mapFinallyMonitorExceptionPathExits.put(ent.getKey(), dest.id);
          }
        }

        if (!newLst.isEmpty()) {
          (i == 0 ? graph.mapShortRangeFinallyPaths : graph.mapLongRangeFinallyPaths).put(ent.getKey(),
            new ArrayList<>(
              new HashSet<>(newLst)));
        }
      }
    }
  }

  public Map<Integer, String[]> getMapDestinationNodes() {
    return mapDestinationNodes;
  }

  public static final class FinallyPathWrapper {
    public final String source;
    public final String destination;
    public final String entry;

    private FinallyPathWrapper(String source, String destination, String entry) {
      this.source = source;
      this.destination = destination;
      this.entry = entry;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) return true;
      if (!(o instanceof FinallyPathWrapper)) return false;

      FinallyPathWrapper fpw = (FinallyPathWrapper) o;
      return (source + ":" + destination + ":" + entry).equals(fpw.source + ":" + fpw.destination + ":" + fpw.entry);
    }

    @Override
    public int hashCode() {
      return (source + ":" + destination + ":" + entry).hashCode();
    }

    @Override
    public String toString() {
      return source + "->(" + entry + ")->" + destination;
    }
  }


  private static class StackEntry {

    public final CatchAllStatement catchstatement;
    public final boolean state;
    public final int edgetype;
    public final boolean isFinallyExceptionPath;

    public final Statement destination;
    public final Statement finallyShortRangeEntry;
    public final Statement finallyLongRangeEntry;
    public final DirectNode finallyShortRangeSource;
    public final DirectNode finallyLongRangeSource;

    StackEntry(CatchAllStatement catchstatement,
               boolean state,
               int edgetype,
               Statement destination,
               Statement finallyShortRangeEntry,
               Statement finallyLongRangeEntry,
               DirectNode finallyShortRangeSource,
               DirectNode finallyLongRangeSource,
               boolean isFinallyExceptionPath) {

      this.catchstatement = catchstatement;
      this.state = state;
      this.edgetype = edgetype;
      this.isFinallyExceptionPath = isFinallyExceptionPath;

      this.destination = destination;
      this.finallyShortRangeEntry = finallyShortRangeEntry;
      this.finallyLongRangeEntry = finallyLongRangeEntry;
      this.finallyShortRangeSource = finallyShortRangeSource;
      this.finallyLongRangeSource = finallyLongRangeSource;
    }

    StackEntry(CatchAllStatement catchstatement, boolean state) {
      this(catchstatement, state, -1, null, null, null, null, null, false);
    }
  }

  private static class Edge {
    public final String sourceid;
    public final Integer statid;
    public final int edgetype;

    Edge(String sourceid, Integer statid, int edgetype) {
      this.sourceid = sourceid;
      this.statid = statid;
      this.edgetype = edgetype;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Edge edge = (Edge) o;
      return edgetype == edge.edgetype && Objects.equals(sourceid, edge.sourceid) && Objects.equals(statid, edge.statid);
    }

    @Override
    public String toString() {
      return "Source: " + sourceid + " Stat: " + statid + " Edge: " + edgetype;
    }
  }
}
