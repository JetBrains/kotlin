@kotlin.jvm.JvmInline()
public final class InlineClass /* pack.InlineClass*/ {
  private final int value;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  InlineClass(int);//  .ctor(int)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public final int getValue();//  getValue()

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(int, java.lang.Object);//  equals-impl(int, java.lang.Object)

  public static final boolean equals-impl0(int, int);//  equals-impl0(int, int)

  public static int constructor-impl(int);//  constructor-impl(int)

  public static int hashCode-impl(int);//  hashCode-impl(int)

  public static java.lang.String toString-impl(int);//  toString-impl(int)
}

public final class Regular /* pack.Regular*/ {
  @org.jetbrains.annotations.NotNull()
  private final pack.ValueClass value;

  private final int inline;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final pack.InlineClass getInline();//  getInline()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  Regular(@org.jetbrains.annotations.NotNull() pack.ValueClass, @org.jetbrains.annotations.NotNull() pack.InlineClass);//  .ctor(pack.ValueClass, pack.InlineClass)

  @org.jetbrains.annotations.NotNull()
  public final pack.ValueClass getValue();//  getValue()

  private  Regular(pack.ValueClass, int);//  .ctor(pack.ValueClass, int)

  public final int getInline-Ww3kNBE();//  getInline-Ww3kNBE()
}

public final class ValueClass /* pack.ValueClass*/ {
  private final int inline;

  private final int regular;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final pack.InlineClass funWithInlineReturnType();//  funWithInlineReturnType()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final pack.InlineClass getInline();//  getInline()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  ValueClass(@org.jetbrains.annotations.NotNull() pack.InlineClass, int);//  .ctor(pack.InlineClass, int)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public final void funWithInlineParameter(@org.jetbrains.annotations.NotNull() pack.InlineClass);//  funWithInlineParameter(pack.InlineClass)

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  private  ValueClass(int, int);//  .ctor(int, int)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final int funWithInlineReturnType-Ww3kNBE();//  funWithInlineReturnType-Ww3kNBE()

  public final int getInline-Ww3kNBE();//  getInline-Ww3kNBE()

  public final int getRegular();//  getRegular()

  public final void funWithInlineParameter-_buEuXY(int);//  funWithInlineParameter-_buEuXY(int)

  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  funWithSelfParameter(pack.ValueClass)

  public int hashCode();//  hashCode()
}
