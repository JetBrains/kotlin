public abstract interface KtInterface /* KtInterface*/ {
  private abstract int getProp();//  getProp()

  private abstract void privateFun();//  privateFun()

  public abstract void defaultFun();//  defaultFun()

  public abstract void withoutBody();//  withoutBody()

  public static final class DefaultImpls /* KtInterface.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static void defaultFun(@org.jetbrains.annotations.NotNull() KtInterface);//  defaultFun(KtInterface)
  }
}
