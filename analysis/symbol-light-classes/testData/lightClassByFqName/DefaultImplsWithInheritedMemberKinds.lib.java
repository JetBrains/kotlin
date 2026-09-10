public abstract interface B /* B*/ extends A {
  public static final class DefaultImpls /* B.DefaultImpls*/ {
    @kotlin.Deprecated(message = "")
    public static void deprecated(@org.jetbrains.annotations.NotNull() B);//  deprecated(B)

    @org.jetbrains.annotations.Nullable()
    public static java.lang.Object suspending(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super kotlin.Unit>);//  suspending(B, kotlin.coroutines.Continuation<? super kotlin.Unit>)

    public static <T> T generic(@org.jetbrains.annotations.NotNull() B, T);// <T>  generic(B, T)

    public static int getExtensionProperty(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() java.lang.String);//  getExtensionProperty(B, java.lang.String)

    public static int getMutable(@org.jetbrains.annotations.NotNull() B);//  getMutable(B)

    public static void extension(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() java.lang.String);//  extension(B, java.lang.String)

    public static void overridden(@org.jetbrains.annotations.NotNull() B);//  overridden(B)

    public static void setMutable(@org.jetbrains.annotations.NotNull() B, int);//  setMutable(B, int)

    public static void withDefaultArgument(@org.jetbrains.annotations.NotNull() B, int);//  withDefaultArgument(B, int)

    public static void withValueClass-YR_CVCc(@org.jetbrains.annotations.NotNull() B, int);//  withValueClass-YR_CVCc(B, int)
  }
}
