public abstract interface BaseInterface /* one.BaseInterface*/ {
  @org.jetbrains.annotations.Nullable()
  public abstract java.lang.String getPropertyWithValueClassParameter-BXGQg7w();//  getPropertyWithValueClassParameter-BXGQg7w()

  public abstract void functionWithValueClassParameter-rdfNfmQ(@org.jetbrains.annotations.NotNull() java.lang.String);//  functionWithValueClassParameter-rdfNfmQ(java.lang.String)

  public abstract void regularFunction();//  regularFunction()

  public static final class DefaultImpls /* one.BaseInterface.DefaultImpls*/ {
    @java.lang.Deprecated()
    @org.jetbrains.annotations.Nullable()
    public static java.lang.String getPropertyWithValueClassParameter-BXGQg7w(@org.jetbrains.annotations.NotNull() one.BaseInterface);//  getPropertyWithValueClassParameter-BXGQg7w(one.BaseInterface)

    @java.lang.Deprecated()
    public static void functionWithValueClassParameter-rdfNfmQ(@org.jetbrains.annotations.NotNull() one.BaseInterface, @org.jetbrains.annotations.NotNull() java.lang.String);//  functionWithValueClassParameter-rdfNfmQ(one.BaseInterface, java.lang.String)

    @java.lang.Deprecated()
    public static void regularFunction(@org.jetbrains.annotations.NotNull() one.BaseInterface);//  regularFunction(one.BaseInterface)
  }
}

@kotlin.jvm.JvmInline()
public final class MyValueClass /* one.MyValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String str;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  MyValueClass(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getStr();//  getStr()

  @org.jetbrains.annotations.NotNull()
  public static java.lang.String constructor-impl(@org.jetbrains.annotations.NotNull() java.lang.String);//  constructor-impl(java.lang.String)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(java.lang.String, java.lang.Object);//  equals-impl(java.lang.String, java.lang.Object)

  public static final boolean equals-impl0(java.lang.String, java.lang.String);//  equals-impl0(java.lang.String, java.lang.String)

  public static int hashCode-impl(java.lang.String);//  hashCode-impl(java.lang.String)

  public static java.lang.String toString-impl(java.lang.String);//  toString-impl(java.lang.String)
}
