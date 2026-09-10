public abstract interface B /* B*/ extends Disabled, Enabled, SameModule {
  public default void declared();//  declared()

  public static final class DefaultImpls /* B.DefaultImpls*/ {
    public static void fromDisabled(@org.jetbrains.annotations.NotNull() B);//  fromDisabled(B)
  }
}
