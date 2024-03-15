package org.jetbrains.java.decompiler.api.passes;

public final class NamedPass implements Pass {
  private final String name;
  private final Pass pass;

  public NamedPass(String name, Pass pass) {
    this.name = name;
    this.pass = pass;
  }

  @Override
  public boolean run(PassContext ctx) {
    boolean res = this.pass.run(ctx);

    if (res) {
      ctx.getRec().add(this.name, ctx.getRoot());
    }

    return res;
  }
}
