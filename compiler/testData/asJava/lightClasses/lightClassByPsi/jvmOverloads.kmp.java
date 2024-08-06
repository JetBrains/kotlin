public final class C /* C*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String p2;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() C.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() java.lang.String type;

  private final boolean p1 = false /* initializer type: boolean */;

  @<error>()
  public  C(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.String, boolean, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.Nullable() java.lang.String, boolean, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public final void bar(int, double, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  bar(int, double, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public final void baz(int, double, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  baz(int, double, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public final void foo(int, double, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  foo(int, double, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public final void foobar(int, double, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  foobar(int, double, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public final void foobarbaz(int, double, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  foobarbaz(int, double, @org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getP2();//  getP2()

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() java.lang.String getType();//  getType()

  public final boolean getP1();//  getP1()

  class Companion ...
}

public static final class Companion /* C.Companion*/ {
  @<error>()
  @<error>()
  public final void fooStatic(int, double, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  fooStatic(int, double, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public final void foo123(int, double, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  foo123(int, double, @org.jetbrains.annotations.NotNull() java.lang.String)

  private  Companion();//  .ctor()
}
