@kotlin.jvm.JvmInline()
public final class IC /* IC*/ {
  private final int i;

  @kotlin.jvm.JvmExposeBoxed()
  public  IC(int);//  .ctor(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getI();//  getI()

  public int hashCode();//  hashCode()
}

public abstract interface Test /* Test*/ {
  @kotlin.jvm.JvmExposeBoxed()
  private abstract @org.jetbrains.annotations.Nullable() IC getBar();//  getBar()

  class DefaultImpls ...
}

public final class TestClass /* TestClass*/ implements Test {
  public  TestClass();//  .ctor()
}
