public abstract interface B /* B*/ extends A, Generic<java.lang.String> {
  public abstract void b();//  b()

  public abstract void overriddenAsAbstract();//  overriddenAsAbstract()

  public abstract void overriddenWithBody();//  overriddenWithBody()

  public static final class DefaultImpls /* B.DefaultImpls*/ {
    @org.jetbrains.annotations.NotNull()
    public static java.lang.String generic(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() java.lang.String);//  generic(B, java.lang.String)

    public static int getProp(@org.jetbrains.annotations.NotNull() B);//  getProp(B)

    public static void a(@org.jetbrains.annotations.NotNull() B);//  a(B)

    public static void b(@org.jetbrains.annotations.NotNull() B);//  b(B)

    public static void overriddenWithBody(@org.jetbrains.annotations.NotNull() B);//  overriddenWithBody(B)
  }
}
