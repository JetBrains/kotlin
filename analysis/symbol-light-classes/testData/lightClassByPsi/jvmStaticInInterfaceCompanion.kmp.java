public abstract interface KtInterface /* KtInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() KtInterface.Companion Companion;

  public default void defaultFun();//  defaultFun()

  class Companion ...

  public static final class DefaultImpls /* KtInterface.DefaultImpls*/ {
    public static void defaultFun(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() KtInterface);//  defaultFun(@org.jetbrains.annotations.NotNull() KtInterface)
  }
}

public static final class Companion /* KtInterface.Companion*/ {
  private static final int staticProperty = 1 /* initializer type: int */;

  private  Companion();//  .ctor()

  public final int getStaticProperty();//  getStaticProperty()

  public final void companionFun();//  companionFun()

  public final void staticFun();//  staticFun()
}
