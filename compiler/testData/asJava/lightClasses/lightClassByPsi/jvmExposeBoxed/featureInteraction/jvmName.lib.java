public final class Foo /* Foo*/ {
  @kotlin.jvm.JvmExposeBoxed(jvmName = "foo11")
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String foo11(@org.jetbrains.annotations.NotNull() StringWrapper);//  foo11(StringWrapper)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "foo22")
  @org.jetbrains.annotations.NotNull()
  public final StringWrapper foo22();//  foo22()

  @kotlin.jvm.JvmName(name = "foo11")
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String foo11(@org.jetbrains.annotations.NotNull() java.lang.String);//  foo11(java.lang.String)

  @kotlin.jvm.JvmName(name = "foo21")
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String foo21();//  foo21()

  public  Foo();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String s;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  StringWrapper(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getS();//  getS()

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
