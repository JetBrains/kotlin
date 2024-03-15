package org.jetbrains.java.decompiler.api.passes;

import org.jetbrains.java.decompiler.util.Pair;

import java.util.ArrayList;
import java.util.List;

public final class LoopingPassBuilder {
  private final List<Pair<Pass, Boolean>> passes = new ArrayList<>();
  private final String name;

  public LoopingPassBuilder(String name) {

    this.name = name;
  }

  public void addFallthroughPass(String name, Pass pass) {
    passes.add(Pair.of(new NamedPass(name, pass), false));
  }

  public void addLoopingPass(String name, Pass pass) {
    passes.add(Pair.of(new NamedPass(name, pass), true));
  }

  public Pass build() {
    return new CompiledPass(name, passes);
  }

  private static final class CompiledPass implements Pass {
    private final List<Pair<Pass, Boolean>> passes;

    public CompiledPass(String name, List<Pair<Pass, Boolean>> passes) {
      this.passes = new ArrayList<>(passes);
    }

    @Override
    public boolean run(PassContext ctx) {
      boolean loop;
      do {
        loop = false;
        for (Pair<Pass, Boolean> pass : this.passes) {
          if (pass.a.run(ctx) && pass.b) {
            loop = true;
            break;
          }
        }

      } while (loop);

      // TODO: should this return if any passed?
      return true;
    }
  }
}
