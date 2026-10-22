public final record Rec /* Rec*/(int i) {
  @org.jetbrains.annotations.NotNull()
  public static final Rec.Companion Companion;

  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.NotNull()
  public static final StringWrapper getBaz();//  getBaz()

  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmStatic()
  public static final void setBaz(@org.jetbrains.annotations.NotNull() StringWrapper);//  setBaz(StringWrapper)

  @org.jetbrains.annotations.NotNull()
  public final Rec copy(int);//  copy(int)

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String getBaz-K4fyztM();//  getBaz-K4fyztM()

  public  Rec(int);//  .ctor(int)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final int component1();//  component1()

  public final int i();//  i()

  public int hashCode();//  hashCode()

  public static final void setBaz-JELJCFg(@org.jetbrains.annotations.NotNull() java.lang.String);//  setBaz-JELJCFg(java.lang.String)

  class Companion ...
}

public static final class Companion /* Rec.Companion*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.NotNull()
  public final StringWrapper getBaz();//  getBaz()

  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmStatic()
  public final void setBaz(@org.jetbrains.annotations.NotNull() StringWrapper);//  setBaz(StringWrapper)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getBaz-K4fyztM();//  getBaz-K4fyztM()

  private  Companion();//  .ctor()

  public final void setBaz-JELJCFg(@org.jetbrains.annotations.NotNull() java.lang.String);//  setBaz-JELJCFg(java.lang.String)
}

@kotlin.jvm.JvmInline()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String s;

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
