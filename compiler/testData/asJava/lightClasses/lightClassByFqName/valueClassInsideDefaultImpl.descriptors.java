public abstract interface BaseInterface /* one.BaseInterface*/ {
  @org.jetbrains.annotations.Nullable()
  public abstract java.lang.String getPropertyWithValueClassParameter();//  getPropertyWithValueClassParameter()

  public abstract void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() java.lang.String);//  functionWithValueClassParameter(java.lang.String)

  public abstract void regularFunction();//  regularFunction()

  public static final class DefaultImpls /* one.BaseInterface.DefaultImpls*/ {
    @org.jetbrains.annotations.Nullable()
    public static java.lang.String getPropertyWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.BaseInterface);//  getPropertyWithValueClassParameter(one.BaseInterface)

    public static void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.BaseInterface, @org.jetbrains.annotations.NotNull() java.lang.String);//  functionWithValueClassParameter(one.BaseInterface, java.lang.String)

    public static void regularFunction(@org.jetbrains.annotations.NotNull() one.BaseInterface);//  regularFunction(one.BaseInterface)
  }
}