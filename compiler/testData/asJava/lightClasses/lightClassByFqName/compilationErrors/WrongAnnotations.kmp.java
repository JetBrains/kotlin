public final class A /* A*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() A.Companion Companion;

  public  A();//  .ctor()

  public static final class Companion /* A.Companion*/ {
    @<error>()
    public final void f1();//  f1()

    @<error>()
    public final void f2();//  f2()

    @<error>()
    public final void f3();//  f3()

    @<error>()
    public final void f4();//  f4()

    @<error>()
    public final void f5();//  f5()

    @<error>()
    public final void f6();//  f6()

    private  Companion();//  .ctor()
  }
}
