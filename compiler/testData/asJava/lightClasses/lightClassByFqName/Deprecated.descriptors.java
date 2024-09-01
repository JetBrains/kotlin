@kotlin.Deprecated(message = "deprecated class", replaceWith = ReplaceWith("new class"), level = kotlin.DeprecationLevel.ERROR)
public final class DeprecatedClass /* foo.DeprecatedClass*/ {
  @org.jetbrains.annotations.NotNull()
  private java.lang.String deprecatedAccessors;

  @org.jetbrains.annotations.NotNull()
  private java.lang.String deprecatedErrorAccessors;

  private boolean deprecatedVariable;

  private int deprecatedErrorVariable;

  @kotlin.Deprecated(message = "deprecated function")
  public final void deprecatedFunction(int);//  deprecatedFunction(int)

  @kotlin.Deprecated(message = "deprecated getter")
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getDeprecatedAccessors();//  getDeprecatedAccessors()

  @kotlin.Deprecated(message = "deprecated setter")
  public final void setDeprecatedAccessors(@org.jetbrains.annotations.NotNull() java.lang.String);//  setDeprecatedAccessors(java.lang.String)

  @kotlin.Deprecated(message = "error function", replaceWith = ReplaceWith("function"), level = kotlin.DeprecationLevel.ERROR)
  public final void deprecatedErrorFunction();//  deprecatedErrorFunction()

  @kotlin.Deprecated(message = "error getter", replaceWith = ReplaceWith("new getter"), level = kotlin.DeprecationLevel.ERROR)
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getDeprecatedErrorAccessors();//  getDeprecatedErrorAccessors()

  @kotlin.Deprecated(message = "error setter", replaceWith = ReplaceWith("new setter"), level = kotlin.DeprecationLevel.ERROR)
  public final void setDeprecatedErrorAccessors(@org.jetbrains.annotations.NotNull() java.lang.String);//  setDeprecatedErrorAccessors(java.lang.String)

  public  DeprecatedClass();//  .ctor()

  public final boolean getDeprecatedVariable();//  getDeprecatedVariable()

  public final int getDeprecatedErrorVariable();//  getDeprecatedErrorVariable()

  public final void setDeprecatedErrorVariable(int);//  setDeprecatedErrorVariable(int)

  public final void setDeprecatedVariable(boolean);//  setDeprecatedVariable(boolean)
}