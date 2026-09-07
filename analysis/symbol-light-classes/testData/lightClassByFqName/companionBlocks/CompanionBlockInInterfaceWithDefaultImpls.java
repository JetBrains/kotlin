public abstract interface I /* I*/<T>  {
  public abstract T abstractMember();//  abstractMember()

  public abstract T member(T);//  member(T)

  public static final class DefaultImpls /* I.DefaultImpls*/ {
    public static <T> T member(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() I<T>, T);// <T>  member(@org.jetbrains.annotations.NotNull() I<T>, T)
  }
}
