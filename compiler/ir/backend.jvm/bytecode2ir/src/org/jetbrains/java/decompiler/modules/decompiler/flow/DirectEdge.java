package org.jetbrains.java.decompiler.modules.decompiler.flow;

import java.util.Objects;

public final class DirectEdge {
  private final DirectNode source;
  private final DirectNode destination;
  private final DirectEdgeType type;

  public DirectEdge(DirectNode source, DirectNode destination, DirectEdgeType type) {
    this.source = source;
    this.destination = destination;
    this.type = type;
  }

  public static DirectEdge of(DirectNode source, DirectNode destination) {
    return new DirectEdge(source, destination, DirectEdgeType.REGULAR);
  }

  public static DirectEdge exception(DirectNode source, DirectNode destination) {
    return new DirectEdge(source, destination, DirectEdgeType.EXCEPTION);
  }

  public DirectNode getSource() {
    return source;
  }

  public DirectNode getDestination() {
    return destination;
  }

  public DirectEdgeType getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DirectEdge that = (DirectEdge) o;
    return source.equals(that.source) && destination.equals(that.destination) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, destination, type);
  }

  @Override
  public String toString() {
    return "(" + this.source + " -> " + this.destination + " | " + this.type + ")";
  }
}
