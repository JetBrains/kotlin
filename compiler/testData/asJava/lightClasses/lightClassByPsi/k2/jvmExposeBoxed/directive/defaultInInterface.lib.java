@kotlin.jvm.JvmInline()
public final class IC /* IC*/ {
  private final int i;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  IC(int);//  .ctor(int)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public final int getI();//  getI()

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(int, java.lang.Object);//  equals-impl(int, java.lang.Object)

  public static final boolean equals-impl0(int, int);//  equals-impl0(int, int)

  public static int constructor-impl(int);//  constructor-impl(int)

  public static int hashCode-impl(int);//  hashCode-impl(int)

  public static java.lang.String toString-impl(int);//  toString-impl(int)
}

public abstract interface Test /* Test*/ {
  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  private abstract IC foo(IC);//  foo(IC)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  private abstract IC getBar();//  getBar()

  private abstract IC getBar-qjS0p_s();//  getBar-qjS0p_s()

  private abstract int foo-Eh1mVAw(int);//  foo-Eh1mVAw(int)

  public abstract int test-Eh1mVAw(int);//  test-Eh1mVAw(int)

  public static final class DefaultImpls /* Test.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static int test-Eh1mVAw(@org.jetbrains.annotations.NotNull() Test, int);//  test-Eh1mVAw(Test, int)
  }
}

public final class TestClass /* TestClass*/ implements Test {
  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public IC test(@org.jetbrains.annotations.NotNull() IC);//  test(IC)

  public  TestClass();//  .ctor()

  public int test-Eh1mVAw(int);//  test-Eh1mVAw(int)
}
