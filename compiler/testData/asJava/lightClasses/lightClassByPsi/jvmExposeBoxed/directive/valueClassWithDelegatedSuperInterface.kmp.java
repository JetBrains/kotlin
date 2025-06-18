public abstract interface Interface /* pack.Interface*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() pack.ValueClassImpl getPropertyWithValueClass();//  getPropertyWithValueClass()

  @kotlin.jvm.JvmExposeBoxed()
  public abstract void functionWithValueParam(@org.jetbrains.annotations.NotNull() pack.ValueClassImpl);//  functionWithValueParam(@org.jetbrains.annotations.NotNull() pack.ValueClassImpl)

  public abstract int getRegularVariable();//  getRegularVariable()

  public abstract void regularFunction();//  regularFunction()

  public abstract void setRegularVariable(int);//  setRegularVariable(int)

  class DefaultImpls ...
}

@<error>()
public final class ValueClass /* pack.ValueClass*/ implements pack.Interface {
  private final int value;

  @java.lang.Override()
  @kotlin.jvm.JvmExposeBoxed()
  public void functionWithValueParam(@org.jetbrains.annotations.NotNull() pack.ValueClassImpl);//  functionWithValueParam(@org.jetbrains.annotations.NotNull() pack.ValueClassImpl)

  @java.lang.Override()
  public void regularFunction();//  regularFunction()

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

@<error>()
public final class ValueClassImpl /* pack.ValueClassImpl*/ implements pack.Interface {
  private final int int;

  @java.lang.Override()
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() pack.ValueClassImpl getPropertyWithValueClass();//  getPropertyWithValueClass()

  @java.lang.Override()
  @kotlin.jvm.JvmExposeBoxed()
  public void functionWithValueParam(@org.jetbrains.annotations.NotNull() pack.ValueClassImpl);//  functionWithValueParam(@org.jetbrains.annotations.NotNull() pack.ValueClassImpl)

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
