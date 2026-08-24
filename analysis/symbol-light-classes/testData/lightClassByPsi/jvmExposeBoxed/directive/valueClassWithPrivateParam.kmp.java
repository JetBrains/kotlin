public final class OriginalClass /* pack.OriginalClass*/ {
  public  OriginalClass();//  .ctor()
}

@<error>()
public final class ValueClass /* pack.ValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() pack.OriginalClass value;

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.OriginalClass);//  .ctor(@org.jetbrains.annotations.NotNull() pack.OriginalClass)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
