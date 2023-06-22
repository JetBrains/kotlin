public abstract interface KtInterface /* KtInterface*/ {
  private int getProp();//  getProp()

  private void privateFun();//  privateFun()

  public abstract void defaultFun();//  defaultFun()

  public abstract void withoutBody();//  withoutBody()

  public static final class DefaultImpls /* KtInterface.DefaultImpls*/ {
    private static final int getProp(@org.jetbrains.annotations.NotNull() KtInterface);//  getProp(KtInterface)

    private static void privateFun(@org.jetbrains.annotations.NotNull() KtInterface);//  privateFun(KtInterface)

    public static void defaultFun(@org.jetbrains.annotations.NotNull() KtInterface);//  defaultFun(KtInterface)
  }
}
