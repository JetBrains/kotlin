public abstract interface KtInterface /* KtInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() KtInterface.Companion Companion;

  @kotlin.jvm.JvmStatic()
  public static void staticFun();//  staticFun()

  public default void defaultFun();//  defaultFun()

  public static int getStaticProperty();//  getStaticProperty()

  class Companion ...

  public static final class DefaultImpls /* KtInterface.DefaultImpls*/ {
    @kotlin.jvm.JvmStatic()
    public static void staticFun(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() KtInterface);//  staticFun(@org.jetbrains.annotations.NotNull() KtInterface)

    public static final int getStaticProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() KtInterface);//  getStaticProperty(@org.jetbrains.annotations.NotNull() KtInterface)

    public static void defaultFun(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() KtInterface);//  defaultFun(@org.jetbrains.annotations.NotNull() KtInterface)
  }
}

public static final class Companion /* KtInterface.Companion*/ {
  private static final int staticProperty = 1 /* initializer type: int */;

  @kotlin.jvm.JvmStatic()
  public final void staticFun();//  staticFun()

  private  Companion();//  .ctor()

  public final int getStaticProperty();//  getStaticProperty()

  public final void companionFun();//  companionFun()
}
