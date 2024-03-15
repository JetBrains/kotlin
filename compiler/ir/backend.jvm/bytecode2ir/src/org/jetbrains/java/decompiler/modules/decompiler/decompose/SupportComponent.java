package org.jetbrains.java.decompiler.modules.decompiler.decompose;

import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.util.DotExporter;
import org.jetbrains.java.decompiler.util.FastFixedSetFactory.FastFixedSet;

import java.util.*;

// Model of a strongly connected component, the entrypoint (header), and the backedges to the entrypoint.
// This allows us to find endpoint cycles to better calculate postdominance
public final class SupportComponent {
  // Statements in this component
  public final List<Statement> stats;
  // Backedges to loop header
  public final Map<Integer, FastFixedSet<Integer>> selfSupportPoints;
  // Loop header
  public final Statement supportedPoint;

  public SupportComponent(List<Statement> stats, Map<Integer, FastFixedSet<Integer>> selfSupportPoints, Statement supportedPoint) {
    this.stats = stats;
    this.selfSupportPoints = selfSupportPoints;
    this.supportedPoint = supportedPoint;
  }


  public static SupportComponent identify(List<Statement> component, Map<Integer, FastFixedSet<Integer>> mapSupportPoints, DominatorEngine dom) {
    Map<Integer, FastFixedSet<Integer>> selfSupportPoints = new HashMap<>();
    Set<Statement> supportedAll = new HashSet<>();
    for (Statement st : component) {
      FastFixedSet<Integer> supReach = mapSupportPoints.get(st.id);

      if (supReach != null) {
        // TODO: continue -> general?
        for (StatEdge edge : st.getSuccessorEdgeView(StatEdge.TYPE_REGULAR)) {
          Statement dest = edge.getDestination();

          if (!component.contains(dest)) {
            // Support point supports statement outside of component, invalid
            return null;
          } else {
            // If the successor is a dominator of the current statement, then we know that it must be an earlier statement, so the successor is supported point
            if (dom.isDominator(st.id, dest.id)) {
              supportedAll.add(dest);
            }
          }
        }

        // no filter needed as this is coming from the component itself
        selfSupportPoints.put(st.id, supReach);
      }
    }

    // There should only be a single component that is supported: if there's more, then we know that there is a nested loop
    // TODO: The algorithm isn't able to decompose nested loops, so we simply quit processing for now
    if (supportedAll.size() != 1) {
      return null;
    }

    // Somehow got no support points- should be impossible!
    if (selfSupportPoints.isEmpty()) {
      return null;
    }

    // Find all edges leaving this component. There should only be one, and that is the edge leading into the header!
    List<Statement> outgoing = new ArrayList<>();
    for (Statement st : component) {
      // TODO: pred edge view
      for (StatEdge edge : st.getPredecessorEdges(StatEdge.TYPE_REGULAR)) {
        if (!component.contains(edge.getSource())) {
          outgoing.add(st);
        }
      }
    }

    if (outgoing.size() != 1) {
      return null;
    }

    // Ensure that the header is the dominator of every node in the component
    Statement head = outgoing.get(0);
    for (Statement st : component) {
      if (!dom.isDominator(st.id, head.id)) {
        return null;
      }
    }

    return new SupportComponent(component, selfSupportPoints, head);
  }

  @Override
  public String toString() {
    return "SupportComponent[" + stats + ", selfSupportPoints=" + selfSupportPoints + ", header=" + supportedPoint + ']';
  }
}
