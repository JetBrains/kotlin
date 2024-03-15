// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement.EdgeDirection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public final class InlineSingleBlockHelper {


  public static boolean inlineSingleBlocks(RootStatement root) {

    boolean res = inlineSingleBlocksRec(root);

    if (res) {
      SequenceHelper.condenseSequences(root);
    }

    return res;
  }

  private static boolean inlineSingleBlocksRec(Statement stat) {

    boolean res = false;

    for (Statement st : stat.getStats()) {
      res |= inlineSingleBlocksRec(st);
    }

    if (stat instanceof SequenceStatement) {

      SequenceStatement seq = (SequenceStatement)stat;
      for (int i = 1; i < seq.getStats().size(); i++) {
        if (isInlineable(seq, i)) {
          inlineBlock(seq, i);
          return true;
        }
      }
    }

    return res;
  }

  private static void inlineBlock(SequenceStatement seq, int index) {

    Statement first = seq.getStats().get(index);
    Statement pre = seq.getStats().get(index - 1);
    pre.removeSuccessor(pre.getFirstSuccessor());   // single regular edge

    StatEdge edge = first.getPredecessorEdges(StatEdge.TYPE_BREAK).get(0);
    Statement source = edge.getSource();
    Statement parent = source.getParent();
    source.removeSuccessor(edge);

    List<Statement> lst = new ArrayList<>();
    for (int i = seq.getStats().size() - 1; i >= index; i--) {
      lst.add(0, seq.getStats().remove(i));
    }

    if (parent instanceof IfStatement && ((IfStatement)parent).iftype == IfStatement.IFTYPE_IF &&
        source == parent.getFirst()) {
      IfStatement ifparent = (IfStatement)parent;

      SequenceStatement block = new SequenceStatement(lst);
      block.setAllParent();

      StatEdge newedge = new StatEdge(StatEdge.TYPE_REGULAR, source, block);
      source.addSuccessor(newedge);
      ifparent.setIfEdge(newedge);
      ifparent.setIfstat(block);

      ifparent.getStats().addWithKey(block, block.id);
      block.setParent(ifparent);
    }
    else {
      lst.add(0, source);

      SequenceStatement block = new SequenceStatement(lst);
      block.setAllParent();

      parent.replaceStatement(source, block);

      // LabelHelper.lowContinueLabels not applicable because of forward continue edges
      // LabelHelper.lowContinueLabels(block, new HashSet<StatEdge>());
      // do it by hand
      for (StatEdge prededge : block.getPredecessorEdges(StatEdge.TYPE_CONTINUE)) {

        block.removePredecessor(prededge);
        prededge.getSource().changeEdgeNode(EdgeDirection.FORWARD, prededge, source);
        source.addPredecessor(prededge);

        source.addLabeledEdge(prededge);
      }


      if (parent instanceof SwitchStatement) {
        ((SwitchStatement)parent).sortEdgesAndNodes();
      }

      source.addSuccessor(new StatEdge(StatEdge.TYPE_REGULAR, source, first));

      Statement last = block.getStats().get(block.getStats().size() - 1);
      if (!last.hasAnySuccessor()){
        // make sure that the scope of all the break edges are updated correctly
        for (Iterator<StatEdge> iterator = last.getLabelEdges().iterator(); iterator.hasNext(); ) {
          StatEdge labelEdge = iterator.next();
          if (labelEdge.getType() == StatEdge.TYPE_BREAK &&
              MergeHelper.isDirectPath(last, labelEdge.getDestination())) {
            // find the correct closure
            Statement closure = parent;
            while(!MergeHelper.isDirectPath(closure, labelEdge.getDestination())) {
              closure = closure.getParent();
              if (closure == null) {
                // No valid closure found. Somehow we got inlined in a place where
                // none of our parents have a way to get to the destination, without
                // going through other code first.
                throw new IllegalStateException("Where have we been inlined to?");
              }
            }
            labelEdge.closure = closure;
            iterator.remove();
          }
        }
      }
    }
  }

  private static boolean isInlineable(SequenceStatement seq, int index) {

    Statement first = seq.getStats().get(index);
    Statement pre = seq.getStats().get(index - 1);

    if (pre.hasBasicSuccEdge()) {
      return false;
    }

    List<StatEdge> lst = first.getPredecessorEdges(StatEdge.TYPE_BREAK);

    if (lst.size() == 1) {
      StatEdge edge = lst.get(0);

      if (sameCatchRanges(edge)) {
        if (!edge.canInline) {
          return false; //Dirty hack, but lets do it!
        }

        if (!edge.explicit) {
          for (int i = index; i < seq.getStats().size(); i++) {
            if (!noExitLabels(seq.getStats().get(i), seq)) {
              return false;
            }
          }
        }

        if (edge.getSource().getParent() instanceof SwitchStatement) {
          SwitchStatement swst = (SwitchStatement) edge.getSource().getParent();

          // Can't inline into an empty switch statement!
          if (swst.getCaseStatements().isEmpty()) {
            return false;
          }
        }

        boolean noPreSuccessors = !pre.hasAnySuccessor();

        if (noPreSuccessors) {
          // No successors so we can't inline (as we don't know where to go!) [TestInlineNoSuccessor]
          return false;
        }

        // Has at least 1 successor so we can inline
        return true;
      }
      // FIXME: count labels properly
    }

    return false;
  }

  private static boolean sameCatchRanges(StatEdge edge) {

    Statement from = edge.getSource();
    Statement to = edge.getDestination();

    while (true) {
      Statement parent = from.getParent();
      if (parent.containsStatementStrict(to)) {
        break;
      }

      if (parent instanceof CatchStatement || parent instanceof CatchAllStatement) {
        if (parent.getFirst() == from) {
          return false;
        }
      } else if (parent instanceof SynchronizedStatement) {
        if (parent.getStats().get(1) == from) {
          return false;
        }
      }

      from = parent;
    }

    return true;
  }

  private static boolean noExitLabels(Statement block, Statement sequence) {
    for (StatEdge edge : block.getAllSuccessorEdges()) {
      if (edge.getType() != StatEdge.TYPE_REGULAR && !(edge.getDestination() instanceof DummyExitStatement)) {
        if (!sequence.containsStatementStrict(edge.getDestination())) {
          return false;
        }
      }
    }

    for (Statement st : block.getStats()) {
      if (!noExitLabels(st, sequence)) {
        return false;
      }
    }

    return true;
  }

  public static boolean isBreakEdgeLabeled(Statement source, Statement closure) {
    if (closure instanceof DoStatement || closure instanceof SwitchStatement) {
      Statement parent = source.getParent();
      return parent != closure &&
             (parent instanceof DoStatement || parent instanceof SwitchStatement || isBreakEdgeLabeled(parent, closure));
    }
    else {
      return true;
    }
  }
}