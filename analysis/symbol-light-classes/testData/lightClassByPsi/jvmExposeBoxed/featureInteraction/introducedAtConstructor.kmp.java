public final class IntroducedAfterBase /* IntroducedAfterBase*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() int value;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String text;

  @<error>()
  private  IntroducedAfterBase(@org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "3") @org.jetbrains.annotations.NotNull() int);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() int)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getText();//  getText()
}

public final class IntroducedOnly /* IntroducedOnly*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() int value;

  @<error>()
  private  IntroducedOnly(@kotlin.IntroducedAt(version = "3") @org.jetbrains.annotations.NotNull() int);//  .ctor(@org.jetbrains.annotations.NotNull() int)
}
