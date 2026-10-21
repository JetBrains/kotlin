@kotlin.jvm.JvmInline()
public final class IC /* IC*/ {
  private final int x;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getX();//  getX()

  public int hashCode();//  hashCode()
}

@kotlin.jvm.JvmRecord()
public final record Rec /* Rec*/(int u, int i, int ic) {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Rec.Companion Companion;

  private final int i;

  private final int ic;

  private final int u;

  private  Rec(int, int, int);//  .ctor(int, int, int)

  public final int component2();//  component2()

  public final int i();//  i()

  class Companion ...
}

public static final class Companion /* Rec.Companion*/ {
  private  Companion();//  .ctor()
}
