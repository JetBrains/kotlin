public final class Clazz /* Clazz*/ implements Iface {
  private int valueInt;

  public  Clazz();//  .ctor()
}

public abstract interface Iface /* Iface*/ {
}

@<error>()
public final class ValueInt /* ValueInt*/ {
  private final int i;

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueInt(int);//  .ctor(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getI();//  getI()

  public int hashCode();//  hashCode()
}
