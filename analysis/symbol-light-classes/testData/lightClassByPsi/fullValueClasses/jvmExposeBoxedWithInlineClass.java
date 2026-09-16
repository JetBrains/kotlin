@kotlin.jvm.JvmInline()
public final class InlineClass /* pack.InlineClass*/ {
  private final int value;

  @kotlin.jvm.JvmExposeBoxed()
  public  InlineClass(int);//  .ctor(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getValue();//  getValue()

  public int hashCode();//  hashCode()
}

public final class Regular /* pack.Regular*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() pack.ValueClass value;

  private final int inline;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.InlineClass getInline();//  getInline()

  @kotlin.jvm.JvmExposeBoxed()
  public  Regular(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass, @org.jetbrains.annotations.NotNull() pack.InlineClass);//  .ctor(@org.jetbrains.annotations.NotNull() pack.ValueClass, @org.jetbrains.annotations.NotNull() pack.InlineClass)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass getValue();//  getValue()

  private  Regular(@org.jetbrains.annotations.NotNull() pack.ValueClass, int);//  .ctor(@org.jetbrains.annotations.NotNull() pack.ValueClass, int)
}

public final class ValueClass /* pack.ValueClass*/ {
  private final int inline;

  private final int regular;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.InlineClass funWithInlineReturnType();//  funWithInlineReturnType()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.InlineClass getInline();//  getInline()

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueClass(@org.jetbrains.annotations.NotNull() pack.InlineClass, int);//  .ctor(@org.jetbrains.annotations.NotNull() pack.InlineClass, int)

  @kotlin.jvm.JvmExposeBoxed()
  public final void funWithInlineParameter(@org.jetbrains.annotations.NotNull() pack.InlineClass);//  funWithInlineParameter(@org.jetbrains.annotations.NotNull() pack.InlineClass)

  @kotlin.jvm.JvmExposeBoxed()
  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  public final int getRegular();//  getRegular()
}
