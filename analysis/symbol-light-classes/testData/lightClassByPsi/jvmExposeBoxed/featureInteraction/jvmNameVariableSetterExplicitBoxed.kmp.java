public final class Foo /* Foo*/ {
  @<error>()
  @<error>()
  public final void setFoo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setFoo(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getFoo();//  getFoo()

  public  Foo();//  .ctor()
}

@<error>()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()
}
