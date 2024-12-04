public abstract interface TestInterface /* test.pkg.TestInterface*/ {
  @kotlin.Deprecated()
  public abstract int getPOld_deprecatedOnGetter();//  getPOld_deprecatedOnGetter()

  @kotlin.Deprecated()
  public abstract void setPOld_deprecatedOnSetter(int);//  setPOld_deprecatedOnSetter(int)

  public abstract int getPNew();//  getPNew()

  public abstract int getPOld_deprecatedOnProperty();//  getPOld_deprecatedOnProperty()

  public abstract int getPOld_deprecatedOnSetter();//  getPOld_deprecatedOnSetter()

  public abstract void setPNew(int);//  setPNew(int)

  public abstract void setPOld_deprecatedOnGetter(int);//  setPOld_deprecatedOnGetter(int)

  public abstract void setPOld_deprecatedOnProperty(int);//  setPOld_deprecatedOnProperty(int)

  class DefaultImpls ...
}
