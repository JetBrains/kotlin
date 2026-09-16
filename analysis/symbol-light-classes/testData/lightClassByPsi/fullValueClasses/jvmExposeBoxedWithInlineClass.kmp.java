public final class InlineClass /* pack.InlineClass*/ {
  private final int value;

  @kotlin.jvm.JvmExposeBoxed()
  public  InlineClass(int);//  .ctor(int)

  public final int getValue();//  getValue()
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

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.InlineClass funWithInlineReturnType();//  funWithInlineReturnType()

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.InlineClass, int);//  .ctor(@org.jetbrains.annotations.NotNull() pack.InlineClass, int)

  @kotlin.jvm.JvmExposeBoxed()
  public final void funWithInlineParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.InlineClass);//  funWithInlineParameter(@org.jetbrains.annotations.NotNull() pack.InlineClass)

  @kotlin.jvm.JvmExposeBoxed()
  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.InlineClass getInline();//  getInline()

  public final int getRegular();//  getRegular()
}
