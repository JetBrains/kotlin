public final class GlobalLateinitKt /* GlobalLateinitKt*/ {
  public static @org.jetbrains.annotations.NotNull() java.lang.String foo;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String getFoo();//  getFoo()

  public static final void setFoo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setFoo(@org.jetbrains.annotations.NotNull() java.lang.String)
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
