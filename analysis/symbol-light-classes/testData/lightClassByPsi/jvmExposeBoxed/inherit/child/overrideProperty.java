public abstract interface I /* I*/ {
  public abstract int getZ();//  getZ()
}

public class P /* P*/ {
  private int y = 1 /* initializer type: int */;

  public  P();//  .ctor()
}

public final class Q /* Q*/ extends P {
  private int y = 2 /* initializer type: int */;

  @java.lang.Override()
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.UInt getX();//  getX()

  @java.lang.Override()
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.UInt getY();//  getY()

  @java.lang.Override()
  @kotlin.jvm.JvmExposeBoxed()
  public void setY(@org.jetbrains.annotations.NotNull() kotlin.UInt);//  setY(@org.jetbrains.annotations.NotNull() kotlin.UInt)

  public  Q();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class VC /* VC*/ implements I {
  private final int v;

  @java.lang.Override()
  public int getZ();//  getZ()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getV();//  getV()

  public int hashCode();//  hashCode()
}
