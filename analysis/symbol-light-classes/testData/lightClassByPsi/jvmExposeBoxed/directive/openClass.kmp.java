@<error>()
public final class IC /* IC*/ {
  private final int i;

  @kotlin.jvm.JvmExposeBoxed()
  public  IC(int);//  .ctor(int)

  public final int getI();//  getI()
}

public abstract interface Test /* Test*/ {
  @org.jetbrains.annotations.Nullable()
  public abstract @org.jetbrains.annotations.Nullable() IC getFoo();//  getFoo()

  public abstract int test(int);//  test(int)
}

public class TestClass1 /* TestClass1*/ implements Test {
  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() IC getFoo();//  getFoo()

  @java.lang.Override()
  public int test(int);//  test(int)

  public  TestClass1();//  .ctor()
}
