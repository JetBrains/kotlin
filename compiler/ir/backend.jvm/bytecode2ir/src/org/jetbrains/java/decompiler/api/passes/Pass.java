package org.jetbrains.java.decompiler.api.passes;

@FunctionalInterface
public interface Pass {
  Pass NO_OP = ctx -> false;

  boolean run(PassContext ctx);
}
