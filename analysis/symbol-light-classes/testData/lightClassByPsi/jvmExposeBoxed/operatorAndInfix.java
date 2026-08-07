@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
@kotlin.jvm.JvmInline()
public final class Money /* Money*/ {
  private final int cents;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Money combineWith(@org.jetbrains.annotations.NotNull() Money);//  combineWith(@org.jetbrains.annotations.NotNull() Money)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Money plus(@org.jetbrains.annotations.NotNull() Money);//  plus(@org.jetbrains.annotations.NotNull() Money)

  @kotlin.jvm.JvmExposeBoxed()
  public  Money(int);//  .ctor(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getCents();//  getCents()

  public int hashCode();//  hashCode()
}
