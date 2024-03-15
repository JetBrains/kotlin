// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.decompose;

import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement.EdgeDirection;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.util.*;
import java.util.Map.Entry;

public class DominatorTreeExceptionFilter {

  private final Statement statement;

  // idom, nodes
  private final Map<Integer, Set<Integer>> mapTreeBranches = new LinkedHashMap<>();

  // handler, range nodes
  private final Map<Integer, Set<Integer>> mapExceptionRanges = new LinkedHashMap<>();

  // handler, head dom
  private Map<Integer, Integer> mapExceptionDoms = new HashMap<>();

  // statement, handler, exit nodes
  private final Map<Integer, Map<Integer, Integer>> mapExceptionRangeUniqueExit = new HashMap<>();

  private DominatorEngine domEngine;

  public DominatorTreeExceptionFilter(Statement statement) {
    this.statement = statement;
  }

  public void initialize() {
    domEngine = new DominatorEngine(statement);
    domEngine.initialize();

    buildDominatorTree();

    buildExceptionRanges();

    buildFilter(statement.getFirst().id);

    // free resources
    mapTreeBranches.clear();
    mapExceptionRanges.clear();
  }

  public boolean acceptStatementPair(Integer head, Integer exit) {
    Map<Integer, Integer> filter = mapExceptionRangeUniqueExit.get(head);
    for (Entry<Integer, Integer> entry : filter.entrySet()) {
      if (!head.equals(mapExceptionDoms.get(entry.getKey()))) {
        Integer filterExit = entry.getValue();
        if (filterExit == -1 || !filterExit.equals(exit)) {
          return false;
        }
      }
    }

    return true;
  }

  private void buildDominatorTree() {
    VBStyleCollection<Integer, Integer> orderedIDoms = domEngine.getOrderedIDoms();

    List<Integer> lstKeys = orderedIDoms.getLstKeys();
    for (int index = lstKeys.size() - 1; index >= 0; index--) {
      Integer key = lstKeys.get(index);
      Integer idom = orderedIDoms.get(index);
      mapTreeBranches.computeIfAbsent(idom, k -> new LinkedHashSet<>()).add(key);
    }

    Integer firstid = statement.getFirst().id;
    mapTreeBranches.get(firstid).remove(firstid);
  }

  private void buildExceptionRanges() {
    for (Statement stat : statement.getStats()) {
      List<Statement> lstPreds = stat.getNeighbours(StatEdge.TYPE_EXCEPTION, EdgeDirection.BACKWARD);
      if (!lstPreds.isEmpty()) {

        Set<Integer> set = new LinkedHashSet<>();

        for (Statement st : lstPreds) {
          set.add(st.id);
        }

        mapExceptionRanges.put(stat.id, set);
      }
    }

    mapExceptionDoms = buildExceptionDoms(statement.getFirst().id);
  }

  private Map<Integer, Integer> buildExceptionDoms(Integer id) {
    Map<Integer, Integer> map = new HashMap<>();

    Set<Integer> children = mapTreeBranches.get(id);
    if (children != null) {
      for (int childid : children) {
        Map<Integer, Integer> mapChild = buildExceptionDoms(childid);
        for (int handler : mapChild.keySet()) {
          map.put(handler, map.containsKey(handler) ? id : mapChild.get(handler));
        }
      }
    }

    for (Entry<Integer, Set<Integer>> entry : mapExceptionRanges.entrySet()) {
      if (entry.getValue().contains(id)) {
        map.put(entry.getKey(), id);
      }
    }

    return map;
  }

  private void buildFilter(Integer id) {
    Map<Integer, Integer> map = new HashMap<>();

    Set<Integer> children = mapTreeBranches.get(id);
    if (children != null) {
      for (Integer childid : children) {
        buildFilter(childid);

        Map<Integer, Integer> mapChild = mapExceptionRangeUniqueExit.get(childid);
        for (Entry<Integer, Set<Integer>> entry : mapExceptionRanges.entrySet()) {
          Integer handler = entry.getKey();
          Set<Integer> range = entry.getValue();

          if (range.contains(id)) {
            Integer exit;
            if (!range.contains(childid)) {
              exit = childid;
            }
            else if (map.containsKey(handler)) {
              exit = -1;
            }
            else {
              exit = mapChild.get(handler);
            }

            if (exit != null) {
              map.put(handler, exit);
            }
          }
        }
      }
    }

    mapExceptionRangeUniqueExit.put(id, map);
  }

  public DominatorEngine getDomEngine() {
    return domEngine;
  }
}