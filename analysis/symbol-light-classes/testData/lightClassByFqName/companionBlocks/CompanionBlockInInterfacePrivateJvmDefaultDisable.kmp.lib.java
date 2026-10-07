public abstract interface I /* I*/ {
  private default void privateMember();//  privateMember()

  private static void privateCompanionBlockMember();//  privateCompanionBlockMember()

  public default void publicMember();//  publicMember()

  public static void publicCompanionBlockMember();//  publicCompanionBlockMember()

  public static final class DefaultImpls /* I.DefaultImpls*/ {
    public static void publicMember(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() I);//  publicMember(@org.jetbrains.annotations.NotNull() I)
  }
}
