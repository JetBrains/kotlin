public abstract interface Foo /* Foo*/ {
  private default int getPrivateProperty();//  getPrivateProperty()

  private default void foo();//  foo()

  private default void setPrivateProperty(int);//  setPrivateProperty(int)

  public default void bar();//  bar()

  public static final class DefaultImpls /* Foo.DefaultImpls*/ {
    public static void bar(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Foo);//  bar(@org.jetbrains.annotations.NotNull() Foo)
  }
}
