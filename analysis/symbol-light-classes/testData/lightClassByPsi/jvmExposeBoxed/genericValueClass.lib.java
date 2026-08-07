@kotlin.jvm.JvmInline()
public final class Box /* Box*/<T>  {
  private final T value;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  Box(T);//  .ctor(T)

  @org.jetbrains.annotations.NotNull()
  public static <T> java.lang.Object constructor-impl(T);// <T>  constructor-impl(T)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public final T getValue();//  getValue()

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(java.lang.Object, java.lang.Object);//  equals-impl(java.lang.Object, java.lang.Object)

  public static final boolean equals-impl0(java.lang.Object, java.lang.Object);//  equals-impl0(java.lang.Object, java.lang.Object)

  public static int hashCode-impl(java.lang.Object);//  hashCode-impl(java.lang.Object)

  public static java.lang.String toString-impl(java.lang.Object);//  toString-impl(java.lang.Object)
}

public final class GenericValueClassKt /* GenericValueClassKt*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final <T> Box<T> roundTrip(@org.jetbrains.annotations.NotNull() Box<T>);// <T>  roundTrip(Box<T>)

  @org.jetbrains.annotations.NotNull()
  public static final <T> java.lang.Object roundTrip-RbRCt1M(@org.jetbrains.annotations.NotNull() java.lang.Object);// <T>  roundTrip-RbRCt1M(java.lang.Object)
}
