public abstract interface Foo /* Foo*/ {
  public abstract void bar();//  bar()

  public static final class DefaultImpls /* Foo.DefaultImpls*/ {
    private static int getPrivateProperty(Foo);//  getPrivateProperty(Foo)

    private static void foo(Foo);//  foo(Foo)

    private static void setPrivateProperty(Foo, int);//  setPrivateProperty(Foo, int)

    public static void bar(@org.jetbrains.annotations.NotNull() Foo);//  bar(Foo)
  }
}
