public abstract interface I /* I*/<T>  {
  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() java.lang.String foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  foo(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()

  public abstract T abstractMember();//  abstractMember()

  public abstract T member(T);//  member(T)

  public static final class DefaultImpls /* I.DefaultImpls*/ {
    public static <T> T member(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() I<T>, T);// <T>  member(@org.jetbrains.annotations.NotNull() I<T>, T)
  }
}
