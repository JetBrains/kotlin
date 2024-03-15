package org.jetbrains.java.decompiler.modules.decompiler.decompose;

import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement.EdgeDirection;
import org.jetbrains.java.decompiler.util.ListStack;

import java.util.*;

// Original comment before it was removed here: https://github.com/JetBrains/intellij-community/commit/44a59462e45ac69bb7c9daa75c3db33446afedf0
//  --------------------------------------------------------------------
//    Algorithm
//  --------------------------------------------------------------------
//  DFS(G)
//  {
//  make a new vertex x with edges x->v for all v
//  initialize a counter N to zero
//  initialize list L to empty
//  build directed tree T, initially a single vertex {x}
//  visit(x)
//  }
//
//  visit(p)
//  {
//  add p to L
//  dfsnum(p) = N
//  increment N
//  low(p) = dfsnum(p)
//  for each edge p->q
//      if q is not already in T
//      {
//      add p->q to T
//      visit(q)
//      low(p) = min(low(p), low(q))
//      } else low(p) = min(low(p), dfsnum(q))
//
//  if low(p)=dfsnum(p)
//  {
//      output "component:"
//      repeat
//      remove last element v from L
//      output v
//      remove v from G
//      until v=p
//  }
//  }
//  --------------------------------------------------------------------

// Original algorithm: https://www.ics.uci.edu/~eppstein/161/960220.html
// Improved based on the description here: https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
// Improvements include using the onStack map to skip visits that aren't needed
public final class StrongConnectivityHelper {
  private final List<List<Statement>> components = new ArrayList<>(); // List of strongly connected components, each entry is a list of statements that compose the component
  private final Set<Statement> processed = new HashSet<>(); // Already processed statements, persistent
  private final ListStack<Statement> stack = new ListStack<>(); // Stack of statements currently being tracked
  private final Set<Statement> visitedStatements = new HashSet<>(); // Already seen statements
  private final Map<Statement, Integer> indexMap = new HashMap<>(); // Statement -> index
  private final Map<Statement, Integer> lowLinkMap = new HashMap<>(); // Statement -> lowest index of any statement reachable from this one
  private final Map<Statement, Boolean> onStack = new HashMap<>(); // Statement -> whether this statement is on the stack or not
  private int index; // Index of each statement

  public StrongConnectivityHelper(Statement stat) {
    visitTree(stat.getFirst());

    for (Statement st : stat.getStats()) {
      if (!this.processed.contains(st) && st.getPredecessorEdges(Statement.STATEDGE_DIRECT_ALL).isEmpty()) {
        visitTree(st);
      }
    }

    // should not find any more nodes! FIXME: ??
    for (Statement st : stat.getStats()) {
      if (!this.processed.contains(st)) {
        visitTree(st);
      }
    }
  }

  private void visitTree(Statement stat) {
    this.stack.clear();
    this.index = 0;
    this.visitedStatements.clear();
    this.indexMap.clear();
    this.lowLinkMap.clear();
    this.onStack.clear();

    // Visit statement, calculate strong connectivity
    visit(stat);

    // Add all visited statements to the processed set
    this.processed.addAll(this.visitedStatements);
    this.processed.add(stat);
  }

  private void visit(Statement stat) {
    this.stack.push(stat);
    this.indexMap.put(stat, this.index);
    this.lowLinkMap.put(stat, this.index);
    this.index++;
    this.onStack.put(stat, true);

    // Get all neighbor successors
    List<Statement> successors = stat.getNeighbours(StatEdge.TYPE_REGULAR, EdgeDirection.FORWARD); // TODO: set?
    // Remove the ones we've already processed
    successors.removeAll(this.processed);

    for (Statement succ : successors) {
      int newValue;

      if (this.visitedStatements.contains(succ)) { // Defined index, already visited
        if (!this.onStack.get(succ)) { // If this statement isn't on the stack, skip processing
          continue;
        }

        // New value is the index of the current statement, since we haven't seen this yet
        newValue = this.indexMap.get(succ);
      } else { // Undefined index, haven't visited yet
        //
        this.visitedStatements.add(succ);
        visit(succ); // Recurse

        // Get the low link value and set as the new value
        newValue = this.lowLinkMap.get(succ);
      }

      // Update low link values with the new value
      this.lowLinkMap.put(stat, Math.min(this.lowLinkMap.get(stat), newValue));
    }

    // If the lowlink of the current statement and the index is the same, it means that we're at the root
    if (this.lowLinkMap.get(stat).intValue() == this.indexMap.get(stat).intValue()) {
      // Start new strongly connected component
      List<Statement> component = new ArrayList<>();

      Statement v;

      do {
        // Pop off statement from stack
        v = this.stack.pop();
        // No longer on stack
        this.onStack.put(v, false);

        // Add to component
        component.add(v);
      }
      while (v != stat); // Repeat for as long as the tested component isn't the root

      // Add component to list
      this.components.add(component);
    }
  }

  // Returns true if the component has no outgoing edges that aren't accounted for by the component itself
  public static boolean isExitComponent(List<? extends Statement> lst) {
    Set<Statement> set = new HashSet<>();

    for (Statement stat : lst) {
      set.addAll(stat.getNeighbours(StatEdge.TYPE_REGULAR, EdgeDirection.FORWARD));
    }

    for (Statement stat : lst) {
      set.remove(stat);
    }

    return set.isEmpty();
  }

  public static List<Statement> getExitReps(List<? extends List<Statement>> lst) {
    List<Statement> res = new ArrayList<>();

    for (List<Statement> comp : lst) {
      if (isExitComponent(comp)) {
        res.add(comp.get(0));
      }
    }

    return res;
  }

  public List<List<Statement>> getComponents() {
    return this.components;
  }
}