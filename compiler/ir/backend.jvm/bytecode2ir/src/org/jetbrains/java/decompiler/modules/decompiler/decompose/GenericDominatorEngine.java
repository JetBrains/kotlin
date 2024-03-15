// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.decompose;

import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.util.List;
import java.util.Set;

public class GenericDominatorEngine {
  private boolean initialized = false;
  private final IGraph graph;

  private final VBStyleCollection<IGraphNode, IGraphNode> colOrderedIDoms = new VBStyleCollection<>();

  private Set<? extends IGraphNode> setRoots;

  public GenericDominatorEngine(IGraph graph) {
    this.graph = graph;
  }

  public void initialize() {
    calcIDoms();
    this.initialized = true;
  }

  private void orderNodes() {

    setRoots = graph.getRoots();

    for (IGraphNode node : graph.getReversePostOrderList()) {
      colOrderedIDoms.addWithKey(null, node);
    }
  }

  private static IGraphNode getCommonIDom(IGraphNode node1, IGraphNode node2, VBStyleCollection<IGraphNode, IGraphNode> orderedIDoms) {

    IGraphNode nodeOld;

    if (node1 == null) {
      return node2;
    } else if (node2 == null) {
      return node1;
    }

    int index1 = orderedIDoms.getIndexByKey(node1);
    int index2 = orderedIDoms.getIndexByKey(node2);

    while (index1 != index2) {
      if (index1 > index2) {
        nodeOld = node1;
        node1 = orderedIDoms.getWithKey(node1);

        if (nodeOld == node1) { // no idom - root or merging point
          return null;
        }

        index1 = orderedIDoms.getIndexByKey(node1);
      } else {
        nodeOld = node2;
        node2 = orderedIDoms.getWithKey(node2);

        if (nodeOld == node2) { // no idom - root or merging point
          return null;
        }

        index2 = orderedIDoms.getIndexByKey(node2);
      }
    }

    return node1;
  }

  private void calcIDoms() {

    orderNodes();

    List<IGraphNode> lstNodes = colOrderedIDoms.getLstKeys();

    while (true) {

      boolean changed = false;

      for (IGraphNode node : lstNodes) {

        IGraphNode idom = null;

        if (!setRoots.contains(node)) {
          for (IGraphNode pred : node.getPredecessors()) {
            if (colOrderedIDoms.getWithKey(pred) != null) {
              idom = getCommonIDom(idom, pred, colOrderedIDoms);
              if (idom == null) {
                break; // no idom found: merging point of two trees
              }
            }
          }
        }

        if (idom == null) {
          idom = node;
        }

        IGraphNode oldidom = colOrderedIDoms.putWithKey(idom, node);
        if (!idom.equals(oldidom)) { // oldidom is null iff the node is touched for the first time
          changed = true;
        }
      }

      if (!changed) {
        break;
      }
    }
  }

  public boolean isDominator(IGraphNode node, IGraphNode dom) {
    if (!this.initialized) {
      throw new IllegalStateException("GenericDominatorEngine not initialized!");
    }

    while (!node.equals(dom)) {

      IGraphNode idom = colOrderedIDoms.getWithKey(node);

      if (idom == node) {
        return false; // root node or merging point
      } else if (idom == null) {
        throw new RuntimeException("Inconsistent idom sequence discovered or node not found in dom graph!");
      } else {
        node = idom;
      }
    }

    return true;
  }
}
