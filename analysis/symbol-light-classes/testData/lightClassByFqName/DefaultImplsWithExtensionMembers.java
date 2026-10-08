public abstract interface A /* A*/ {
  public abstract int getExtensionProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  getExtensionProperty(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract int getExtensionPropertyWithContextParameter(double, char);//  getExtensionPropertyWithContextParameter(double, char)

  public abstract void extension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, long);//  extension(@org.jetbrains.annotations.NotNull() java.lang.String, long)

  public abstract void extensionWithContextParameter(double, char, long);//  extensionWithContextParameter(double, char, long)

  public abstract void setExtensionProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  setExtensionProperty(@org.jetbrains.annotations.NotNull() java.lang.String, int)

  public abstract void withContextParameter(double, long);//  withContextParameter(double, long)

  public static final class DefaultImpls /* A.DefaultImpls*/ {
    public static int getExtensionProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A);//  getExtensionProperty(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() A)

    public static int getExtensionPropertyWithContextParameter(double, char, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A);//  getExtensionPropertyWithContextParameter(double, char, @org.jetbrains.annotations.NotNull() A)

    public static void extension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A, long);//  extension(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() A, long)

    public static void extensionWithContextParameter(double, char, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A, long);//  extensionWithContextParameter(double, char, @org.jetbrains.annotations.NotNull() A, long)

    public static void setExtensionProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A, int);//  setExtensionProperty(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() A, int)

    public static void withContextParameter(double, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A, long);//  withContextParameter(double, @org.jetbrains.annotations.NotNull() A, long)
  }
}
