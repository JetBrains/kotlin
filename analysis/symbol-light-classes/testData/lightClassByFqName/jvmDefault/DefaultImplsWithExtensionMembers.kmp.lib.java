public abstract interface A /* A*/ {
  public default int getExtensionProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  getExtensionProperty(@org.jetbrains.annotations.NotNull() java.lang.String)

  public default int getExtensionPropertyWithContextParameter(double, char);//  getExtensionPropertyWithContextParameter(double, char)

  public default void extension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, long);//  extension(@org.jetbrains.annotations.NotNull() java.lang.String, long)

  public default void extensionWithContextParameter(double, char, long);//  extensionWithContextParameter(double, char, long)

  public default void setExtensionProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  setExtensionProperty(@org.jetbrains.annotations.NotNull() java.lang.String, int)

  public default void withContextParameter(double, long);//  withContextParameter(double, long)

  public static final class DefaultImpls /* A.DefaultImpls*/ {
    public static int getExtensionProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  getExtensionProperty(@org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() java.lang.String)

    public static int getExtensionPropertyWithContextParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A, double, char);//  getExtensionPropertyWithContextParameter(@org.jetbrains.annotations.NotNull() A, double, char)

    public static void extension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, long);//  extension(@org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() java.lang.String, long)

    public static void extensionWithContextParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A, double, char, long);//  extensionWithContextParameter(@org.jetbrains.annotations.NotNull() A, double, char, long)

    public static void setExtensionProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  setExtensionProperty(@org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() java.lang.String, int)

    public static void withContextParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A, double, long);//  withContextParameter(@org.jetbrains.annotations.NotNull() A, double, long)
  }
}
