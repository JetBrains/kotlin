public final class AllNullable /* AllNullable*/ {
  @org.jetbrains.annotations.Nullable()
  private final IntWrapper b;

  @org.jetbrains.annotations.Nullable()
  private final StringWrapper a;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.Nullable()
  public final IntWrapper getB();//  getB()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.Nullable()
  public final StringWrapper getA();//  getA()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  AllNullable(@org.jetbrains.annotations.Nullable() StringWrapper, @org.jetbrains.annotations.Nullable() IntWrapper);//  .ctor(StringWrapper, IntWrapper)

  @org.jetbrains.annotations.Nullable()
  public final IntWrapper getB-qF8lFNU();//  getB-qF8lFNU()

  @org.jetbrains.annotations.Nullable()
  public final StringWrapper getA-DSQDras();//  getA-DSQDras()
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

public final class NoneNullable /* NoneNullable*/ {
  private final int a;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final IntWrapper getA();//  getA()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  NoneNullable(@org.jetbrains.annotations.NotNull() IntWrapper);//  .ctor(IntWrapper)

  private  NoneNullable(int);//  .ctor(int)

  public final int getA-7j0DjTs();//  getA-7j0DjTs()
}

public final class SomeNotNull /* SomeNotNull*/ {
  @org.jetbrains.annotations.Nullable()
  private final StringWrapper a;

  private final int b;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final IntWrapper getB();//  getB()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.Nullable()
  public final StringWrapper getA();//  getA()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  SomeNotNull(@org.jetbrains.annotations.Nullable() StringWrapper, @org.jetbrains.annotations.NotNull() IntWrapper);//  .ctor(StringWrapper, IntWrapper)

  @org.jetbrains.annotations.Nullable()
  public final StringWrapper getA-DSQDras();//  getA-DSQDras()

  private  SomeNotNull(StringWrapper, int);//  .ctor(StringWrapper, int)

  public final int getB-7j0DjTs();//  getB-7j0DjTs()
}

@kotlin.jvm.JvmInline()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.Nullable()
  private final java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  public static java.lang.String constructor-impl(@org.jetbrains.annotations.Nullable() java.lang.String);//  constructor-impl(java.lang.String)

  @org.jetbrains.annotations.Nullable()
  public final java.lang.String getS();//  getS()

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(java.lang.String, java.lang.Object);//  equals-impl(java.lang.String, java.lang.Object)

  public static final boolean equals-impl0(java.lang.String, java.lang.String);//  equals-impl0(java.lang.String, java.lang.String)

  public static int hashCode-impl(java.lang.String);//  hashCode-impl(java.lang.String)

  public static java.lang.String toString-impl(java.lang.String);//  toString-impl(java.lang.String)
}
