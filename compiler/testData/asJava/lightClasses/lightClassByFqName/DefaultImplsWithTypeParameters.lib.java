public abstract interface Foo /* Foo*/<X, Y>  {
  public abstract <Z> void foo(X, Y, Z);// <Z>  foo(X, Y, Z)

  public abstract int getX();//  getX()

  public static final class DefaultImpls /* Foo.DefaultImpls*/ {
    public static <X, Y, Z> void foo(@org.jetbrains.annotations.NotNull() Foo<X, Y>, X, Y, Z);// <X, Y, Z>  foo(Foo<X, Y>, X, Y, Z)

    public static <X, Y> int getX(@org.jetbrains.annotations.NotNull() Foo<X, Y>);// <X, Y>  getX(Foo<X, Y>)
  }
}
