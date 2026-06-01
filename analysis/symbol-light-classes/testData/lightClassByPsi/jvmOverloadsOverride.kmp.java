public class Base /* Base*/ {
  @<error>()
  public void foo(int, int, int);//  foo(int, int, int)

  public  Base();//  .ctor()
}

public final class Derived /* Derived*/ extends Base {
  @<error>()
  @java.lang.Override()
  @kotlin.Suppress(names = {"OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS"})
  public void foo(int, int, int);//  foo(int, int, int)

  public  Derived();//  .ctor()
}
