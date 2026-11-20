public abstract interface I /* I*/ {
  private static void privateCompanionBlockMember();//  privateCompanionBlockMember()

  public abstract void publicMember();//  publicMember()

  public static void publicCompanionBlockMember();//  publicCompanionBlockMember()

  public static final class DefaultImpls /* I.DefaultImpls*/ {
    private static void privateMember(I);//  privateMember(I)

    public static void publicMember(@org.jetbrains.annotations.NotNull() I);//  publicMember(I)
  }
}
