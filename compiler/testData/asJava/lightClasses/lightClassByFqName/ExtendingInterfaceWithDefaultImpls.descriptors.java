public abstract interface C /* p.C*/<T>  extends p.B {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String c();//  c()

  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String more();//  more()

  public abstract int getProp3();//  getProp3()

  public abstract void setProp3(int);//  setProp3(int)

  public static final class DefaultImpls /* p.C.DefaultImpls*/ {
    @org.jetbrains.annotations.NotNull()
    public static java.lang.String a(@org.jetbrains.annotations.NotNull() p.C);//  a(p.C)

    @org.jetbrains.annotations.NotNull()
    public static java.lang.String b(@org.jetbrains.annotations.NotNull() p.C);//  b(p.C)

    @org.jetbrains.annotations.NotNull()
    public static java.lang.String c(@org.jetbrains.annotations.NotNull() p.C);//  c(p.C)

    @p.Anno()
    public static void setProp1(@org.jetbrains.annotations.NotNull() p.C, int);//  setProp1(p.C, int)

    @p.Anno()
    public static void setProp2(@org.jetbrains.annotations.NotNull() p.C, int);//  setProp2(p.C, int)

    @p.Anno()
    public static void setProp3(@org.jetbrains.annotations.NotNull() p.C, int);//  setProp3(p.C, int)

    public static int getProp1(@org.jetbrains.annotations.NotNull() p.C);//  getProp1(p.C)

    public static int getProp2(@org.jetbrains.annotations.NotNull() p.C);//  getProp2(p.C)

    public static int getProp3(@org.jetbrains.annotations.NotNull() p.C);//  getProp3(p.C)
  }
}