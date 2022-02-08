public final class C /* C*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String p2;

  @org.jetbrains.annotations.NotNull()
  public static final C.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private final java.lang.String type;

  private final boolean p1;

  @<error>()
  public  C(@org.jetbrains.annotations.Nullable() java.lang.String, boolean, @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String, boolean, java.lang.String)

  @<error>()
  public final void bar(int, double, @org.jetbrains.annotations.NotNull() java.lang.String);//  bar(int, double, java.lang.String)

  @<error>()
  public final void baz(int, double, @org.jetbrains.annotations.NotNull() java.lang.String);//  baz(int, double, java.lang.String)

  @<error>()
  public final void foo(int, double, @org.jetbrains.annotations.NotNull() java.lang.String);//  foo(int, double, java.lang.String)

  @<error>()
  public final void foobar(int, double, @org.jetbrains.annotations.NotNull() java.lang.String);//  foobar(int, double, java.lang.String)

  @<error>()
  public final void foobarbaz(int, double, @org.jetbrains.annotations.NotNull() java.lang.String);//  foobarbaz(int, double, java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getP2();//  getP2()

  @org.jetbrains.annotations.Nullable()
  public final java.lang.String getType();//  getType()

  public final boolean getP1();//  getP1()


  class Companion ...

  }

public static final class Companion /* C.Companion*/ {
  @<error>()
  @<error>()
  public final void fooStatic(int, double, @org.jetbrains.annotations.NotNull() java.lang.String);//  fooStatic(int, double, java.lang.String)

  @<error>()
  public final void foo123(int, double, @org.jetbrains.annotations.NotNull() java.lang.String);//  foo123(int, double, java.lang.String)

  private  Companion();//  .ctor()

}
