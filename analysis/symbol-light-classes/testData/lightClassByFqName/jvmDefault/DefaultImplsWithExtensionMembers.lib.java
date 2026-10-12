public abstract interface A /* A*/ {
  public abstract int getExtensionProperty(@org.jetbrains.annotations.NotNull() java.lang.String);//  getExtensionProperty(java.lang.String)

  public abstract int getExtensionPropertyWithContextParameter(double, char);//  getExtensionPropertyWithContextParameter(double, char)

  public abstract void extension(@org.jetbrains.annotations.NotNull() java.lang.String, long);//  extension(java.lang.String, long)

  public abstract void extensionWithContextParameter(double, char, long);//  extensionWithContextParameter(double, char, long)

  public abstract void setExtensionProperty(@org.jetbrains.annotations.NotNull() java.lang.String, int);//  setExtensionProperty(java.lang.String, int)

  public abstract void withContextParameter(double, long);//  withContextParameter(double, long)

  public static final class DefaultImpls /* A.DefaultImpls*/ {
    public static int getExtensionProperty(@org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() java.lang.String);//  getExtensionProperty(A, java.lang.String)

    public static int getExtensionPropertyWithContextParameter(@org.jetbrains.annotations.NotNull() A, double, char);//  getExtensionPropertyWithContextParameter(A, double, char)

    public static void extension(@org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() java.lang.String, long);//  extension(A, java.lang.String, long)

    public static void extensionWithContextParameter(@org.jetbrains.annotations.NotNull() A, double, char, long);//  extensionWithContextParameter(A, double, char, long)

    public static void setExtensionProperty(@org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() java.lang.String, int);//  setExtensionProperty(A, java.lang.String, int)

    public static void withContextParameter(@org.jetbrains.annotations.NotNull() A, double, long);//  withContextParameter(A, double, long)
  }
}
