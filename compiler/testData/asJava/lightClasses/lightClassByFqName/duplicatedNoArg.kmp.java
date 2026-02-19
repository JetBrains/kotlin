public final class Foo /* test.pkg.Foo*/ {
  @<error>()
  @test.pkg.B()
  public  Foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, int)

  @test.pkg.A()
  public  Foo();//  .ctor()

  @test.pkg.A()
  public  Foo(int);//  .ctor(int)
}
