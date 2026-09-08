public final class Clazz /* Clazz*/ implements Iface {
  private int valueInt;

  @java.lang.Override()
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() ValueInt getValueInt();//  getValueInt()

  @java.lang.Override()
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() ValueInt returnValueInt();//  returnValueInt()

  @java.lang.Override()
  @kotlin.jvm.JvmExposeBoxed()
  public void setValueInt(@org.jetbrains.annotations.NotNull() ValueInt);//  setValueInt(@org.jetbrains.annotations.NotNull() ValueInt)

  @java.lang.Override()
  @kotlin.jvm.JvmExposeBoxed()
  public void useValueInt(@org.jetbrains.annotations.NotNull() ValueInt);//  useValueInt(@org.jetbrains.annotations.NotNull() ValueInt)

  public  Clazz();//  .ctor()
}

public abstract interface Iface /* Iface*/ {
}

@kotlin.jvm.JvmInline()
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
