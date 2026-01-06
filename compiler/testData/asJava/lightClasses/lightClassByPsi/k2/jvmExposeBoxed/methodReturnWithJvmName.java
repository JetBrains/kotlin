@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class Foo /* Foo*/ {
  @kotlin.jvm.JvmExposeBoxed(jvmName = "bar")
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() StringWrapper bar();//  bar()

  @kotlin.jvm.JvmName(name = "foo")
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String foo();//  foo()

  public  Foo();//  .ctor()
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
