public abstract interface B /* B*/ extends Disabled, Enabled, SameModule {
  public default void declared();//  declared()

  public static final class DefaultImpls /* B.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static void declared(@org.jetbrains.annotations.NotNull() B);//  declared(B)

    @java.lang.Deprecated()
    public static void fromEnabled(@org.jetbrains.annotations.NotNull() B);//  fromEnabled(B)

    @java.lang.Deprecated()
    public static void fromSameModule(@org.jetbrains.annotations.NotNull() B);//  fromSameModule(B)

    public static void fromDisabled(@org.jetbrains.annotations.NotNull() B);//  fromDisabled(B)
  }
}
