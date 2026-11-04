public class Open /* Open*/ {
  @kotlin.jvm.JvmExposeBoxed()
  public final /* vararg */ void notOverridable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UShort @org.jetbrains.annotations.NotNull() ...);//  notOverridable(@org.jetbrains.annotations.NotNull() kotlin.UShort @org.jetbrains.annotations.NotNull() [])

  public  Open();//  .ctor()
}

public final class Stack /* Stack*/ {
  @org.jetbrains.annotations.NotNull()
  private final int @org.jetbrains.annotations.NotNull() [] items;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() kotlin.UIntArray getItems();//  getItems()

  @kotlin.jvm.JvmExposeBoxed()
  public /* vararg */  Stack(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() ...);//  .ctor(@org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [])

  @kotlin.jvm.JvmExposeBoxed()
  public final /* vararg */ void push(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UByte @org.jetbrains.annotations.NotNull() ...);//  push(@org.jetbrains.annotations.NotNull() kotlin.UByte @org.jetbrains.annotations.NotNull() [])

  private /* vararg */  Stack(@org.jetbrains.annotations.NotNull() int...);//  .ctor(int[])
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalUnsignedTypes.class})
public final class UnsignedVarargKt /* UnsignedVarargKt*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final /* vararg */ @org.jetbrains.annotations.NotNull() kotlin.UInt sum(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() ...);//  sum(@org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [])

  @kotlin.jvm.JvmExposeBoxed()
  public static final int notLast(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.ULong @org.jetbrains.annotations.NotNull() [], @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  notLast(@org.jetbrains.annotations.NotNull() kotlin.ULong @org.jetbrains.annotations.NotNull() [], @org.jetbrains.annotations.NotNull() java.lang.String)
}

@kotlin.jvm.JvmInline()
public final class Wrapper /* Wrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @kotlin.jvm.JvmExposeBoxed()
  public  Wrapper(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  @kotlin.jvm.JvmExposeBoxed()
  public /* vararg */  Wrapper(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() ...);//  .ctor(@org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [])

  @kotlin.jvm.JvmExposeBoxed()
  public final /* vararg */ void member(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() ...);//  member(@org.jetbrains.annotations.NotNull() kotlin.UInt @org.jetbrains.annotations.NotNull() [])

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
