public abstract interface Foo /* Foo*/<X, Y>  {
  public abstract <Z> void foo(X, Y, Z);// <Z>  foo(X, Y, Z)

  public abstract int getX();//  getX()

  public static final class DefaultImpls /* Foo.DefaultImpls*/ {
    public static <Z> void foo(@org.jetbrains.annotations.NotNull() Foo, X, Y, Z);// <Z>  foo(Foo, X, Y, Z)

    public static int getX(@org.jetbrains.annotations.NotNull() Foo);//  getX(Foo)
  }
}