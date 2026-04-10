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
  private final int propertyWithValueClass;

  private final int value;

  private int regularVariable;
}

@kotlin.jvm.JvmInline()
public final class ValueClassImpl /* pack.ValueClassImpl*/ implements pack.Interface {
  private final int int;
}
