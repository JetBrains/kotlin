public final class Exposed /* Exposed*/ {
  @kotlin.jvm.JvmExposeBoxed(jvmName = "exposedName")
  @org.jetbrains.annotations.NotNull()
  public final IntWrapper exposedName(@org.jetbrains.annotations.Nullable() kotlin.Result<java.lang.String>);//  exposedName(kotlin.Result<java.lang.String>)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "renamed")
  @kotlin.jvm.JvmName(name = "renamed")
  @org.jetbrains.annotations.NotNull()
  public final IntWrapper renamed(@org.jetbrains.annotations.Nullable() kotlin.Result<java.lang.String>);//  renamed(kotlin.Result<java.lang.String>)

  @kotlin.jvm.JvmName(name = "regularName")
  public final int regularName(@org.jetbrains.annotations.Nullable() kotlin.Result<java.lang.String>);//  regularName(kotlin.Result<java.lang.String>)

  public  Exposed();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class IntWrapper /* IntWrapper*/ {
  private final int i;

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public final int getI();//  getI()

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(int, java.lang.Object);//  equals-impl(int, java.lang.Object)

  public static final boolean equals-impl0(int, int);//  equals-impl0(int, int)

  public static int constructor-impl(int);//  constructor-impl(int)

  public static int hashCode-impl(int);//  hashCode-impl(int)

  public static java.lang.String toString-impl(int);//  toString-impl(int)
}
