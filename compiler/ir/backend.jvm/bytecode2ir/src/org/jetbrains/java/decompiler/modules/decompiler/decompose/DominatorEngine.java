// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.decompose;

import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.util.*;

public class DominatorEngine {

  private final Statement statement;

  private final VBStyleCollection<Integer, Integer> colOrderedIDoms = new VBStyleCollection<>();


  public DominatorEngine(Statement statement) {
    this.statement = statement;
  }

  public void initialize() {
    calcIDoms();
  }

  private void orderStatements() {

    for (Statement stat : statement.getReversePostOrderList()) {
      colOrderedIDoms.addWithKey(null, stat.id);
    }
  }

  private static Integer getCommonIDom(Integer key1, Integer key2, VBStyleCollection<Integer, Integer> orderedIDoms) {

    if (key1 == null) {
      return key2;
    }
    else if (key2 == null) {
      return key1;
    }

    int index1 = orderedIDoms.getIndexByKey(key1);
    int index2 = orderedIDoms.getIndexByKey(key2);

    while (index1 != index2) {
      if (index1 > index2) {
        key1 = orderedIDoms.getWithKey(key1);
        index1 = orderedIDoms.getIndexByKey(key1);
      }
      else {
        key2 = orderedIDoms.getWithKey(key2);
        index2 = orderedIDoms.getIndexByKey(key2);
      }
    }

    return key1;
  }

  private void calcIDoms() {

    orderStatements();

    colOrderedIDoms.putWithKey(statement.getFirst().id, statement.getFirst().id);

    // exclude first statement
    List<Integer> lstIds = colOrderedIDoms.getLstKeys().subList(1, colOrderedIDoms.getLstKeys().size());

    while (true) {

      boolean changed = false;

      for (int id : lstIds) {

        Statement stat = statement.getStats().getWithKey(id);
        Integer idom = null;

        for (StatEdge edge : stat.getAllPredecessorEdges()) {
          if (colOrderedIDoms.getWithKey(edge.getSource().id) != null) {
            idom = getCommonIDom(idom, edge.getSource().id, colOrderedIDoms);
          }
        }

        Integer oldidom = colOrderedIDoms.putWithKey(idom, id);
        if (!idom.equals(oldidom)) {
          changed = true;
        }
      }

      if (!changed) {
        break;
      }
    }
  }

  public VBStyleCollection<Integer, Integer> getOrderedIDoms() {
    return colOrderedIDoms;
  }

  // Returns if 'node' is dominated by 'dom'
  // aka if 'dom' is a dominator of 'node'
  public boolean isDominator(Integer node, Integer dom) {
    while (!node.equals(dom)) {

      Integer idom = colOrderedIDoms.getWithKey(node);

      if (idom.equals(node)) {
        return false; // root node
      } else {
        node = idom;
      }
    }

    return true;
  }

  // Find all nodes dominated by the start node
  public Set<Integer> allDomsFor(Integer start) {
    Set<Integer> ret = new HashSet<>();

    Deque<Integer> stack = new LinkedList<>();
    stack.add(start);

    while (!stack.isEmpty()) {
      Integer id = stack.removeFirst();

      if (ret.contains(id)) {
        continue;
      }

      ret.add(id);

      // Find every node that equals the current in the set, then add those keys onto the stack
      // This will have the effect of traversing down the tree
      for (Integer key : this.colOrderedIDoms.getLstKeys()) {
        Integer ndid = this.colOrderedIDoms.getWithKey(key);

        if (ndid.equals(id)) {
          stack.add(key);
        }
      }
    }

    return ret;
  }
}
