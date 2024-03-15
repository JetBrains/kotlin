package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;

import static org.jetbrains.java.decompiler.modules.decompiler.IfNode.EdgeType.*;

// Models an if statement, child if statements and their successors.
// Does not model grandchild if statements or successors of successors.
// Does **not** model if statements with else branches.
// Therefore the maximum amount of information that the root node can contain is:
// root [IfStatement] {
//   ifstat1 [IfStatement] {
//      ifstat2 [Any Statement]
//   }
//   elsestat2 [Any Statement]
// }
// elsestat1 [If Statement] {
//   ifstat3 [Any Statement]
// }
// elsestat3 [Any Statement]
// Note that an "elsestat" in this context is simply the stat we jump to when the condition is false, even if it's
// reachable from the if body (i.e. there should never need to be an "else" keyword in the source code).

// UPDATE:
// to handle ternaries better, the root node now allows for the elsestat1 to be inside an else. It will then have an
// EdgeType.ELSE edge type. This shouldn't interfere with any other handling as they all check this edge type.


class IfNode {
  // The stat that this node refers to. Root node will be an if stat, child nodes can be any stat.
  final Statement value;


  // if not null, then this is the stat/node that the control flow
  // would go to if the condition is true.
  IfNode innerNode;

  // DIRECT or INDIRECT.
  EdgeType innerType;

  // If successorType is ELSE (roots only), this is the else stat instead
  IfNode successorNode;

  // unless this node is the root, it will always be INDIRECT.
  EdgeType successorType;

  private IfNode(Statement value) {
    ValidationHelper.notNull(value);

    this.value = value;
  }

  private void setInner(IfNode ifNode, EdgeType indirect) {
    ValidationHelper.notNull(ifNode);
    ValidationHelper.notNull(indirect);

    this.innerNode = ifNode;
    this.innerType = indirect;
  }

  private void setSuccessor(IfNode ifNode, EdgeType indirect) {
    ValidationHelper.notNull(ifNode);
    ValidationHelper.notNull(indirect);

    this.successorNode = ifNode;
    this.successorType = indirect;
  }

  enum EdgeType {
    DIRECT, // direct edge (regular)
    INDIRECT, // indirect edge (continue, break, implicit)
    ELSE // special case for the elseStat of the root node
  }

  static IfNode build(IfStatement stat, boolean stsingle) {
    IfNode res = new IfNode(stat);

    // if branch
    if (stat.getIfstat() == null) {
      res.setInner(new IfNode(stat.getIfEdge().getDestination()), INDIRECT);
    } else {
      res.setInner(buildSubIfNode(stat.getIfstat()), DIRECT);
    }

    // else branch
    if (stat.iftype == IfStatement.IFTYPE_IFELSE) {
      res.setSuccessor(buildSubIfNode(stat.getElsestat()), ELSE);
    } else {
      StatEdge edge = stat.getFirstSuccessor();
      if (stsingle || edge.getType() != StatEdge.TYPE_REGULAR) {
        res.setSuccessor(new IfNode(edge.getDestination()), INDIRECT);
      } else {
        res.setSuccessor(buildSubIfNode(edge.getDestination()), DIRECT);
      }
    }


    return res;
  }

  // will produce one of the following:
  // node [IfStatement] {
  //   inner [Any Statement] or [Goto]
  // }
  // successor [Goto]
  // or
  // node [Non-IfStatement] // or an if-else statement
  // successor [Goto] // or null if there isn't a successor
  private static IfNode buildSubIfNode(Statement statement) {
    IfNode ifnode = new IfNode(statement);

    if (statement instanceof IfStatement && ((IfStatement) statement).iftype == IfStatement.IFTYPE_IF) {
      IfStatement ifStatement = (IfStatement) statement;
      ifnode.setInner(new IfNode(ifStatement.getIfEdge().getDestination()), ifStatement.getIfstat() == null ? INDIRECT : DIRECT);
      // note that the successor is always indirect, cause if it were direct, the 'if' should have been wrapped in a sequence
    }

    if (statement.hasAnySuccessor()) { // note that IFTYPE_IF always has a successor
      ifnode.setSuccessor(new IfNode(statement.getFirstSuccessor().getDestination()), INDIRECT);
    }

    return ifnode;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.value.id);

    if (this.innerNode != null) {
      sb.append(": inner(").append(this.innerType).append(") {").append(this.innerNode).append("}");
    }
    if (this.successorNode != null) {
      if (this.innerNode == null){
        sb.append(": ");
      }
      sb.append(" successor(").append(this.successorType).append(") {").append(this.successorNode).append("}");
    }

    return sb.toString();
  }
}