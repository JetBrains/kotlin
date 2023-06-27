public abstract interface KtInterface /* KtInterface*/ {
  private abstract int getPrivateProp();//  getPrivateProp()

  private abstract void privateFun();//  privateFun()

  public abstract int getDefaultProp();//  getDefaultProp()

  public abstract int getPropWithoutBody();//  getPropWithoutBody()

  public abstract void defaultFun();//  defaultFun()

  public abstract void withoutBody();//  withoutBody()

  public static final class DefaultImpls /* KtInterface.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static int getDefaultProp(@org.jetbrains.annotations.NotNull() KtInterface);//  getDefaultProp(KtInterface)

    @java.lang.Deprecated()
    public static void defaultFun(@org.jetbrains.annotations.NotNull() KtInterface);//  defaultFun(KtInterface)
  }
}
