public abstract interface Interface /* pack.Interface*/ {
  public abstract int getPropertyWithValueClass();//  getPropertyWithValueClass()

  public abstract int getRegularVariable();//  getRegularVariable()

  public abstract void functionWithValueParam(int);//  functionWithValueParam(int)

  public abstract void regularFunction();//  regularFunction()

  public abstract void setRegularVariable(int);//  setRegularVariable(int)

  public static final class DefaultImpls /* pack.Interface.DefaultImpls*/ {
    public static void regularFunction(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.Interface);//  regularFunction(@org.jetbrains.annotations.NotNull() pack.Interface)
  }
}

@<error>()
public final class ValueClass /* pack.ValueClass*/ implements pack.Interface {
  private final int int;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @java.lang.Override()
  public int getPropertyWithValueClass();//  getPropertyWithValueClass()

  @java.lang.Override()
  public int getRegularVariable();//  getRegularVariable()

  @java.lang.Override()
  public void functionWithValueParam(int);//  functionWithValueParam(int)

  @java.lang.Override()
  public void regularFunction();//  regularFunction()

  @java.lang.Override()
  public void setRegularVariable(int);//  setRegularVariable(int)

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueClass(int);//  .ctor(int)

  public final int getInt();//  getInt()
}
