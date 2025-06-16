public final class JvmOverloadsValueParameterKt /* JvmOverloadsValueParameterKt*/ {
  @kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
  @kotlin.jvm.JvmExposeBoxed(jvmName = "bar")
  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String bar(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() StringWrapper);//  bar(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() StringWrapper)

  @kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String foo();//  foo()

  @kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  foo(@org.jetbrains.annotations.NotNull() java.lang.String)
}

@kotlin.jvm.JvmInline()
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
