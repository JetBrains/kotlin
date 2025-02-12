public abstract interface C /* p.C*/<T>  extends p.B {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String c();//  c()

  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String more();//  more()

  public abstract int getProp3();//  getProp3()

  public abstract void setProp3(int);//  setProp3(int)

  public static final class DefaultImpls /* p.C.DefaultImpls*/ {
    @org.jetbrains.annotations.NotNull()
    public static <T> java.lang.String a(@org.jetbrains.annotations.NotNull() p.C<T>);// <T>  a(p.C<T>)

    @org.jetbrains.annotations.NotNull()
    public static <T> java.lang.String b(@org.jetbrains.annotations.NotNull() p.C<T>);// <T>  b(p.C<T>)

    @org.jetbrains.annotations.NotNull()
    public static <T> java.lang.String c(@org.jetbrains.annotations.NotNull() p.C<T>);// <T>  c(p.C<T>)

    public static <T> int getProp1(@org.jetbrains.annotations.NotNull() p.C<T>);// <T>  getProp1(p.C<T>)

    public static <T> int getProp2(@org.jetbrains.annotations.NotNull() p.C<T>);// <T>  getProp2(p.C<T>)

    public static <T> int getProp3(@org.jetbrains.annotations.NotNull() p.C<T>);// <T>  getProp3(p.C<T>)

    public static <T> void setProp1(@org.jetbrains.annotations.NotNull() p.C<T>, int);// <T>  setProp1(p.C<T>, int)

    public static <T> void setProp2(@org.jetbrains.annotations.NotNull() p.C<T>, int);// <T>  setProp2(p.C<T>, int)

    public static <T> void setProp3(@org.jetbrains.annotations.NotNull() p.C<T>, int);// <T>  setProp3(p.C<T>, int)
  }
}
