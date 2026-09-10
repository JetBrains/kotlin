public abstract interface B /* B*/ extends Disabled, Enabled, SameModule {
  public abstract void declared();//  declared()

  public static final class DefaultImpls /* B.DefaultImpls*/ {
    public static void declared(@org.jetbrains.annotations.NotNull() B);//  declared(B)

    public static void fromDisabled(@org.jetbrains.annotations.NotNull() B);//  fromDisabled(B)

    public static void fromSameModule(@org.jetbrains.annotations.NotNull() B);//  fromSameModule(B)
  }
}
