@<error>()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() java.lang.String s;

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() java.lang.String getS();//  getS()
}

@<error>()
public final class Test /* Test*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() StringWrapper s;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String ok();//  ok()

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() StringWrapper getS();//  getS()

  public  Test(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() StringWrapper);//  .ctor(@org.jetbrains.annotations.Nullable() StringWrapper)
}
