public final class IntroducedAfterBase /* IntroducedAfterBase*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() int value;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String text;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() kotlin.UInt getValue();//  getValue()

  @kotlin.jvm.JvmExposeBoxed()
  public  IntroducedAfterBase(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "3") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.UInt)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getText();//  getText()

  private  IntroducedAfterBase(@org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "3") @org.jetbrains.annotations.NotNull() int);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() int)
}

public final class IntroducedOnly /* IntroducedOnly*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() int value;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() kotlin.UInt getValue();//  getValue()

  @kotlin.jvm.JvmExposeBoxed()
  public  IntroducedOnly(@kotlin.IntroducedAt(version = "3") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.UInt);//  .ctor(@org.jetbrains.annotations.NotNull() kotlin.UInt)

  private  IntroducedOnly(@kotlin.IntroducedAt(version = "3") @org.jetbrains.annotations.NotNull() int);//  .ctor(@org.jetbrains.annotations.NotNull() int)
}

@<error>()
public final class JvmMarker /* JvmMarker*/ {
  private final int value;

  @kotlin.jvm.JvmExposeBoxed()
  public  JvmMarker(int);//  .ctor(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getValue();//  getValue()

  public int hashCode();//  hashCode()
}
