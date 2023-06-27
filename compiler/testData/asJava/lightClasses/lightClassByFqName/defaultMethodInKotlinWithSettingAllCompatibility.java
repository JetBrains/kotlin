public abstract interface KtInterface /* KtInterface*/ {
  public abstract int getDefaultProp();//  getDefaultProp()

  public abstract int getPropWithoutBody();//  getPropWithoutBody()

  public abstract void defaultFun();//  defaultFun()

  public abstract void withoutBody();//  withoutBody()

  public static final class DefaultImpls /* KtInterface.DefaultImpls*/ {
    private static int getPrivateProp(@org.jetbrains.annotations.NotNull() KtInterface);//  getPrivateProp(KtInterface)

    private static void privateFun(@org.jetbrains.annotations.NotNull() KtInterface);//  privateFun(KtInterface)

    public static int getDefaultProp(@org.jetbrains.annotations.NotNull() KtInterface);//  getDefaultProp(KtInterface)

    public static void defaultFun(@org.jetbrains.annotations.NotNull() KtInterface);//  defaultFun(KtInterface)
  }
}