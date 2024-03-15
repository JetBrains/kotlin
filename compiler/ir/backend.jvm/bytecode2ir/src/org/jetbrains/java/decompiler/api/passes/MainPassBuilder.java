package org.jetbrains.java.decompiler.api.passes;

import org.jetbrains.java.decompiler.api.GraphParser;

import java.util.ArrayList;
import java.util.List;

public final class MainPassBuilder {
  private final List<Pass> passes = new ArrayList<>();
  private GraphParser parser;

  public void setGraphParser(GraphParser parser) {
    this.parser = parser;
  }

  public void addPass(String name, Pass pass) {
    passes.add(new NamedPass(name, pass));
  }
}
