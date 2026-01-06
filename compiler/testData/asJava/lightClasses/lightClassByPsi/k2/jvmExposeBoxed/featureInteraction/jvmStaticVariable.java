@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class Foo /* Foo*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Foo INSTANCE;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() StringWrapper getBaz();//  getBaz()

  @kotlin.jvm.JvmExposeBoxed()
  public static final void setBaz(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() StringWrapper);//  setBaz(@org.jetbrains.annotations.NotNull() StringWrapper)

  private  Foo();//  .ctor()
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
