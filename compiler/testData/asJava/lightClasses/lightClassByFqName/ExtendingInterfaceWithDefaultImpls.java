public abstract interface C /* p.C*/<T>  extends p.B {
  @java.lang.Deprecated()
  @p.Anno()
  public static void getProp3$annotations();//  getProp3$annotations()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String c();//  c()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String more();//  more()

  public abstract int getProp3();//  getProp3()

  public abstract void setProp3(int);//  setProp3(int)

  public static final class DefaultImpls /* p.C.DefaultImpls*/ {
    @java.lang.Deprecated()
    @p.Anno()
    public static void getProp3$annotations();//  getProp3$annotations()

    @org.jetbrains.annotations.NotNull()
    public static <T> @org.jetbrains.annotations.NotNull() java.lang.String c(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() p.C<T>);// <T>  c(@org.jetbrains.annotations.NotNull() p.C<T>)

    public static <T> int getProp3(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() p.C<T>);// <T>  getProp3(@org.jetbrains.annotations.NotNull() p.C<T>)

    public static <T> void setProp3(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() p.C<T>, int);// <T>  setProp3(@org.jetbrains.annotations.NotNull() p.C<T>, int)
  }
}
