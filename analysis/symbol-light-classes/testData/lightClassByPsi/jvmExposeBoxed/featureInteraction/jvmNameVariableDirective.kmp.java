public final class Foo /* Foo*/ {
  @<error>()
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getFoo2();//  getFoo2()

  @<error>()
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getFoo3(int);//  getFoo3(int)

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getFoo1();//  getFoo1()

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getFoo4(int);//  getFoo4(int)

  @<error>()
  public final void setFoo2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setFoo2(@org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public final void setFoo3(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setFoo3(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  public  Foo();//  .ctor()
}

@<error>()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @kotlin.jvm.JvmExposeBoxed()
  public  StringWrapper(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()
}
