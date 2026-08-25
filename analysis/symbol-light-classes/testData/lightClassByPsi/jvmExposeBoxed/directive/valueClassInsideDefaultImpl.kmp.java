public abstract interface BaseInterface /* one.BaseInterface*/ {
  @org.jetbrains.annotations.Nullable()
  public abstract @org.jetbrains.annotations.Nullable() java.lang.String getPropertyWithValueClassParameter();//  getPropertyWithValueClassParameter()

  public abstract void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract void regularFunction();//  regularFunction()

  public static final class DefaultImpls /* one.BaseInterface.DefaultImpls*/ {
    @org.jetbrains.annotations.Nullable()
    public static @org.jetbrains.annotations.Nullable() java.lang.String getPropertyWithValueClassParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.BaseInterface);//  getPropertyWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.BaseInterface)

    public static void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.BaseInterface, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.BaseInterface, @org.jetbrains.annotations.NotNull() java.lang.String)

    public static void regularFunction(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.BaseInterface);//  regularFunction(@org.jetbrains.annotations.NotNull() one.BaseInterface)
  }
}

@<error>()
public final class MyValueClass /* one.MyValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String str;

  @kotlin.jvm.JvmExposeBoxed()
  public  MyValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getStr();//  getStr()
}
