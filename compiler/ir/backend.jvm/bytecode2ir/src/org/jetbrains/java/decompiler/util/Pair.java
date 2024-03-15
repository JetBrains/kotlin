package org.jetbrains.java.decompiler.util;

import java.util.Objects;

public final class Pair<A, B> {
  public final A a;
  public final B b;

  private Pair(A a, B b) {
    this.a = a;
    this.b = b;
  }

  public static <A, B> Pair<A, B> of(A a, B b) {
    return new Pair<>(a, b);
  }

  @Override
  public int hashCode() {
    return a.hashCode() ^ b.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Pair<?, ?> that = (Pair<?, ?>) o;
    return Objects.equals(a, that.a) && Objects.equals(b, that.b);
  }

  @Override
  public String toString() {
    return "Pair{" + a + "," + b + '}';
  }
}
