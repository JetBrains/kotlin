public abstract interface KtInterface /* KtInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public static final KtInterface.Companion Companion;

  @kotlin.jvm.JvmStatic()
  public static void staticFun();//  staticFun()

  public default void defaultFun();//  defaultFun()

  public static int getStaticProperty();//  getStaticProperty()

  class Companion ...

  public static final class DefaultImpls /* KtInterface.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static void defaultFun(@org.jetbrains.annotations.NotNull() KtInterface);//  defaultFun(KtInterface)
  }
}

public static final class Companion /* KtInterface.Companion*/ {
  private static final int staticProperty;

  @kotlin.jvm.JvmStatic()
  public final void staticFun();//  staticFun()

  private  Companion();//  .ctor()

  public final int getStaticProperty();//  getStaticProperty()

  public final void companionFun();//  companionFun()
}
