public final class Foo /* Foo*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() IntWrapper getFoo();//  getFoo()

  @kotlin.jvm.JvmExposeBoxed()
  public final void setFoo(@org.jetbrains.annotations.NotNull() IntWrapper);//  setFoo(@org.jetbrains.annotations.NotNull() IntWrapper)

  public  Foo();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class IntWrapper /* IntWrapper*/ {
  private final int i;

  @kotlin.jvm.JvmExposeBoxed()
  public  IntWrapper(int);//  .ctor(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getI();//  getI()

  public int hashCode();//  hashCode()
}
