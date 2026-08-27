public final class Foo /* Foo*/ {
  @<error>()
  @kotlin.Throws(exceptionClasses = {IOException.class})
  public final void foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() int);//  foo(@org.jetbrains.annotations.NotNull() int)

  public  Foo();//  .ctor()
}
