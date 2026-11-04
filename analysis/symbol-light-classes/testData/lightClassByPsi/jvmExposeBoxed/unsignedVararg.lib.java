public class Base /* Base*/ {
  @org.jetbrains.annotations.NotNull()
  public /* vararg */ java.lang.String f--ajY-9A(@org.jetbrains.annotations.NotNull() int...);//  f--ajY-9A(int[])

  public  Base();//  .ctor()
}

public final class Derived /* Derived*/ extends Base {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public /* vararg */ java.lang.String f(@org.jetbrains.annotations.NotNull() kotlin.UInt...);//  f(kotlin.UInt[])

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public java.lang.String f(@org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  f(kotlin.UIntArray)

  @org.jetbrains.annotations.NotNull()
  public /* vararg */ java.lang.String f--ajY-9A(@org.jetbrains.annotations.NotNull() int...);//  f--ajY-9A(int[])

  public  Derived();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class IC /* IC*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String s;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final /* vararg */ java.lang.String member(@org.jetbrains.annotations.NotNull() kotlin.UInt...);//  member(kotlin.UInt[])

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String member(@org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  member(kotlin.UIntArray)

  @kotlin.jvm.JvmExposeBoxed()
  public  IC(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)

  @kotlin.jvm.JvmExposeBoxed()
  public  IC(@org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  .ctor(kotlin.UIntArray)

  @kotlin.jvm.JvmExposeBoxed()
  public /* vararg */  IC(@org.jetbrains.annotations.NotNull() kotlin.UInt...);//  .ctor(kotlin.UInt[])

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getS();//  getS()

  @org.jetbrains.annotations.NotNull()
  public static /* vararg */ java.lang.String constructor-impl(@org.jetbrains.annotations.NotNull() int...);//  constructor-impl(int[])

  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ java.lang.String member--ajY-9A(java.lang.String, @org.jetbrains.annotations.NotNull() int...);//  member--ajY-9A(java.lang.String, int[])

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

public final class Prefixed /* Prefixed*/ {
  @org.jetbrains.annotations.NotNull()
  private final byte[] items;

  @org.jetbrains.annotations.NotNull()
  private final java.lang.String prefix;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final IC getPrefix();//  getPrefix()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final kotlin.UByteArray getItems();//  getItems()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  Prefixed(@org.jetbrains.annotations.NotNull() IC, @org.jetbrains.annotations.NotNull() kotlin.UByteArray);//  .ctor(IC, kotlin.UByteArray)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public /* vararg */  Prefixed(@org.jetbrains.annotations.NotNull() IC, @org.jetbrains.annotations.NotNull() kotlin.UByte...);//  .ctor(IC, kotlin.UByte[])

  @org.jetbrains.annotations.NotNull()
  public final byte[] getItems-TcUX1vc();//  getItems-TcUX1vc()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getPrefix-fhib4bs();//  getPrefix-fhib4bs()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String result();//  result()

  private /* vararg */  Prefixed(java.lang.String, byte...);//  .ctor(java.lang.String, byte[])
}

public final class UIntStack /* UIntStack*/ {
  @org.jetbrains.annotations.NotNull()
  private final int[] items;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final /* vararg */ kotlin.UInt sum(@org.jetbrains.annotations.NotNull() kotlin.UInt...);//  sum(kotlin.UInt[])

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final kotlin.UInt sum(@org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  sum(kotlin.UIntArray)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final kotlin.UIntArray getItems();//  getItems()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  UIntStack(@org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  .ctor(kotlin.UIntArray)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public /* vararg */  UIntStack(@org.jetbrains.annotations.NotNull() kotlin.UInt...);//  .ctor(kotlin.UInt[])

  @org.jetbrains.annotations.NotNull()
  public final int[] getItems--hP7Qyg();//  getItems--hP7Qyg()

  private /* vararg */  UIntStack(int...);//  .ctor(int[])

  public final /* vararg */ int sum-JpblYG8(@org.jetbrains.annotations.NotNull() int...);//  sum-JpblYG8(int[])
}

public final class UnsignedVarargKt /* UnsignedVarargKt*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ java.lang.String withDefaults(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.UInt...);//  withDefaults(java.lang.String, kotlin.UInt[])

  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String withDefaults();//  withDefaults()

  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String withDefaults(@org.jetbrains.annotations.NotNull() java.lang.String);//  withDefaults(java.lang.String)

  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String withDefaults(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  withDefaults(java.lang.String, kotlin.UIntArray)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ java.lang.String sumUShort(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.UShort...);//  sumUShort(java.lang.String, kotlin.UShort[])

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ kotlin.ULong sumULong(@org.jetbrains.annotations.NotNull() kotlin.ULong...);//  sumULong(kotlin.ULong[])

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String notLast(@org.jetbrains.annotations.NotNull() kotlin.UIntArray, @org.jetbrains.annotations.NotNull() java.lang.String);//  notLast(kotlin.UIntArray, java.lang.String)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String notLast(@org.jetbrains.annotations.NotNull() kotlin.UInt[], @org.jetbrains.annotations.NotNull() java.lang.String);//  notLast(kotlin.UInt[], java.lang.String)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String sumUShort(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.UShortArray);//  sumUShort(java.lang.String, kotlin.UShortArray)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final kotlin.ULong sumULong(@org.jetbrains.annotations.NotNull() kotlin.ULongArray);//  sumULong(kotlin.ULongArray)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "sumAll")
  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ kotlin.UInt sumAll(@org.jetbrains.annotations.NotNull() kotlin.UInt...);//  sumAll(kotlin.UInt[])

  @kotlin.jvm.JvmExposeBoxed(jvmName = "sumAll")
  @org.jetbrains.annotations.NotNull()
  public static final kotlin.UInt sumAll(@org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  sumAll(kotlin.UIntArray)

  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ java.lang.String withDefaults-wZx4R44(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() int...);//  withDefaults-wZx4R44(java.lang.String, int[])

  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ java.lang.String sumUShort-jDvGgag(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() short...);//  sumUShort-jDvGgag(java.lang.String, short[])

  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String notLast-LpG4sQ0(@org.jetbrains.annotations.NotNull() int[], @org.jetbrains.annotations.NotNull() java.lang.String);//  notLast-LpG4sQ0(int[], java.lang.String)

  public static final /* vararg */ int sumNamed--ajY-9A(@org.jetbrains.annotations.NotNull() int...);//  sumNamed--ajY-9A(int[])

  public static final /* vararg */ long sumULong-QwZRm1k(@org.jetbrains.annotations.NotNull() long...);//  sumULong-QwZRm1k(long[])
}
