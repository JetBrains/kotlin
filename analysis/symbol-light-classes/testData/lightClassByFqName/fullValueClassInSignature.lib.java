public final class Usage /* pack.Usage*/ {
  @org.jetbrains.annotations.NotNull()
  private final pack.MultiField multi;

  @org.jetbrains.annotations.NotNull()
  private final pack.SingleField single;

  @org.jetbrains.annotations.NotNull()
  private pack.MultiField mutable;

  @org.jetbrains.annotations.NotNull()
  public final /* vararg */ pack.MultiField consumeAll(@org.jetbrains.annotations.NotNull() pack.MultiField...);//  consumeAll(pack.MultiField[])

  @org.jetbrains.annotations.NotNull()
  public final pack.MultiField consume(@org.jetbrains.annotations.NotNull() pack.SingleField, @org.jetbrains.annotations.NotNull() pack.MultiField);//  consume(pack.SingleField, pack.MultiField)

  @org.jetbrains.annotations.NotNull()
  public final pack.MultiField getMulti();//  getMulti()

  @org.jetbrains.annotations.NotNull()
  public final pack.MultiField getMutable();//  getMutable()

  @org.jetbrains.annotations.NotNull()
  public final pack.SingleField getSingle();//  getSingle()

  public  Usage(@org.jetbrains.annotations.NotNull() pack.SingleField, @org.jetbrains.annotations.NotNull() pack.MultiField);//  .ctor(pack.SingleField, pack.MultiField)

  public final void setMutable(@org.jetbrains.annotations.NotNull() pack.MultiField);//  setMutable(pack.MultiField)
}
