public class Base /* Base*/ {
  @kotlin.jvm.JvmOverloads()
  public final void foo();//  foo()

  @kotlin.jvm.JvmOverloads()
  public final void foo(int);//  foo(int)

  @kotlin.jvm.JvmOverloads()
  public final void foo(int, int);//  foo(int, int)

  @kotlin.jvm.JvmOverloads()
  public void foo(int, int, int);//  foo(int, int, int)

  public  Base();//  .ctor()
}

public final class Derived /* Derived*/ extends Base {
  @kotlin.jvm.JvmOverloads()
  public void foo(int, int, int);//  foo(int, int, int)

  public  Derived();//  .ctor()
}
