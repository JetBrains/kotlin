public abstract interface Interface /* pack.Interface*/ {
  public abstract int getValue();//  getValue()
}

@<error>()
public final class ValueClass /* pack.ValueClass*/ implements pack.Interface {
  private final int value;

  @java.lang.Override()
  public int getValue();//  getValue()

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueClass(int);//  .ctor(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
