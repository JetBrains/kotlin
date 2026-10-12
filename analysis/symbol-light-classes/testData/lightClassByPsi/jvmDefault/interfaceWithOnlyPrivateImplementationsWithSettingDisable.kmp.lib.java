public abstract interface OnlyPrivateImplementations /* OnlyPrivateImplementations*/ {
  private default int getBar();//  getBar()

  private default void foo();//  foo()

  public abstract void baz();//  baz()
}
