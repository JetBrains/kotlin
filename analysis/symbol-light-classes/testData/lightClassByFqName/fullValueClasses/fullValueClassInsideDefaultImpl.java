public abstract interface BaseInterface /* one.BaseInterface*/ {
  @org.jetbrains.annotations.Nullable()
  public abstract @org.jetbrains.annotations.Nullable() one.MyValueClass getPropertyWithValueClassParameter();//  getPropertyWithValueClassParameter()

  public abstract void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.MyValueClass);//  functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.MyValueClass)

  public abstract void regularFunction();//  regularFunction()

  public static final class DefaultImpls /* one.BaseInterface.DefaultImpls*/ {
    @org.jetbrains.annotations.Nullable()
    public static @org.jetbrains.annotations.Nullable() one.MyValueClass getPropertyWithValueClassParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.BaseInterface);//  getPropertyWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.BaseInterface)

    public static void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.BaseInterface, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.MyValueClass);//  functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.BaseInterface, @org.jetbrains.annotations.NotNull() one.MyValueClass)

    public static void regularFunction(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.BaseInterface);//  regularFunction(@org.jetbrains.annotations.NotNull() one.BaseInterface)
  }
}
