public final class GlobalVariableKt /* GlobalVariableKt*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String getFoo();//  getFoo()

  @<error>()
  public static final void setFoo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setFoo(@org.jetbrains.annotations.NotNull() java.lang.String)
}

@<error>()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()
}
