public final class InlineClass /* pack.InlineClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String value;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()
}

public final class Regular /* pack.Regular*/ {
  public  Regular();//  .ctor()

  public final void inlineParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.InlineClass);//  inlineParameter(@org.jetbrains.annotations.NotNull() pack.InlineClass)

  public final void singleFieldParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.SingleFieldValueClass);//  singleFieldParameter(@org.jetbrains.annotations.NotNull() pack.SingleFieldValueClass)
}

public final class SingleFieldValueClass /* pack.SingleFieldValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String value;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()
}
