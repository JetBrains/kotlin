public final class IntroducedAfterBase /* IntroducedAfterBase*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String text;

  private final int value;

  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  public  IntroducedAfterBase(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final kotlin.UInt getValue();//  getValue()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  IntroducedAfterBase(@org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "3") @org.jetbrains.annotations.NotNull() kotlin.UInt);//  .ctor(java.lang.String, kotlin.UInt)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getText();//  getText()

  public  IntroducedAfterBase();//  .ctor()

  public  IntroducedAfterBase(@org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "3") int);//  .ctor(java.lang.String, int)

  public final int getValue-pVg5ArA();//  getValue-pVg5ArA()
}

public final class IntroducedOnly /* IntroducedOnly*/ {
  private final int value;

  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  public  IntroducedOnly();//  .ctor()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final kotlin.UInt getValue();//  getValue()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  IntroducedOnly(@kotlin.IntroducedAt(version = "3") @org.jetbrains.annotations.NotNull() kotlin.UInt);//  .ctor(kotlin.UInt)

  public  IntroducedOnly(@kotlin.IntroducedAt(version = "3") int);//  .ctor(int)

  public final int getValue-pVg5ArA();//  getValue-pVg5ArA()
}

@kotlin.jvm.JvmInline()
public final class JvmMarker /* JvmMarker*/ {
  private final int value;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  JvmMarker(int);//  .ctor(int)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public final int getValue();//  getValue()

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(int, java.lang.Object);//  equals-impl(int, java.lang.Object)

  public static final boolean equals-impl0(int, int);//  equals-impl0(int, int)

  public static int constructor-impl(int);//  constructor-impl(int)

  public static int hashCode-impl(int);//  hashCode-impl(int)

  public static java.lang.String toString-impl(int);//  toString-impl(int)
}
