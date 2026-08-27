public final class IntroducedAfterBase /* IntroducedAfterBase*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() int value;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String text;

  @<error>()
  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  public  IntroducedAfterBase(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  private  IntroducedAfterBase(@org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "3") @org.jetbrains.annotations.NotNull() int);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() int)

  @<error>()
  public  IntroducedAfterBase();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getText();//  getText()
}

public final class IntroducedOnly /* IntroducedOnly*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() int value;

  @<error>()
  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  public  IntroducedOnly();//  .ctor()

  @<error>()
  private  IntroducedOnly(@kotlin.IntroducedAt(version = "3") @org.jetbrains.annotations.NotNull() int);//  .ctor(@org.jetbrains.annotations.NotNull() int)
}
