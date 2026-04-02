public abstract interface Interface /* pack.Interface*/ {
  public abstract int getRegularVariable();//  getRegularVariable()

  public abstract void regularFunction();//  regularFunction()

  public abstract void setRegularVariable(int);//  setRegularVariable(int)

  public static final class DefaultImpls /* pack.Interface.DefaultImpls*/ {
    public static void regularFunction(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.Interface);//  regularFunction(@org.jetbrains.annotations.NotNull() pack.Interface)
  }
}

@kotlin.jvm.JvmInline()
public final class ValueClass /* pack.ValueClass*/ implements pack.Interface {
  private final int value;

  @java.lang.Override()
  public int getRegularVariable();//  getRegularVariable()

  @java.lang.Override()
  public void regularFunction();//  regularFunction()

  @java.lang.Override()
  public void setRegularVariable(int);//  setRegularVariable(int)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClassImpl getValue();//  getValue()

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueClass(@org.jetbrains.annotations.NotNull() pack.ValueClassImpl);//  .ctor(@org.jetbrains.annotations.NotNull() pack.ValueClassImpl)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

@kotlin.jvm.JvmInline()
public final class ValueClassImpl /* pack.ValueClassImpl*/ implements pack.Interface {
  private final int int;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @java.lang.Override()
  public int getRegularVariable();//  getRegularVariable()

  @java.lang.Override()
  public void regularFunction();//  regularFunction()

  @java.lang.Override()
  public void setRegularVariable(int);//  setRegularVariable(int)

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueClassImpl(int);//  .ctor(int)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getInt();//  getInt()

  public int hashCode();//  hashCode()
}
