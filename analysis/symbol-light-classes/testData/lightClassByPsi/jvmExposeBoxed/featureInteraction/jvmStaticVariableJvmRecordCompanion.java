@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
@kotlin.jvm.JvmRecord()
public final record Rec /* Rec*/(int i) {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Rec.Companion Companion;

  private final int i;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() StringWrapper getBaz();//  getBaz()

  @kotlin.jvm.JvmExposeBoxed()
  public static final void setBaz(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() StringWrapper);//  setBaz(@org.jetbrains.annotations.NotNull() StringWrapper)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Rec copy(int);//  copy(int)

  public  Rec(int);//  .ctor(int)

  public final int component1();//  component1()

  public final int i();//  i()

  class Companion ...
}

public static final class Companion /* Rec.Companion*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() StringWrapper getBaz();//  getBaz()

  @kotlin.jvm.JvmExposeBoxed()
  public final void setBaz(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() StringWrapper);//  setBaz(@org.jetbrains.annotations.NotNull() StringWrapper)

  private  Companion();//  .ctor()
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
