public class Base /* Base*/ {
  public  Base();//  .ctor()
}

public final class Derived /* Derived*/ extends Base {
  public  Derived();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class IC /* IC*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class Prefixed /* Prefixed*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String prefix;

  @org.jetbrains.annotations.NotNull()
  private final byte @org.jetbrains.annotations.NotNull() [] items;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String result();//  result()

  private /* vararg */  Prefixed(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() byte...);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, byte[])
}

public final class UIntStack /* UIntStack*/ {
  @org.jetbrains.annotations.NotNull()
  private final int @org.jetbrains.annotations.NotNull() [] items;

  private /* vararg */  UIntStack(@org.jetbrains.annotations.NotNull() int...);//  .ctor(int[])
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class, kotlin.ExperimentalUnsignedTypes.class})
public final class UnsignedVarargKt /* UnsignedVarargKt*/ {
}
