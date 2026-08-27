public class Base /* Base*/ {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String overridden(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String);//  overridden(java.lang.String, java.lang.String)

  public  Base();//  .ctor()
}

public final class FinalOverride /* FinalOverride*/ extends Base {
  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  @org.jetbrains.annotations.NotNull()
  public java.lang.String overridden(@org.jetbrains.annotations.NotNull() java.lang.String);//  overridden(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public java.lang.String overridden(@org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "1") @org.jetbrains.annotations.NotNull() java.lang.String);//  overridden(java.lang.String, java.lang.String)

  public  FinalOverride();//  .ctor()
}
