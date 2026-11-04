public class Base /* Base*/ {
  public  Base();//  .ctor()
}

public final class Derived /* Derived*/ extends Base {
  @java.lang.Override()
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public /* vararg */ @org.jetbrains.annotations.NotNull() java.lang.String f(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() ...);//  f(@org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [])

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String f(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  f(@org.jetbrains.annotations.NotNull() kotlin.UIntArray)

  public  Derived();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class IC /* IC*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final /* vararg */ @org.jetbrains.annotations.NotNull() java.lang.String member(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() ...);//  member(@org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [])

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String member(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  member(@org.jetbrains.annotations.NotNull() kotlin.UIntArray)

  @kotlin.jvm.JvmExposeBoxed()
  public  IC(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  @kotlin.jvm.JvmExposeBoxed()
  public  IC(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  .ctor(@org.jetbrains.annotations.NotNull() kotlin.UIntArray)

  @kotlin.jvm.JvmExposeBoxed()
  public /* vararg */  IC(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() ...);//  .ctor(@org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [])

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class Prefixed /* Prefixed*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String prefix;

  @org.jetbrains.annotations.NotNull()
  private final byte @org.jetbrains.annotations.NotNull() [] items;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() IC getPrefix();//  getPrefix()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() kotlin.UByteArray getItems();//  getItems()

  @kotlin.jvm.JvmExposeBoxed()
  public  Prefixed(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() IC, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UByteArray);//  .ctor(@org.jetbrains.annotations.NotNull() IC, @org.jetbrains.annotations.NotNull() kotlin.UByteArray)

  @kotlin.jvm.JvmExposeBoxed()
  public /* vararg */  Prefixed(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() IC, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UByte @org.jetbrains.annotations.NotNull() ...);//  .ctor(@org.jetbrains.annotations.NotNull() IC, @org.jetbrains.annotations.NotNull() kotlin.UByte @org.jetbrains.annotations.NotNull() [])

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String result();//  result()

  private /* vararg */  Prefixed(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() byte...);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, byte[])
}

public final class UIntStack /* UIntStack*/ {
  @org.jetbrains.annotations.NotNull()
  private final int @org.jetbrains.annotations.NotNull() [] items;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final /* vararg */ @org.jetbrains.annotations.NotNull() kotlin.UInt sum(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() ...);//  sum(@org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [])

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() kotlin.UInt sum(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  sum(@org.jetbrains.annotations.NotNull() kotlin.UIntArray)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() kotlin.UIntArray getItems();//  getItems()

  @kotlin.jvm.JvmExposeBoxed()
  public  UIntStack(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  .ctor(@org.jetbrains.annotations.NotNull() kotlin.UIntArray)

  @kotlin.jvm.JvmExposeBoxed()
  public /* vararg */  UIntStack(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() ...);//  .ctor(@org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [])

  private /* vararg */  UIntStack(@org.jetbrains.annotations.NotNull() int...);//  .ctor(int[])
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class, kotlin.ExperimentalUnsignedTypes.class})
public final class UnsignedVarargKt /* UnsignedVarargKt*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ @org.jetbrains.annotations.NotNull() java.lang.String withDefaults(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() ...);//  withDefaults(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [])

  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String withDefaults();//  withDefaults()

  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String withDefaults(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  withDefaults(@org.jetbrains.annotations.NotNull() java.lang.String)

  @kotlin.jvm.JvmExposeBoxed()
  @kotlin.jvm.JvmOverloads()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String withDefaults(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  withDefaults(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.UIntArray)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ @org.jetbrains.annotations.NotNull() java.lang.String sumUShort(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UShort @org.jetbrains.annotations.NotNull() ...);//  sumUShort(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.UShort @org.jetbrains.annotations.NotNull() [])

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ @org.jetbrains.annotations.NotNull() kotlin.ULong sumULong(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.ULong @org.jetbrains.annotations.NotNull() ...);//  sumULong(@org.jetbrains.annotations.NotNull() kotlin.ULong @org.jetbrains.annotations.NotNull() [])

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String notLast(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [], @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  notLast(@org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [], @org.jetbrains.annotations.NotNull() java.lang.String)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String notLast(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UIntArray, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  notLast(@org.jetbrains.annotations.NotNull() kotlin.UIntArray, @org.jetbrains.annotations.NotNull() java.lang.String)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String sumUShort(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UShortArray);//  sumUShort(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.UShortArray)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() kotlin.ULong sumULong(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.ULongArray);//  sumULong(@org.jetbrains.annotations.NotNull() kotlin.ULongArray)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "sumAll")
  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ @org.jetbrains.annotations.NotNull() kotlin.UInt sumAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() ...);//  sumAll(@org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [])

  @kotlin.jvm.JvmExposeBoxed(jvmName = "sumAll")
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() kotlin.UInt sumAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UIntArray);//  sumAll(@org.jetbrains.annotations.NotNull() kotlin.UIntArray)
}
