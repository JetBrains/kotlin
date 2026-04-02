@<error>()
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
  private abstract @org.jetbrains.annotations.NotNull() IC foo(@org.jetbrains.annotations.NotNull() IC);//  foo(@org.jetbrains.annotations.NotNull() IC)

  @kotlin.jvm.JvmExposeBoxed()
  private abstract @org.jetbrains.annotations.Nullable() IC getBar();//  getBar()

  public static final class DefaultImpls /* Test.DefaultImpls*/ {
    @kotlin.jvm.JvmExposeBoxed()
    private static @org.jetbrains.annotations.NotNull() IC foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Test, @org.jetbrains.annotations.NotNull() IC);//  foo(@org.jetbrains.annotations.NotNull() Test, @org.jetbrains.annotations.NotNull() IC)

    @kotlin.jvm.JvmExposeBoxed()
    private static final @org.jetbrains.annotations.Nullable() IC getBar(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Test);//  getBar(@org.jetbrains.annotations.NotNull() Test)
  }
}

public final class TestClass /* TestClass*/ implements Test {
  public  TestClass();//  .ctor()
}
