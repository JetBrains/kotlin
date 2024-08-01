public final class DeprecatedKt /* foo.DeprecatedKt*/ {
  @org.jetbrains.annotations.NotNull()
  private static java.lang.String deprecatedAccessors = "2" /* initializer type: java.lang.String */;

  @org.jetbrains.annotations.NotNull()
  private static java.lang.String deprecatedErrorAccessors = "2" /* initializer type: java.lang.String */;

  private static boolean deprecatedVariable = false /* initializer type: boolean */;

  private static int deprecatedErrorVariable = 1 /* initializer type: int */;

  @kotlin.Deprecated(message = "deprecated function")
  public static final void deprecatedFunction(int);//  deprecatedFunction(int)

  @kotlin.Deprecated(message = "deprecated getter")
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String getDeprecatedAccessors();//  getDeprecatedAccessors()

  @kotlin.Deprecated(message = "deprecated setter")
  public static final void setDeprecatedAccessors(@org.jetbrains.annotations.NotNull() java.lang.String);//  setDeprecatedAccessors(java.lang.String)

  @kotlin.Deprecated(message = "error function", replaceWith = @kotlin.ReplaceWith(expression = "function", imports = {}), level = kotlin.DeprecationLevel.ERROR)
  public static final void deprecatedErrorFunction();//  deprecatedErrorFunction()

  @kotlin.Deprecated(message = "error getter", replaceWith = @kotlin.ReplaceWith(expression = "new getter", imports = {}), level = kotlin.DeprecationLevel.ERROR)
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String getDeprecatedErrorAccessors();//  getDeprecatedErrorAccessors()

  @kotlin.Deprecated(message = "error setter", replaceWith = @kotlin.ReplaceWith(expression = "new setter", imports = {}), level = kotlin.DeprecationLevel.ERROR)
  public static final void setDeprecatedErrorAccessors(@org.jetbrains.annotations.NotNull() java.lang.String);//  setDeprecatedErrorAccessors(java.lang.String)

  public static final boolean getDeprecatedVariable();//  getDeprecatedVariable()

  public static final int getDeprecatedErrorVariable();//  getDeprecatedErrorVariable()

  public static final void setDeprecatedErrorVariable(int);//  setDeprecatedErrorVariable(int)

  public static final void setDeprecatedVariable(boolean);//  setDeprecatedVariable(boolean)
}
