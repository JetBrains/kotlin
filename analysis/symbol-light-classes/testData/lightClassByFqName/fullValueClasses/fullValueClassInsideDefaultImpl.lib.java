public abstract interface BaseInterface /* one.BaseInterface*/ {
  @org.jetbrains.annotations.Nullable()
  public abstract one.MyValueClass getPropertyWithValueClassParameter();//  getPropertyWithValueClassParameter()

  public abstract void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.MyValueClass);//  functionWithValueClassParameter(one.MyValueClass)

  public abstract void regularFunction();//  regularFunction()

  public static final class DefaultImpls /* one.BaseInterface.DefaultImpls*/ {
    @java.lang.Deprecated()
    @org.jetbrains.annotations.Nullable()
    public static one.MyValueClass getPropertyWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.BaseInterface);//  getPropertyWithValueClassParameter(one.BaseInterface)

    @java.lang.Deprecated()
    public static void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.BaseInterface, @org.jetbrains.annotations.NotNull() one.MyValueClass);//  functionWithValueClassParameter(one.BaseInterface, one.MyValueClass)

    @java.lang.Deprecated()
    public static void regularFunction(@org.jetbrains.annotations.NotNull() one.BaseInterface);//  regularFunction(one.BaseInterface)
  }
}
