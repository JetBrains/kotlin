public abstract interface MyInterface /* MyInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String getPublicMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  getPublicMangledVariable(@org.jetbrains.annotations.NotNull() java.lang.String)

  private abstract @org.jetbrains.annotations.NotNull() java.lang.String getMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  getMangledVariable(@org.jetbrains.annotations.NotNull() java.lang.String)

  private abstract int getRegularVariable();//  getRegularVariable()

  private abstract void mangledMethod(@org.jetbrains.annotations.NotNull() java.lang.String);//  mangledMethod(@org.jetbrains.annotations.NotNull() java.lang.String)

  private abstract void regularMethod();//  regularMethod()

  private abstract void setMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String);//  setMangledVariable(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String)

  private abstract void setRegularVariable(int);//  setRegularVariable(int)

  public abstract int getPublicRegularVariable();//  getPublicRegularVariable()

  public abstract void publicMangledMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  publicMangledMethod(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract void publicRegularMethod();//  publicRegularMethod()

  public abstract void setPublicMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setPublicMangledVariable(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract void setPublicRegularVariable(int);//  setPublicRegularVariable(int)

  public static final class DefaultImpls /* MyInterface.DefaultImpls*/ {
    @org.jetbrains.annotations.NotNull()
    public static @org.jetbrains.annotations.NotNull() java.lang.String getPublicMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  getPublicMangledVariable(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() MyInterface)

    private static final @org.jetbrains.annotations.NotNull() java.lang.String getMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  getMangledVariable(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() MyInterface)

    private static final int getRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  getRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface)

    private static final void setMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String);//  setMangledVariable(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String)

    private static final void setRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, int);//  setRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface, int)

    private static void mangledMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String);//  mangledMethod(@org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String)

    private static void regularMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  regularMethod(@org.jetbrains.annotations.NotNull() MyInterface)

    public static int getPublicRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  getPublicRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface)

    public static void publicMangledMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  publicMangledMethod(@org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String)

    public static void publicRegularMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  publicRegularMethod(@org.jetbrains.annotations.NotNull() MyInterface)

    public static void setPublicMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setPublicMangledVariable(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String)

    public static void setPublicRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, int);//  setPublicRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface, int)
  }
}
