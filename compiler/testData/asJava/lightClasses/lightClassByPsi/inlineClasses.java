public enum Foo /* Foo*/ {
  ;

  private final int x;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public final int getX();//  getX()
}

public final class InlineInheritance /* InlineInheritance*/ implements I {
  private final int v;

  @java.lang.Override()
  public int getX();//  getX()

  @java.lang.Override()
  public int y();//  y()

  public final int getV();//  getV()
}

public final class InlinedDelegate /* InlinedDelegate*/<T>  {
  private T node;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final T getNode();//  getNode()

  public int hashCode();//  hashCode()
}

public final class UInt /* UInt*/ {
  private final int value;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
