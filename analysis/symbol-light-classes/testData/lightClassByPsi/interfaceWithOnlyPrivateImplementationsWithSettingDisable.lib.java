public abstract interface OnlyPrivateImplementations /* OnlyPrivateImplementations*/ {
  public abstract void baz();//  baz()

  public static final class DefaultImpls /* OnlyPrivateImplementations.DefaultImpls*/ {
    private static int getBar(OnlyPrivateImplementations);//  getBar(OnlyPrivateImplementations)

    private static void foo(OnlyPrivateImplementations);//  foo(OnlyPrivateImplementations)
  }
}
