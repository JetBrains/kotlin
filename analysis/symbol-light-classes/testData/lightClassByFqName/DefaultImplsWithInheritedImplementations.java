public abstract interface B /* B*/ extends A, Generic<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  public abstract void overriddenAsAbstract();//  overriddenAsAbstract()

  @java.lang.Override()
  public abstract void overriddenWithBody();//  overriddenWithBody()

  public abstract void b();//  b()

  public static final class DefaultImpls /* B.DefaultImpls*/ {
    @java.lang.Override()
    public static void overriddenWithBody(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  overriddenWithBody(@org.jetbrains.annotations.NotNull() B)

    public static void b(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  b(@org.jetbrains.annotations.NotNull() B)
  }
}
