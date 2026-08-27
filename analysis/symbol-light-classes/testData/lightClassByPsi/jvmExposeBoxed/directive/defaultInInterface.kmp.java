@<error>()
public final class IC /* IC*/ {
  private final int i;

  @kotlin.jvm.JvmExposeBoxed()
  public  IC(int);//  .ctor(int)

  public final int getI();//  getI()
}

public abstract interface Test /* Test*/ {
  private abstract @org.jetbrains.annotations.Nullable() IC getBar();//  getBar()

  private abstract int foo(int);//  foo(int)

  public abstract int test(int);//  test(int)

  public static final class DefaultImpls /* Test.DefaultImpls*/ {
    private static final @org.jetbrains.annotations.Nullable() IC getBar(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Test);//  getBar(@org.jetbrains.annotations.NotNull() Test)

    private static int foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Test, int);//  foo(@org.jetbrains.annotations.NotNull() Test, int)

    public static int test(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Test, int);//  test(@org.jetbrains.annotations.NotNull() Test, int)
  }
}

public final class TestClass /* TestClass*/ implements Test {
  @java.lang.Override()
  public int test(int);//  test(int)

  public  TestClass();//  .ctor()
}
