public abstract interface MyInterface /* MyInterface*/ {
  @kotlin.jvm.JvmExposeBoxed()
  private abstract @org.jetbrains.annotations.NotNull() java.lang.String getMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() StringWrapper);//  getMangledVariable(@org.jetbrains.annotations.NotNull() StringWrapper)

  @kotlin.jvm.JvmExposeBoxed()
  private abstract void mangledMethod(@org.jetbrains.annotations.NotNull() StringWrapper);//  mangledMethod(@org.jetbrains.annotations.NotNull() StringWrapper)

  @kotlin.jvm.JvmExposeBoxed()
  private abstract void setMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() StringWrapper, @org.jetbrains.annotations.NotNull() java.lang.String);//  setMangledVariable(@org.jetbrains.annotations.NotNull() StringWrapper, @org.jetbrains.annotations.NotNull() java.lang.String)

  private abstract int getRegularVariable();//  getRegularVariable()

  private abstract void regularMethod();//  regularMethod()

  private abstract void setRegularVariable(int);//  setRegularVariable(int)

  public abstract int getPublicRegularVariable();//  getPublicRegularVariable()

  public abstract void publicRegularMethod();//  publicRegularMethod()

  public abstract void setPublicRegularVariable(int);//  setPublicRegularVariable(int)

  public static final class DefaultImpls /* MyInterface.DefaultImpls*/ {
    @kotlin.jvm.JvmExposeBoxed()
    private static final @org.jetbrains.annotations.NotNull() java.lang.String getMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() StringWrapper, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  getMangledVariable(@org.jetbrains.annotations.NotNull() StringWrapper, @org.jetbrains.annotations.NotNull() MyInterface)

    @kotlin.jvm.JvmExposeBoxed()
    private static final void setMangledVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() StringWrapper, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String);//  setMangledVariable(@org.jetbrains.annotations.NotNull() StringWrapper, @org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String)

    @kotlin.jvm.JvmExposeBoxed()
    private static void mangledMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() StringWrapper);//  mangledMethod(@org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() StringWrapper)

    private static final int getRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  getRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface)

    private static final void setRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, int);//  setRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface, int)

    private static void regularMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  regularMethod(@org.jetbrains.annotations.NotNull() MyInterface)

    public static int getPublicRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  getPublicRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface)

    public static void publicRegularMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface);//  publicRegularMethod(@org.jetbrains.annotations.NotNull() MyInterface)

    public static void setPublicRegularVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyInterface, int);//  setPublicRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface, int)
  }
}
