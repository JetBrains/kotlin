public abstract interface ControlTransform /* ControlTransform*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String apply(@org.jetbrains.annotations.NotNull() java.lang.String);//  apply(java.lang.String)
}

public final class SamHost /* SamHost*/ {
  @org.jetbrains.annotations.NotNull()
  private final Transform transform;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final StringWrapper run(@org.jetbrains.annotations.NotNull() StringWrapper);//  run(StringWrapper)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String run-c5Hft5I(@org.jetbrains.annotations.NotNull() java.lang.String);//  run-c5Hft5I(java.lang.String)

  public  SamHost(@org.jetbrains.annotations.NotNull() Transform);//  .ctor(Transform)
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

public abstract interface Transform /* Transform*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String apply-c5Hft5I(@org.jetbrains.annotations.NotNull() java.lang.String);//  apply-c5Hft5I(java.lang.String)
}
