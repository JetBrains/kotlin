public final class Bounded /* Bounded*/<T extends Wrapper>  {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() [] xs;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() [] getXs();//  getXs()

  public /* vararg */  Bounded(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() ...);//  .ctor(@org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() [])
}

public final class ExposedH /* ExposedH*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.Nullable() kotlin.UByte @org.jetbrains.annotations.NotNull() [] xs;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.Nullable() kotlin.UByte @org.jetbrains.annotations.NotNull() [] getXs();//  getXs()

  public /* vararg */  ExposedH(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() kotlin.UByte @org.jetbrains.annotations.Nullable() ...);//  .ctor(@org.jetbrains.annotations.Nullable() kotlin.UByte @org.jetbrains.annotations.Nullable() [])

  public final /* vararg */ int member(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() kotlin.UByte @org.jetbrains.annotations.Nullable() ...);//  member(@org.jetbrains.annotations.Nullable() kotlin.UByte @org.jetbrains.annotations.Nullable() [])
}

public final class H /* H*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.Nullable() kotlin.UInt @org.jetbrains.annotations.NotNull() [] xs;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.Nullable() kotlin.UInt @org.jetbrains.annotations.NotNull() [] getXs();//  getXs()

  public /* vararg */  H(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() kotlin.UInt @org.jetbrains.annotations.Nullable() ...);//  .ctor(@org.jetbrains.annotations.Nullable() kotlin.UInt @org.jetbrains.annotations.Nullable() [])

  public final /* vararg */ <T extends Wrapper> int boundedMember(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() ...);// <T extends Wrapper>  boundedMember(@org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() [])

  public final /* vararg */ int member(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() kotlin.UShort @org.jetbrains.annotations.Nullable() ...);//  member(@org.jetbrains.annotations.Nullable() kotlin.UShort @org.jetbrains.annotations.Nullable() [])
}

public final class Mangled /* Mangled*/ {
  @org.jetbrains.annotations.NotNull()
  private final int @org.jetbrains.annotations.NotNull() [] xs;

  private /* vararg */  Mangled(@org.jetbrains.annotations.NotNull() int...);//  .ctor(int[])
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class VarargManglingKt /* VarargManglingKt*/ {
  public static final /* vararg */ <T extends Wrapper> int boundedByValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() ...);// <T extends Wrapper>  boundedByValueClass(@org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ <T extends kotlin.UInt> int boundedByUnsigned(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() ...);// <T extends kotlin.UInt>  boundedByUnsigned(@org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ int aliasedNullable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() kotlin.UInt @org.jetbrains.annotations.Nullable() ...);//  aliasedNullable(@org.jetbrains.annotations.Nullable() kotlin.UInt @org.jetbrains.annotations.Nullable() [])

  public static final /* vararg */ int exposedNullable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() kotlin.UInt @org.jetbrains.annotations.Nullable() ...);//  exposedNullable(@org.jetbrains.annotations.Nullable() kotlin.UInt @org.jetbrains.annotations.Nullable() [])

  public static final /* vararg */ int plainNullable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() kotlin.UInt @org.jetbrains.annotations.Nullable() ...);//  plainNullable(@org.jetbrains.annotations.Nullable() kotlin.UInt @org.jetbrains.annotations.Nullable() [])

  public static final int nullableNotLast(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() kotlin.ULong @org.jetbrains.annotations.Nullable() [], @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  nullableNotLast(@org.jetbrains.annotations.Nullable() kotlin.ULong @org.jetbrains.annotations.Nullable() [], @org.jetbrains.annotations.NotNull() java.lang.String)
}

@kotlin.jvm.JvmInline()
public final class Wrapper /* Wrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
