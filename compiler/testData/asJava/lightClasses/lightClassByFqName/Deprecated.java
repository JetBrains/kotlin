@kotlin.Deprecated(message = "deprecated class", replaceWith = @kotlin.ReplaceWith(expression = "new class", imports = {}), level = kotlin.DeprecationLevel.ERROR)
public final class DeprecatedClass /* foo.DeprecatedClass*/ {
  @org.jetbrains.annotations.NotNull()
  private java.lang.String deprecatedAccessors = "2" /* initializer type: java.lang.String */;

  @org.jetbrains.annotations.NotNull()
  private java.lang.String deprecatedErrorAccessors = "2" /* initializer type: java.lang.String */;

  private boolean deprecatedVariable = false /* initializer type: boolean */;

  private int deprecatedErrorVariable = 1 /* initializer type: int */;

  @kotlin.Deprecated(message = "deprecated function")
  public final void deprecatedFunction(int);//  deprecatedFunction(int)

  @kotlin.Deprecated(message = "deprecated getter")
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getDeprecatedAccessors();//  getDeprecatedAccessors()

  @kotlin.Deprecated(message = "deprecated setter")
  public final void setDeprecatedAccessors(@org.jetbrains.annotations.NotNull() java.lang.String);//  setDeprecatedAccessors(java.lang.String)

  @kotlin.Deprecated(message = "error function", replaceWith = @kotlin.ReplaceWith(expression = "function", imports = {}), level = kotlin.DeprecationLevel.ERROR)
  public final void deprecatedErrorFunction();//  deprecatedErrorFunction()

  @kotlin.Deprecated(message = "error getter", replaceWith = @kotlin.ReplaceWith(expression = "new getter", imports = {}), level = kotlin.DeprecationLevel.ERROR)
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getDeprecatedErrorAccessors();//  getDeprecatedErrorAccessors()

  @kotlin.Deprecated(message = "error setter", replaceWith = @kotlin.ReplaceWith(expression = "new setter", imports = {}), level = kotlin.DeprecationLevel.ERROR)
  public final void setDeprecatedErrorAccessors(@org.jetbrains.annotations.NotNull() java.lang.String);//  setDeprecatedErrorAccessors(java.lang.String)

  public  DeprecatedClass();//  .ctor()

  public final boolean getDeprecatedVariable();//  getDeprecatedVariable()

  public final int getDeprecatedErrorVariable();//  getDeprecatedErrorVariable()

  public final void setDeprecatedErrorVariable(int);//  setDeprecatedErrorVariable(int)

  public final void setDeprecatedVariable(boolean);//  setDeprecatedVariable(boolean)
}
