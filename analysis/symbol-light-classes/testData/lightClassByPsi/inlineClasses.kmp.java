public final class ConstructorWithInlineContextParameter /* ConstructorWithInlineContextParameter*/ {
  public  ConstructorWithInlineContextParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)
}

public final class ConstructorWithInlineContextParameterAndJvmOverloads /* ConstructorWithInlineContextParameterAndJvmOverloads*/ {
  public  ConstructorWithInlineContextParameterAndJvmOverloads(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)
}

@kotlin.jvm.JvmInline()
public enum Foo /* Foo*/ {
  A,
  B;

  private final int x;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public final int getX();//  getX()
}

@kotlin.jvm.JvmInline()
public final class InlineInheritance /* InlineInheritance*/ implements I {
  private final int v;

  @java.lang.Override()
  public int getX();//  getX()

  @java.lang.Override()
  public int y();//  y()

  public final int getV();//  getV()
}

@kotlin.jvm.JvmInline()
public final class InlinedDelegate /* InlinedDelegate*/<T>  {
  private T node;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final T getNode();//  getNode()

  public final void setNode(T);//  setNode(T)

  public int hashCode();//  hashCode()
}

@kotlin.jvm.JvmInline()
public final class UInt /* UInt*/ {
  private final int value;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
