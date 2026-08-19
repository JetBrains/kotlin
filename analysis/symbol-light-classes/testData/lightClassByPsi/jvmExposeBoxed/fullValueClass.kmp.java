public final class FullValue /* exposed.FullValue*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String y;

  private final int x;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getY();//  getY()

  public final int getX();//  getX()
}

@<error>()
public final class Usage /* exposed.Usage*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() exposed.FullValue mutable;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() exposed.FullValue value;

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() exposed.FullValue consume(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() exposed.FullValue);//  consume(@org.jetbrains.annotations.NotNull() exposed.FullValue)

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() exposed.FullValue getMutable();//  getMutable()

  @<error>()
  public final void setMutable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() exposed.FullValue);//  setMutable(@org.jetbrains.annotations.NotNull() exposed.FullValue)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() exposed.FullValue getValue();//  getValue()

  public  Usage(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() exposed.FullValue);//  .ctor(@org.jetbrains.annotations.NotNull() exposed.FullValue)
}
