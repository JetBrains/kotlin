public abstract interface I /* I*/ {
  private abstract void privateMember();//  privateMember()

  private static void privateCompanionBlockMember();//  privateCompanionBlockMember()

  public abstract void publicMember();//  publicMember()

  public static void publicCompanionBlockMember();//  publicCompanionBlockMember()

  public static final class DefaultImpls /* I.DefaultImpls*/ {
    private static void privateMember(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() I);//  privateMember(@org.jetbrains.annotations.NotNull() I)

    public static void publicMember(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() I);//  publicMember(@org.jetbrains.annotations.NotNull() I)
  }
}
