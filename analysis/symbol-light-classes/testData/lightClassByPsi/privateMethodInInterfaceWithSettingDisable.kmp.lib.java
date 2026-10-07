public abstract interface Foo /* Foo*/ {
  private default int getPrivateProperty();//  getPrivateProperty()

  private default void foo();//  foo()

  private default void setPrivateProperty(int);//  setPrivateProperty(int)

  public default void bar();//  bar()

  public static final class DefaultImpls /* Foo.DefaultImpls*/ {
    private static final int getPrivateProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Foo);//  getPrivateProperty(@org.jetbrains.annotations.NotNull() Foo)

    private static final void setPrivateProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Foo, int);//  setPrivateProperty(@org.jetbrains.annotations.NotNull() Foo, int)

    private static void foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Foo);//  foo(@org.jetbrains.annotations.NotNull() Foo)

    public static void bar(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Foo);//  bar(@org.jetbrains.annotations.NotNull() Foo)
  }
}
