public abstract interface MyInterface /* MyInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public default java.lang.String getPublicMangledVariable-JELJCFg(@org.jetbrains.annotations.NotNull() java.lang.String);//  getPublicMangledVariable-JELJCFg(java.lang.String)

  private default int getRegularVariable();//  getRegularVariable()

  private default java.lang.String getMangledVariable-JELJCFg(java.lang.String);//  getMangledVariable-JELJCFg(java.lang.String)

  private default void mangledMethod-JELJCFg(java.lang.String);//  mangledMethod-JELJCFg(java.lang.String)

  private default void regularMethod();//  regularMethod()

  private default void setMangledVariable-d-auiwc(java.lang.String, java.lang.String);//  setMangledVariable-d-auiwc(java.lang.String, java.lang.String)

  private default void setRegularVariable(int);//  setRegularVariable(int)

  public default int getPublicRegularVariable();//  getPublicRegularVariable()

  public default void publicMangledMethod-JELJCFg(@org.jetbrains.annotations.NotNull() java.lang.String);//  publicMangledMethod-JELJCFg(java.lang.String)

  public default void publicRegularMethod();//  publicRegularMethod()

  public default void setPublicMangledVariable-d-auiwc(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String);//  setPublicMangledVariable-d-auiwc(java.lang.String, java.lang.String)

  public default void setPublicRegularVariable(int);//  setPublicRegularVariable(int)

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
