package org.jetbrains.java.decompiler.modules.decompiler.flow;

public enum DirectNodeType {
  DIRECT("") {
    @Override
    protected String makeId(int statId) {
      return "" + statId;
    }
  },
  TAIL("tail"),
  INIT("init"),
  CONDITION("cond"),
  INCREMENT("inc"),
  TRY("try"),
  FOREACH_VARDEF("foreach"),
  CASE("case");

  private final String name;

  DirectNodeType(String name) {
    this.name = name;
  }

  protected String makeId(int statId) {
    return statId + "_" + name;
  }
}
