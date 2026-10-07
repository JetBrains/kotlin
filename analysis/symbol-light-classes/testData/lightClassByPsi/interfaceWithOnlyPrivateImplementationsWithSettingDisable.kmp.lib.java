public abstract interface OnlyPrivateImplementations /* OnlyPrivateImplementations*/ {
  private default int getBar();//  getBar()

  private default void foo();//  foo()

  public abstract void baz();//  baz()

  public static final class DefaultImpls /* OnlyPrivateImplementations.DefaultImpls*/ {
  }
}
