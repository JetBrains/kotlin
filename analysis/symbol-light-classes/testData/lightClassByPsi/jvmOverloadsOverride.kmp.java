public class Base /* Base*/ {
  public  Base();//  .ctor()

  public void foo(int, int, int);//  foo(int, int, int)
}

public final class Derived /* Derived*/ extends Base {
  @java.lang.Override()
  @kotlin.Suppress(names = {"OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS"})
  public void foo(int, int, int);//  foo(int, int, int)

  public  Derived();//  .ctor()
}
