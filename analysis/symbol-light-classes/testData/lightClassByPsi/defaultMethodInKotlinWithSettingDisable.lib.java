public abstract interface KtInterface /* KtInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public static final KtInterface.Companion Companion;

  @kotlin.jvm.JvmStatic()
  public static void staticFun();//  staticFun()

  public abstract int getDefaultProperty();//  getDefaultProperty()

  public abstract int getPropertyWithoutBody();//  getPropertyWithoutBody()

  public abstract void defaultFun();//  defaultFun()

  public abstract void expressionBodyFun();//  expressionBodyFun()

  public abstract void withoutBody();//  withoutBody()

  public static int getStaticProperty();//  getStaticProperty()

  class Companion ...

  public static final class DefaultImpls /* KtInterface.DefaultImpls*/ {
    public static int getDefaultProperty(@org.jetbrains.annotations.NotNull() KtInterface);//  getDefaultProperty(KtInterface)

    public static void defaultFun(@org.jetbrains.annotations.NotNull() KtInterface);//  defaultFun(KtInterface)

    public static void expressionBodyFun(@org.jetbrains.annotations.NotNull() KtInterface);//  expressionBodyFun(KtInterface)
  }
}

public static final class Companion /* KtInterface.Companion*/ {
  private static final int staticProperty;

  @kotlin.jvm.JvmStatic()
  public final void staticFun();//  staticFun()

  private  Companion();//  .ctor()

  public final int getStaticProperty();//  getStaticProperty()
}
