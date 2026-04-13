public abstract interface MyInterface /* MyInterface*/ {
  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  private abstract java.lang.String getMangledVariable(StringWrapper);//  getMangledVariable(StringWrapper)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  private abstract void mangledMethod(StringWrapper);//  mangledMethod(StringWrapper)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  private abstract void setMangledVariable(StringWrapper, java.lang.String);//  setMangledVariable(StringWrapper, java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String getPublicMangledVariable-JELJCFg(@org.jetbrains.annotations.NotNull() java.lang.String);//  getPublicMangledVariable-JELJCFg(java.lang.String)

  private abstract int getRegularVariable();//  getRegularVariable()

  private abstract java.lang.String getMangledVariable-JELJCFg(java.lang.String);//  getMangledVariable-JELJCFg(java.lang.String)

  private abstract void mangledMethod-JELJCFg(java.lang.String);//  mangledMethod-JELJCFg(java.lang.String)

  private abstract void regularMethod();//  regularMethod()

  private abstract void setMangledVariable-d-auiwc(java.lang.String, java.lang.String);//  setMangledVariable-d-auiwc(java.lang.String, java.lang.String)

  private abstract void setRegularVariable(int);//  setRegularVariable(int)

  public abstract int getPublicRegularVariable();//  getPublicRegularVariable()

  public abstract void publicMangledMethod-JELJCFg(@org.jetbrains.annotations.NotNull() java.lang.String);//  publicMangledMethod-JELJCFg(java.lang.String)

  public abstract void publicRegularMethod();//  publicRegularMethod()

  public abstract void setPublicMangledVariable-d-auiwc(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String);//  setPublicMangledVariable-d-auiwc(java.lang.String, java.lang.String)

  public abstract void setPublicRegularVariable(int);//  setPublicRegularVariable(int)

  public static final class DefaultImpls /* MyInterface.DefaultImpls*/ {
    @java.lang.Deprecated()
    @org.jetbrains.annotations.NotNull()
    public static java.lang.String getPublicMangledVariable-JELJCFg(@org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String);//  getPublicMangledVariable-JELJCFg(MyInterface, java.lang.String)

    @java.lang.Deprecated()
    public static int getPublicRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface);//  getPublicRegularVariable(MyInterface)

    @java.lang.Deprecated()
    public static void publicMangledMethod-JELJCFg(@org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String);//  publicMangledMethod-JELJCFg(MyInterface, java.lang.String)

    @java.lang.Deprecated()
    public static void publicRegularMethod(@org.jetbrains.annotations.NotNull() MyInterface);//  publicRegularMethod(MyInterface)

    @java.lang.Deprecated()
    public static void setPublicMangledVariable-d-auiwc(@org.jetbrains.annotations.NotNull() MyInterface, @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String);//  setPublicMangledVariable-d-auiwc(MyInterface, java.lang.String, java.lang.String)

    @java.lang.Deprecated()
    public static void setPublicRegularVariable(@org.jetbrains.annotations.NotNull() MyInterface, int);//  setPublicRegularVariable(MyInterface, int)
  }
}
