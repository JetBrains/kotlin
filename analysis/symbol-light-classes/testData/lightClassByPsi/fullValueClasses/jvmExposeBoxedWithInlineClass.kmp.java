public final class InlineClass /* pack.InlineClass*/ {
  private final int value;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public  InlineClass(int);//  .ctor(int)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getValue();//  getValue()

  public int hashCode();//  hashCode()
}

public final class Regular /* pack.Regular*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() pack.InlineClass inline;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() pack.ValueClass value;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.InlineClass getInline();//  getInline()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass getValue();//  getValue()

  public  Regular(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.InlineClass);//  .ctor(@org.jetbrains.annotations.NotNull() pack.ValueClass, @org.jetbrains.annotations.NotNull() pack.InlineClass)
}

public final class ValueClass /* pack.ValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() pack.InlineClass inline;

  private final int regular;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.InlineClass funWithInlineReturnType();//  funWithInlineReturnType()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.InlineClass getInline();//  getInline()

  public  ValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.InlineClass, int);//  .ctor(@org.jetbrains.annotations.NotNull() pack.InlineClass, int)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getRegular();//  getRegular()

  public final void funWithInlineParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.InlineClass);//  funWithInlineParameter(@org.jetbrains.annotations.NotNull() pack.InlineClass)

  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  public int hashCode();//  hashCode()
}
