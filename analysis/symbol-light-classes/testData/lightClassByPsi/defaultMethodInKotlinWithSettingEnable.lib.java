public abstract interface KtInterface /* KtInterface*/ {
  public abstract void withoutBody();//  withoutBody()

  public default void defaultFun();//  defaultFun()

  public static final class DefaultImpls /* KtInterface.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static void defaultFun(@org.jetbrains.annotations.NotNull() KtInterface);//  defaultFun(KtInterface)
  }
}
