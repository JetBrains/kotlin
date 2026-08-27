public final class Foo /* common.pack.Foo*/ {
  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  public final void foo(int);//  foo(int)

  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to version 1.", level = kotlin.DeprecationLevel.ERROR)
  public final void foo(int, @kotlin.IntroducedAt(version = "1") int);//  foo(int, int)

  public  Foo();//  .ctor()

  public final void foo(int, @kotlin.IntroducedAt(version = "1") int, @kotlin.IntroducedAt(version = "2") int);//  foo(int, int, int)
}
