public final class FullValue /* exposed.FullValue*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String y;

  private final int x;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String render();//  render()

  @kotlin.jvm.JvmExposeBoxed()
  public  FullValue();//  .ctor()

  @kotlin.jvm.JvmExposeBoxed()
  public  FullValue(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getY();//  getY()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final int getX();//  getX()

  public int hashCode();//  hashCode()
}

public final class Usage /* exposed.Usage*/ {
  @org.jetbrains.annotations.NotNull()
  private exposed.FullValue mutable;

  @org.jetbrains.annotations.NotNull()
  private final exposed.FullValue value;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final exposed.FullValue consume(@org.jetbrains.annotations.NotNull() exposed.FullValue);//  consume(exposed.FullValue)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final exposed.FullValue getMutable();//  getMutable()

  @kotlin.jvm.JvmExposeBoxed()
  public final void setMutable(@org.jetbrains.annotations.NotNull() exposed.FullValue);//  setMutable(exposed.FullValue)

  @org.jetbrains.annotations.NotNull()
  public final exposed.FullValue getValue();//  getValue()

  public  Usage(@org.jetbrains.annotations.NotNull() exposed.FullValue);//  .ctor(exposed.FullValue)
}
