public class Base /* Base*/ {
  @kotlin.jvm.JvmOverloads()
  public void foo();//  foo()

  @kotlin.jvm.JvmOverloads()
  public void foo(int);//  foo(int)

  @kotlin.jvm.JvmOverloads()
  public void foo(int, int);//  foo(int, int)

  @kotlin.jvm.JvmOverloads()
  public void foo(int, int, int);//  foo(int, int, int)

  public  Base();//  .ctor()
}

public final class Derived /* Derived*/ extends Base {
  @java.lang.Override()
  @kotlin.Suppress(names = {"OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS"})
  @kotlin.jvm.JvmOverloads()
  public void foo(int, int, int);//  foo(int, int, int)

  public  Derived();//  .ctor()
}
