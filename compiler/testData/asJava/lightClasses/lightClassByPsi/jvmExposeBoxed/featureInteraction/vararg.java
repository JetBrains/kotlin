public final class Bar /* Bar*/ {
  @kotlin.jvm.JvmExposeBoxed()
  public final /* vararg */ void foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() StringWrapper @org.jetbrains.annotations.NotNull() ...);//  foo(@org.jetbrains.annotations.NotNull() StringWrapper @org.jetbrains.annotations.NotNull() [])

  public  Bar();//  .ctor()
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

@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class VarargKt /* VarargKt*/ {
  @kotlin.jvm.JvmExposeBoxed()
  public static final /* vararg */ void foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() StringWrapper @org.jetbrains.annotations.NotNull() ...);//  foo(@org.jetbrains.annotations.NotNull() StringWrapper @org.jetbrains.annotations.NotNull() [])
}
