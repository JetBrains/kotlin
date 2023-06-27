public abstract interface KtInterface /* KtInterface*/ {
  private abstract int getPrivateProp();//  getPrivateProp()

  private abstract void privateFun();//  privateFun()

  public abstract int getDefaultProp();//  getDefaultProp()

  public abstract int getPropWithoutBody();//  getPropWithoutBody()

  public abstract void defaultFun();//  defaultFun()

  public abstract void withoutBody();//  withoutBody()

  class DefaultImpls ...
}
