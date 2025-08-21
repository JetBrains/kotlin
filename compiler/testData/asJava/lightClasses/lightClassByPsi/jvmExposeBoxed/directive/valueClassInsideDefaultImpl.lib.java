public abstract interface BaseInterface /* one.BaseInterface*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.Nullable()
  public abstract one.MyValueClass getPropertyWithValueClassParameter();//  getPropertyWithValueClassParameter()

  @kotlin.jvm.JvmExposeBoxed()
  public abstract void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.MyValueClass);//  functionWithValueClassParameter(one.MyValueClass)

  @org.jetbrains.annotations.Nullable()
  public abstract java.lang.String getPropertyWithValueClassParameter-BXGQg7w();//  getPropertyWithValueClassParameter-BXGQg7w()

  public abstract void functionWithValueClassParameter-rdfNfmQ(@org.jetbrains.annotations.NotNull() java.lang.String);//  functionWithValueClassParameter-rdfNfmQ(java.lang.String)

  public abstract void regularFunction();//  regularFunction()

  class DefaultImpls ...
}

@kotlin.jvm.JvmInline()
public final class MyValueClass /* one.MyValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String str;

  @kotlin.jvm.JvmExposeBoxed()
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
