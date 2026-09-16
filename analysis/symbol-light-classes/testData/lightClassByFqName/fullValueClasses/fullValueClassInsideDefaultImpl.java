public abstract interface BaseInterface /* one.BaseInterface*/ {
  @org.jetbrains.annotations.Nullable()
  public default @org.jetbrains.annotations.Nullable() one.MyValueClass getPropertyWithValueClassParameter();//  getPropertyWithValueClassParameter()

  public default void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.MyValueClass);//  functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.MyValueClass)

  public default void regularFunction();//  regularFunction()

  public static final class DefaultImpls /* one.BaseInterface.DefaultImpls*/ {
    @org.jetbrains.annotations.Nullable()
    public static @org.jetbrains.annotations.Nullable() one.MyValueClass getPropertyWithValueClassParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.BaseInterface);//  getPropertyWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.BaseInterface)

    public static void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.BaseInterface, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.MyValueClass);//  functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.BaseInterface, @org.jetbrains.annotations.NotNull() one.MyValueClass)

    public static void regularFunction(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.BaseInterface);//  regularFunction(@org.jetbrains.annotations.NotNull() one.BaseInterface)
  }
}
