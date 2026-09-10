public abstract interface MyInterface /* MyInterface*/ {
  private default int getRegularVariable();//  getRegularVariable()

  private default void regularMethod();//  regularMethod()

  private default void setRegularVariable(int);//  setRegularVariable(int)

  public default int getPublicRegularVariable();//  getPublicRegularVariable()

  public default void publicRegularMethod();//  publicRegularMethod()

  public default void setPublicRegularVariable(int);//  setPublicRegularVariable(int)

  public static final class DefaultImpls /* MyInterface.DefaultImpls*/ {
    @java.lang.Deprecated()
    private static final int getRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  getRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface)

    @java.lang.Deprecated()
    private static final void setRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, int);//  setRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface, int)

    @java.lang.Deprecated()
    private static void regularMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  regularMethod(@org.jetbrains.annotations.NotNull() MyInterface)

    @java.lang.Deprecated()
    public static int getPublicRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  getPublicRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface)

    @java.lang.Deprecated()
    public static void publicRegularMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  publicRegularMethod(@org.jetbrains.annotations.NotNull() MyInterface)

    @java.lang.Deprecated()
    public static void setPublicRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, int);//  setPublicRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface, int)
  }
}
