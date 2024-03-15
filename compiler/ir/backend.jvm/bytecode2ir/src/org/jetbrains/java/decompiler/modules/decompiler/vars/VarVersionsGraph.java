// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.vars;

import org.jetbrains.java.decompiler.modules.decompiler.ValidationHelper;
import org.jetbrains.java.decompiler.modules.decompiler.decompose.GenericDominatorEngine;
import org.jetbrains.java.decompiler.modules.decompiler.decompose.IGraph;
import org.jetbrains.java.decompiler.modules.decompiler.decompose.IGraphNode;
import org.jetbrains.java.decompiler.struct.attr.StructLocalVariableTableAttribute.LocalVariable;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.util.*;

public class VarVersionsGraph {
  public final VBStyleCollection<VarVersionNode, VarVersionPair> nodes = new VBStyleCollection<>();

  private GenericDominatorEngine engine;

  public VarVersionNode createNode(VarVersionPair ver) {
    return createNode(ver, null);
  }

  public VarVersionNode createNode(VarVersionPair ver, LocalVariable lvt) {
    VarVersionNode node;
    nodes.addWithKey(node = new VarVersionNode(ver.var, ver.version, lvt), ver);
    return node;
  }

  public void addNodes(Collection<VarVersionNode> colnodes, Collection<VarVersionPair> colpaars) {
    nodes.addAllWithKey(colnodes, colpaars);
  }

  public boolean isDominatorSet(VarVersionNode node, Set<VarVersionNode> domnodes) {
    if (domnodes.size() == 1) {
      return engine.isDominator(node, domnodes.iterator().next());
    } else {
      Set<VarVersionNode> marked = new HashSet<>();

      if (domnodes.contains(node)) {
        return true;
      }

      List<VarVersionNode> lstNodes = new LinkedList<>();
      lstNodes.add(node);

      while (!lstNodes.isEmpty()) {
        VarVersionNode nd = lstNodes.remove(0);

        if (marked.contains(nd)) {
          continue;
        } else {
          marked.add(nd);
        }

        if (nd.preds.isEmpty()) {
          return false;
        }

        for (VarVersionEdge edge : nd.preds) {
          VarVersionNode pred = edge.source;

          if (!marked.contains(pred) && !domnodes.contains(pred)) {
            lstNodes.add(pred);
          }
        }
      }
    }

    return true;
  }

  public void initDominators() {
    Set<VarVersionNode> roots = new HashSet<>();

    for (VarVersionNode node : nodes) {
      if (node.preds.isEmpty()) {
        roots.add(node);
      }
    }

    // TODO: optimization!! This is called multiple times for each method and the allocations will add up!
    Set<VarVersionNode> reached = rootReachability(roots);
    // If the nodes we reach don't include every node we have, then we need to process further to decompose the cycles
    if (this.nodes.size() != reached.size()) {
      // Not all nodes are reachable, due to cyclic nodes

      // Find only the nodes that aren't accounted for
      Set<VarVersionNode> intersection = new HashSet<>(this.nodes);
      intersection.removeAll(reached);

      // Var -> [versions]
      Map<Integer, List<Integer>> varMap = new HashMap<>();

      Set<VarVersionNode> visited = new HashSet<>();
      for (VarVersionNode node : intersection) {
        if (visited.contains(node)) {
          continue;
        }

        // DFS to find all nodes reachable from this node
        Set<VarVersionNode> found = findNodes(node);
        // Skip all the found nodes from this node in the future
        visited.addAll(found);

        // For every node that we found, keep track of the var index and the versions of each node
        // Each disjoint set *should* only reference a single var, so we operate under that assumption and keep track of the versions based on the var index
        // If this isn't true, then this algorithm won't find every cyclic root as it will account multiple disjoint sets as one!
        for (VarVersionNode foundNode : found) {
          varMap.computeIfAbsent(foundNode.var, k -> new ArrayList<>()).add(foundNode.version);
        }
      }

      for (Integer var : varMap.keySet()) {
        // Sort versions
        varMap.get(var).sort(Comparator.naturalOrder());

        // First version is the lowest version, so that can be considered as the root
        VarVersionPair pair = new VarVersionPair(var, varMap.get(var).get(0));

        // Add to existing roots
        roots.add(this.nodes.getWithKey(pair));
      }

      // TODO: needs another validation pass?
    }

    engine = new GenericDominatorEngine(new IGraph() {
      @Override
      public List<? extends IGraphNode> getReversePostOrderList() {
        return getReversedPostOrder(roots);
      }

      @Override
      public Set<? extends IGraphNode> getRoots() {
        return new HashSet<IGraphNode>(roots);
      }
    });

    engine.initialize();
  }

  private Set<VarVersionNode> findNodes(VarVersionNode start) {
    Set<VarVersionNode> visited = new HashSet<>();
    Deque<VarVersionNode> stack = new LinkedList<>();
    stack.add(start);

    while (!stack.isEmpty()) {
      VarVersionNode node = stack.removeLast();

      if (visited.add(node)) {
        for (VarVersionEdge edge : node.succs) {
          stack.addLast(edge.dest);
        }
      }
    }

    return visited;
  }

  public Set<VarVersionNode> rootReachability(Set<VarVersionNode> roots) {
    Set<VarVersionNode> visited = new HashSet<>();

    Deque<VarVersionNode> stack = new LinkedList<>(roots);

    while (!stack.isEmpty()) {
      VarVersionNode node = stack.removeLast();

      if (visited.add(node)) {
        for (VarVersionEdge edge : node.succs) {
          stack.addLast(edge.dest);
        }
      }
    }

    return visited;
  }

  public boolean areVarsAnalogous(int varBase, int varCheck) {
    Deque<VarVersionNode> stack = new LinkedList<>();
    Set<VarVersionNode> visited = new HashSet<>();

    VarVersionNode start = this.nodes.getWithKey(new VarVersionPair(varBase, 1));
    stack.add(start);

    while (!stack.isEmpty()) {
      VarVersionNode node = stack.removeFirst();

      if (visited.contains(node)) {
        continue;
      }

      visited.add(node);
      VarVersionNode analog = this.nodes.getWithKey(new VarVersionPair(varCheck, node.version));

      if (analog == null) {
        return false;
      }

      if (node.succs.size() != analog.succs.size()) {
        return false;
      }

      // FIXME: better checking
      for (VarVersionEdge suc : node.succs) {
        stack.add(suc.dest);

        VarVersionNode sucAnalog = this.nodes.getWithKey(new VarVersionPair(varCheck, suc.dest.version));

        if (sucAnalog == null) {
          return false;
        }
      }
    }

    return true;
  }

  private static List<VarVersionNode> getReversedPostOrder(Collection<VarVersionNode> roots) {
    List<VarVersionNode> lst = new LinkedList<>();
    Set<VarVersionNode> setVisited = new HashSet<>();

    for (VarVersionNode root : roots) {
      List<VarVersionNode> lstTemp = new LinkedList<>();
      addToReversePostOrderListIterative(root, lstTemp, setVisited);
      lst.addAll(lstTemp);
    }

    return lst;
  }

  private static void addToReversePostOrderListIterative(VarVersionNode root, List<? super VarVersionNode> lst, Set<? super VarVersionNode> setVisited) {
    Map<VarVersionNode, List<VarVersionEdge>> mapNodeSuccs = new HashMap<>();
    LinkedList<VarVersionNode> stackNode = new LinkedList<>();
    LinkedList<Integer> stackIndex = new LinkedList<>();

    stackNode.add(root);
    stackIndex.add(0);

    while (!stackNode.isEmpty()) {
      VarVersionNode node = stackNode.getLast();
      int index = stackIndex.removeLast();

      setVisited.add(node);

      List<VarVersionEdge> lstSuccs = mapNodeSuccs.computeIfAbsent(node, n -> new ArrayList<>(n.succs));
      for (; index < lstSuccs.size(); index++) {
        VarVersionNode succ = lstSuccs.get(index).dest;

        if (!setVisited.contains(succ)) {
          stackIndex.add(index + 1);
          stackNode.add(succ);
          stackIndex.add(0);
          break;
        }
      }

      if (index == lstSuccs.size()) {
        lst.add(0, node);
        stackNode.removeLast();
      }
    }
  }
}