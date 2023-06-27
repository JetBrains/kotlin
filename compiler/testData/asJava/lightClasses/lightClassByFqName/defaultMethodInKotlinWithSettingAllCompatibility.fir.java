public abstract interface KtInterface /* KtInterface*/ {
  private int getPrivateProp();//  getPrivateProp()

  private void privateFun();//  privateFun()

  public abstract int getDefaultProp();//  getDefaultProp()

  public abstract int getPropWithoutBody();//  getPropWithoutBody()

  public abstract void defaultFun();//  defaultFun()

  public abstract void withoutBody();//  withoutBody()

  public static final class DefaultImpls /* KtInterface.DefaultImpls*/ {
    public static int getDefaultProp(@org.jetbrains.annotations.NotNull() KtInterface);//  getDefaultProp(KtInterface)

    public static void defaultFun(@org.jetbrains.annotations.NotNull() KtInterface);//  defaultFun(KtInterface)
  }
}
