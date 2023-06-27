public abstract interface KtInterface /* KtInterface*/ {
  private int getPrivateProp();//  getPrivateProp()

  private void privateFun();//  privateFun()

  public abstract int getDefaultProp();//  getDefaultProp()

  public abstract int getPropWithoutBody();//  getPropWithoutBody()

  public abstract void defaultFun();//  defaultFun()

  public abstract void withoutBody();//  withoutBody()

  class DefaultImpls ...
}
