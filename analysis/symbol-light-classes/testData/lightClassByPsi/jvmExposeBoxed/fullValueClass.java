public final class FullValue /* exposed.FullValue*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String y;

  private final int x;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String render();//  render()

  @kotlin.jvm.JvmExposeBoxed()
  public  FullValue();//  .ctor()

  @kotlin.jvm.JvmExposeBoxed()
  public  FullValue(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getY();//  getY()

  public final int getX();//  getX()
}

public final class Usage /* exposed.Usage*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() exposed.FullValue mutable;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() exposed.FullValue value;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() exposed.FullValue consume(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() exposed.FullValue);//  consume(@org.jetbrains.annotations.NotNull() exposed.FullValue)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() exposed.FullValue getMutable();//  getMutable()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() exposed.FullValue getValue();//  getValue()

  public  Usage(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() exposed.FullValue);//  .ctor(@org.jetbrains.annotations.NotNull() exposed.FullValue)

  public final void setMutable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() exposed.FullValue);//  setMutable(@org.jetbrains.annotations.NotNull() exposed.FullValue)
}
