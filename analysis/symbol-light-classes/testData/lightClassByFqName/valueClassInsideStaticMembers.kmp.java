public final class MyClass /* one.MyClass*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() one.MyClass.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private static final @org.jetbrains.annotations.Nullable() java.lang.String staticPropertyWithInitializer = null /* initializer type: null */;

  public  MyClass();//  .ctor()

  public static final class Companion /* one.MyClass.Companion*/ {
    @<error>()
    public final void staticFunction(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  staticFunction(@org.jetbrains.annotations.NotNull() java.lang.String)

    @org.jetbrains.annotations.Nullable()
    public final @org.jetbrains.annotations.Nullable() java.lang.String getStaticProperty();//  getStaticProperty()

    @org.jetbrains.annotations.Nullable()
    public final @org.jetbrains.annotations.Nullable() java.lang.String getStaticPropertyWithInitializer();//  getStaticPropertyWithInitializer()

    private  Companion();//  .ctor()
  }
}
