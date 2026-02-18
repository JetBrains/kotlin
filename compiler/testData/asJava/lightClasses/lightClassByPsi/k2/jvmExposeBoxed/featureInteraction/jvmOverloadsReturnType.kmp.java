public final class JvmOverloadsReturnTypeKt /* JvmOverloadsReturnTypeKt*/ {
  @<error>()
  @<error>()
  @kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  foo(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String)
}

@<error>()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
