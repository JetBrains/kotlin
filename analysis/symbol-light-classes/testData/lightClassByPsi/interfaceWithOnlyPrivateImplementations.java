public abstract interface OnlyPrivateImplementations /* OnlyPrivateImplementations*/ {
  private default int getBar();//  getBar()

  private default void foo();//  foo()

  public abstract void baz();//  baz()

  public static final class DefaultImpls /* OnlyPrivateImplementations.DefaultImpls*/ {
    private static final int getBar(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() OnlyPrivateImplementations);//  getBar(@org.jetbrains.annotations.NotNull() OnlyPrivateImplementations)

    private static void foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() OnlyPrivateImplementations);//  foo(@org.jetbrains.annotations.NotNull() OnlyPrivateImplementations)
  }
}
