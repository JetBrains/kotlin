public final class Bounded /* Bounded*/<T extends Wrapper>  {
  @org.jetbrains.annotations.NotNull()
  private final T[] xs;

  @org.jetbrains.annotations.NotNull()
  public final T[] getXs();//  getXs()

  public /* vararg */  Bounded(@org.jetbrains.annotations.NotNull() T...);//  .ctor(T[])
}

public final class ExposedH /* ExposedH*/ {
  @org.jetbrains.annotations.NotNull()
  private final kotlin.UByte[] xs;

  @org.jetbrains.annotations.NotNull()
  public final kotlin.UByte[] getXs();//  getXs()

  public /* vararg */  ExposedH(@org.jetbrains.annotations.NotNull() kotlin.UByte...);//  .ctor(kotlin.UByte[])

  public final /* vararg */ int member(@org.jetbrains.annotations.NotNull() kotlin.UByte...);//  member(kotlin.UByte[])
}

public final class H /* H*/ {
  @org.jetbrains.annotations.NotNull()
  private final kotlin.UInt[] xs;

  @org.jetbrains.annotations.NotNull()
  public final kotlin.UInt[] getXs();//  getXs()

  public /* vararg */  H(@org.jetbrains.annotations.NotNull() kotlin.UInt...);//  .ctor(kotlin.UInt[])

  public final /* vararg */ <T extends Wrapper> int boundedMember(@org.jetbrains.annotations.NotNull() T...);// <T extends Wrapper>  boundedMember(T[])

  public final /* vararg */ int member(@org.jetbrains.annotations.NotNull() kotlin.UShort...);//  member(kotlin.UShort[])
}

public final class Mangled /* Mangled*/ {
  @org.jetbrains.annotations.NotNull()
  private final int[] xs;

  @org.jetbrains.annotations.NotNull()
  public final int[] getXs--hP7Qyg();//  getXs--hP7Qyg()

  private /* vararg */  Mangled(int...);//  .ctor(int[])
}

public final class VarargManglingKt /* VarargManglingKt*/ {
  @kotlin.jvm.JvmExposeBoxed()
  public static final /* vararg */ int exposedNullable(@org.jetbrains.annotations.NotNull() kotlin.UInt...);//  exposedNullable(kotlin.UInt[])

  public static final /* vararg */ <T extends Wrapper> int boundedByValueClass(@org.jetbrains.annotations.NotNull() T...);// <T extends Wrapper>  boundedByValueClass(T[])

  public static final /* vararg */ <T extends kotlin.UInt> int boundedByUnsigned(@org.jetbrains.annotations.NotNull() T...);// <T extends kotlin.UInt>  boundedByUnsigned(T[])

  public static final /* vararg */ int aliasedMangled--ajY-9A(@org.jetbrains.annotations.NotNull() int...);//  aliasedMangled--ajY-9A(int[])

  public static final /* vararg */ int aliasedNullable(@org.jetbrains.annotations.NotNull() kotlin.UInt...);//  aliasedNullable(kotlin.UInt[])

  public static final /* vararg */ int mangled--ajY-9A(@org.jetbrains.annotations.NotNull() int...);//  mangled--ajY-9A(int[])

  public static final /* vararg */ int plainNullable(@org.jetbrains.annotations.NotNull() kotlin.UInt...);//  plainNullable(kotlin.UInt[])

  public static final int nullableNotLast(@org.jetbrains.annotations.NotNull() kotlin.ULong[], @org.jetbrains.annotations.NotNull() java.lang.String);//  nullableNotLast(kotlin.ULong[], java.lang.String)
}

@kotlin.jvm.JvmInline()
public final class Wrapper /* Wrapper*/ {
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
