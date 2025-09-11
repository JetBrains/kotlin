public final class Foo /* test.pkg.Foo*/ {
  @<error>()
  @test.pkg.A()
  public  Foo();//  .ctor()

  @<error>()
  @test.pkg.A()
  public  Foo(int);//  .ctor(int)

  @test.pkg.B()
  public  Foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, int)
}
