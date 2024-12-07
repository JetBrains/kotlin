public final class DeprecatedKt /* foo.DeprecatedKt*/ {
  @org.jetbrains.annotations.NotNull()
  private static java.lang.String deprecatedAccessors;

  @org.jetbrains.annotations.NotNull()
  private static java.lang.String deprecatedErrorAccessors;

  private static boolean deprecatedVariable;

  private static int deprecatedErrorVariable;

  @kotlin.Deprecated(message = "deprecated function")
  public static final void deprecatedFunction(int);//  deprecatedFunction(int)

  @kotlin.Deprecated(message = "deprecated getter")
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String getDeprecatedAccessors();//  getDeprecatedAccessors()

  @kotlin.Deprecated(message = "deprecated setter")
  public static final void setDeprecatedAccessors(@org.jetbrains.annotations.NotNull() java.lang.String);//  setDeprecatedAccessors(java.lang.String)

  @kotlin.Deprecated(message = "error function", replaceWith = ReplaceWith("function"), level = kotlin.DeprecationLevel.ERROR)
  public static final void deprecatedErrorFunction();//  deprecatedErrorFunction()

  @kotlin.Deprecated(message = "error getter", replaceWith = ReplaceWith("new getter"), level = kotlin.DeprecationLevel.ERROR)
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String getDeprecatedErrorAccessors();//  getDeprecatedErrorAccessors()

  @kotlin.Deprecated(message = "error setter", replaceWith = ReplaceWith("new setter"), level = kotlin.DeprecationLevel.ERROR)
  public static final void setDeprecatedErrorAccessors(@org.jetbrains.annotations.NotNull() java.lang.String);//  setDeprecatedErrorAccessors(java.lang.String)

  public static final boolean getDeprecatedVariable();//  getDeprecatedVariable()

  public static final int getDeprecatedErrorVariable();//  getDeprecatedErrorVariable()

  public static final void setDeprecatedErrorVariable(int);//  setDeprecatedErrorVariable(int)

  public static final void setDeprecatedVariable(boolean);//  setDeprecatedVariable(boolean)
}