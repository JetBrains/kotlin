public abstract interface B /* B*/ extends Left, Right {
  public static final class DefaultImpls /* B.DefaultImpls*/ {
    @org.jetbrains.annotations.NotNull()
    public static java.lang.String generic(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() java.lang.String);//  generic(B, java.lang.String)

    public static void base(@org.jetbrains.annotations.NotNull() B);//  base(B)

    public static void overriddenInLeft(@org.jetbrains.annotations.NotNull() B);//  overriddenInLeft(B)
  }
}
