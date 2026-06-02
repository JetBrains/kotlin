public final class DeprecatedKt /* foo.DeprecatedKt*/ {
  @java.lang.Deprecated()
  private static boolean deprecatedVariable = false /* initializer type: boolean */;

  @java.lang.Deprecated()
  private static int deprecatedErrorVariable = 1 /* initializer type: int */;

  @org.jetbrains.annotations.NotNull()
  private static @org.jetbrains.annotations.NotNull() java.lang.String deprecatedAccessors = "2" /* initializer type: java.lang.String */;

  @org.jetbrains.annotations.NotNull()
  private static @org.jetbrains.annotations.NotNull() java.lang.String deprecatedErrorAccessors = "2" /* initializer type: java.lang.String */;

  @java.lang.Deprecated()
  @kotlin.Deprecated(message = "deprecated function")
  public static final void deprecatedFunction(int);//  deprecatedFunction(int)

  @java.lang.Deprecated()
  @kotlin.Deprecated(message = "deprecated getter")
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String getDeprecatedAccessors();//  getDeprecatedAccessors()

  @java.lang.Deprecated()
  @kotlin.Deprecated(message = "deprecated setter")
  public static final void setDeprecatedAccessors(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setDeprecatedAccessors(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Deprecated()
  @kotlin.Deprecated(message = "error function", replaceWith = @kotlin.ReplaceWith(expression = "function", imports = {}), level = kotlin.DeprecationLevel.ERROR)
  public static final void deprecatedErrorFunction();//  deprecatedErrorFunction()

  @java.lang.Deprecated()
  @kotlin.Deprecated(message = "error getter", replaceWith = @kotlin.ReplaceWith(expression = "new getter", imports = {}), level = kotlin.DeprecationLevel.ERROR)
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String getDeprecatedErrorAccessors();//  getDeprecatedErrorAccessors()

  @java.lang.Deprecated()
  @kotlin.Deprecated(message = "error setter", replaceWith = @kotlin.ReplaceWith(expression = "new setter", imports = {}), level = kotlin.DeprecationLevel.ERROR)
  public static final void setDeprecatedErrorAccessors(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setDeprecatedErrorAccessors(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Deprecated()
  public static final boolean getDeprecatedVariable();//  getDeprecatedVariable()

  @java.lang.Deprecated()
  public static final int getDeprecatedErrorVariable();//  getDeprecatedErrorVariable()

  @java.lang.Deprecated()
  public static final void setDeprecatedErrorVariable(int);//  setDeprecatedErrorVariable(int)

  @java.lang.Deprecated()
  public static final void setDeprecatedVariable(boolean);//  setDeprecatedVariable(boolean)
}
