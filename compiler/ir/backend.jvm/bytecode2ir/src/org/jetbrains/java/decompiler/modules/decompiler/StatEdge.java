// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement.EdgeDirection;

import java.util.ArrayList;
import java.util.List;

// A connection between 2 statements.
// Describes edges in graphs where statements are the vertices.
public class StatEdge {
  // Represents direct control flow between 2 statements
  public static final int TYPE_REGULAR = 1;
  // Represents implicit control flow between any statement in a try block and the catch blocks
  // This is because any statement in the try block can throw and execution can flow to the catch blocks from there
  public static final int TYPE_EXCEPTION = 2;
  // Represents control flow out of a statement and to the next statement
  // Also represents returns
  public static final int TYPE_BREAK = 4;
  // Represents control flow back up to a previous statement, marking a loop
  public static final int TYPE_CONTINUE = 8;
  // Represents exits from finally blocks
  public static final int TYPE_FINALLYEXIT = 32;

  public static final int[] TYPES = new int[]{
    TYPE_REGULAR,
    TYPE_EXCEPTION,
    TYPE_BREAK,
    TYPE_CONTINUE,
    TYPE_FINALLYEXIT
  };

  private int type;

  private Statement source;

  private Statement destination;

  private List<String> exceptions;

  // The statement that this edge is enclosed in.
  // Take for example, this structure, where we are analyzing the break edge:
  //
  // label1: {
  //   if (...) {
  //     break label1;
  //   }
  // }
  // System.out.println("Test");
  //
  // The body of the if statement would be considered the source, and the println would be considered the destination.
  // The sequence statement enclosing the if would be considered the closure.
  // BREAK and CONTINUE edge types should always have a closure! (except for when break edges are return edges)
  public Statement closure;

  // Whether this edge is labeled or not.
  public boolean labeled = true;

  // Whether this edge is explicitly defined or implicit.
  public boolean explicit = true;

  // Whether this edge can be inlined to simplify the decompiled output or not.
  public boolean canInline = true;

  // If this edge is a continue edge set as a break edge for readability
  public boolean phantomContinue = false;

  public StatEdge(int type, Statement source, Statement destination, Statement closure) {
    this(type, source, destination);
    this.closure = closure;
  }

  public StatEdge(int type, Statement source, Statement destination) {
    this.type = type;
    this.source = source;
    this.destination = destination;

    ValidationHelper.notNull(source);
    ValidationHelper.notNull(destination);
  }

  public StatEdge(Statement source, Statement destination, List<String> exceptions) {
    this(TYPE_EXCEPTION, source, destination);
    if (exceptions != null) {
      this.exceptions = new ArrayList<>(exceptions);
    }
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public Statement getSource() {
    return source;
  }

  public void setSource(Statement source) {
    this.source = source;
  }

  /**
   * Makes this edge point from a different source. This will
   * remove this edge as a successor of the old source,
   * add it as a successor of the new source,
   * and update the predecessor of the current destination.
   * @param newSource the new source of this edge
   */
  public void changeSource(Statement newSource) {
    ValidationHelper.notNull(newSource);

    Statement oldSource = this.source;
    oldSource.removeEdgeInternal(EdgeDirection.FORWARD, this);
    newSource.addEdgeInternal(EdgeDirection.FORWARD, this);
    this.source = newSource;
  }

  public Statement getDestination() {
    return destination;
  }

  public void setDestination(Statement destination) {
    this.destination = destination;
  }

  /**
   * Makes this edge point from a different destination. This will
   * remove this edge as a predecessor of the old destination,
   * add it as a predecessor of the new destination,
   * and update the successor of the current source.
   * @param newDestination the new destination of this edge
   */
  public void changeDestination(Statement newDestination) {
    ValidationHelper.notNull(newDestination);

    Statement oldDestination = this.destination;
    oldDestination.removeEdgeInternal(EdgeDirection.BACKWARD, this);
    newDestination.addEdgeInternal(EdgeDirection.BACKWARD, this);
    this.destination = newDestination;
  }


  /**
   * Updates the type of this edge.
   * It will notify both the source and the destination of the change.
   * @param type
   */
  public void changeType(int type) {
    this.source.changeEdgeType(EdgeDirection.FORWARD, this, type);
  }

  /**
   * Removes this edge from the graph.
   * The source will lose this edge as a successor,
   * the destination will lose this edge as a predecessor.
   * and if there is a labeled closure, it will be removed from there as well.
   */
  public void remove() {
    this.source.removeEdgeInternal(EdgeDirection.FORWARD, this);
    this.destination.removeEdgeInternal(EdgeDirection.BACKWARD, this);

    if (this.closure != null) {
      this.closure.getLabelEdges().remove(this);
    }
  }


  /**
   * Remove the closure of this edge. This edge will no
   * longer be labeled.
   */
  public void removeClosure() {
    this.closure.getLabelEdges().remove(this);
    this.labeled = false;
    this.closure = null;
  }

  public List<String> getExceptions() {
    return this.exceptions;
  }

  //	public void setException(String exception) {
  //		this.exception = exception;
  //	}

  @Override
  public String toString() {
    return this.type + ": " + this.source.toString() + " -> " + this.destination.toString() + ((this.closure == null) ? "" : " (" + this.closure + ")") + ((this.exceptions == null) ? "" : " Exceptions: " + this.exceptions);
  }
}
