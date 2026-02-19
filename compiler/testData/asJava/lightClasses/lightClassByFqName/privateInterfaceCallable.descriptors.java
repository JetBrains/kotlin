public abstract interface MyInterface /* MyInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String getPublicMangledVariable(@org.jetbrains.annotations.NotNull() java.lang.String);//  getPublicMangledVariable(java.lang.String)

  public abstract int getPublicRegularVariable();//  getPublicRegularVariable()

  public abstract void publicMangledMethod(@org.jetbrains.annotations.NotNull() java.lang.String);//  publicMangledMethod(java.lang.String)

  public abstract void publicRegularMethod();//  publicRegularMethod()

  public abstract void setPublicMangledVariable(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String);//  setPublicMangledVariable(java.lang.String, java.lang.String)

  public abstract void setPublicRegularVariable(int);//  setPublicRegularVariable(int)

  public static final class DefaultImpls /* MyInterface.DefaultImpls*/ {
    @org.jetbrains.annotations.NotNull()
    public static java.lang.String getPublicMangledVariable(@org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String);//  getPublicMangledVariable(MyInterface, java.lang.String)

    private static int getRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface);//  getRegularVariable(MyInterface)

    private static java.lang.String getMangledVariable(@org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String);//  getMangledVariable(MyInterface, java.lang.String)

    private static void mangledMethod(@org.jetbrains.annotations.NotNull() MyInterface, java.lang.String);//  mangledMethod(MyInterface, java.lang.String)

    private static void regularMethod(@org.jetbrains.annotations.NotNull() MyInterface);//  regularMethod(MyInterface)

    private static void setMangledVariable(@org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String, java.lang.String);//  setMangledVariable(MyInterface, java.lang.String, java.lang.String)

    private static void setRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface, int);//  setRegularVariable(MyInterface, int)

    public static int getPublicRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface);//  getPublicRegularVariable(MyInterface)

    public static void publicMangledMethod(@org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String);//  publicMangledMethod(MyInterface, java.lang.String)

    public static void publicRegularMethod(@org.jetbrains.annotations.NotNull() MyInterface);//  publicRegularMethod(MyInterface)

    public static void setPublicMangledVariable(@org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String);//  setPublicMangledVariable(MyInterface, java.lang.String, java.lang.String)

    public static void setPublicRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface, int);//  setPublicRegularVariable(MyInterface, int)
  }
}
