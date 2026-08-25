public final class Bar /* Bar*/ {
  @<error>()
  public final /* vararg */ void foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  foo(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public  Bar();//  .ctor()
}

@<error>()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class VarargKt /* VarargKt*/ {
  @<error>()
  public static final /* vararg */ void foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  foo(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])
}
